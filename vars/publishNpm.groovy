/**
 * publishNpm.groovy
 * This function handles versioning, scoping, and publishing to Nexus.
 * Usage in Jenkinsfile: publishNpm(group: 'my-org')
 */
def call(Map config = [:]) {
    // 1. INPUT HANDLING & DEFAULTS
    // 'config' is a Map passed from the Jenkinsfile. 
    // The Elvis operator (?:) provides a fallback if the user leaves a field blank.
    def groupName = config.group ?: "default-group"
    def credentialsId = config.credentialsId ?: "nexus-server"
    def urlCredentialsId = config.urlCredentialsId ?: "NEXUS_NPM_URL"

    // 2. SECRET RETRIEVAL
    // We fetch secrets from Jenkins Credentials Store. 
    // This makes them available as Shell Environment Variables within this block.
    withCredentials([
        usernamePassword(credentialsId: credentialsId, usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS'),
        string(credentialsId: urlCredentialsId, variable: 'NEXUS_URL')
    ]) {
     
        // 3. PREPARING METADATA
        // Generates a UTC timestamp (e.g., 20260209-1100) to ensure every build has a unique version.
        def timestamp = new Date().format("yyyyMMdd-HHmmss", TimeZone.getTimeZone('UTC'))
        
        // Generates the Base64 auth string for NPM. '-w0' prevents line breaks which break the .npmrc.
        // We use single quotes for the script to avoid the "Insecure Interpolation" warning.
        def authString = sh(script: 'echo -n "$NEXUS_USER:$NEXUS_PASS" | base64 -w0', returnStdout: true).trim()
        
        // Clean the Nexus URL by removing any trailing slashes to prevent "http://url//repository" errors.
        def baseUrl = env.NEXUS_URL.trim().replaceAll(/\/+$/, "")
        
        // Construct the full repository URL and the auth path (the URL minus the http:// protocol).
        def fullUrl = "${baseUrl}/npm-hosted/"
        def registryPath = fullUrl.replaceAll(/^https?:\/\//, "")

        // 4. CONFIGURATION FILE GENERATION
        // writeFile is a native Jenkins step. It creates the .npmrc file in the workspace.
        // It maps the registry to the credentials via the registryPath.
        writeFile file: '.npmrc', text: """
            registry=${fullUrl}
            //${registryPath}:_auth=${authString}
            //${registryPath}:always-auth=true
        """.stripIndent()

        // 5. PACKAGE TRANSFORMATION & PUBLISHING
        sh """
            export HUSKY=0 
            
            # 1. Extract and update version
            # --ignore-scripts prevents Husky from failing the versioning process
            OLD_VER=\$(node -p "require('./package.json').version")
            NEW_VER="\${OLD_VER}-${timestamp}"
            
            npm version \${NEW_VER} --no-git-tag-version --force --ignore-scripts
            
            # Scope the package name
            node -e "
                const pkg = require('./package.json');
                if (!pkg.name.startsWith('@${groupName}/')) {
                    pkg.name = '@${groupName}/' + pkg.name.replace(/^@.*\\//, '');
                    require('fs').writeFileSync('package.json', JSON.stringify(pkg, null, 2));
                }
            "

            # Final Publish
            # This will NOT skip your build if you ran 'npm run build' in a previous stage.
            # It only skips the automatic "hooks" that are breaking your pipeline.
            npm publish --ignore-scripts
            rm .npmrc
        """
    }
}
