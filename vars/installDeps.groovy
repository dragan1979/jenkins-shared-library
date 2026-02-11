def call(Map config = [:]) {
    def credentialsId = config.credentialsId ?: 'nexus-server'
    def urlCredentialsId = config.urlCredentialsId ?: 'NEXUS_NPM_URL'
    def repoPath = config.repoPath ?: "/npm-all/"
    
    withCredentials([
        usernamePassword(credentialsId: credentialsId, usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS'),
        string(credentialsId: urlCredentialsId, variable: 'NEXUS_URL')
    ]) {
        // Use Groovy for base64 - no shell needed
        def authString = "${NEXUS_USER}:${NEXUS_PASS}".bytes.encodeBase64().toString()
        def baseUrl = env.NEXUS_URL.trim().replaceAll(/\/+$/, "")
        def fullUrl = "${baseUrl}/${repoPath}"
        def registryPath = fullUrl.replaceAll(/^https?:\/\//, "")
        
        // Don't echo secrets!
        echo "Configuring NPM registry..."
        // The warning is cosmetic - the file is written correctly
        writeFile file: '.npmrc', text: """\
        registry=${fullUrl}
        //${registryPath}:_auth=${authString}
        //${registryPath}:always-auth=true
        """
        echo "Installing dependencies..."
        sh '''
         # Install with verbose output to see what's happening
         # It ensures that the dependencies installed on the server are exactly what the developers tested locally, 
         # without accidentally modifying source code during the build.
         # --pure-lockfile
         # This flag changes how Yarn handles the yarn.lock file (the file that stores the exact version and download link for every single sub-dependency).
         # Normal Behavior: Usually, if you add a package or if Yarn finds a "better" version that fits your rules, it will update the yarn.lock file automatically.
         # Pure Behavior: Yarn will use the yarn.lock to install everything, but it is forbidden from writing to or changing it.
         yarn install --pure-lockfile
        '''
        
        // Cleanup
        sh 'rm -f .npmrc'
    }
}
