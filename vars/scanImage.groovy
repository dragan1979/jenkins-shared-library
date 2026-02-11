def call(Map config = [:]) {
    def imageName = config.image ?: env.APP_NAME
    def imageTag = config.tag ?: env.IMAGE_TAG
    def severity = config.severity ?: "HIGH,CRITICAL"
    
    // Create a local cache to avoid the "home directory" error.
    // Trivy tries to use temp directories or cache inside Jenkins home folder which is not standard,we need to tell Trivy where to save it's cache using
    // --cache-dir and ignore checking home directory
    // 1. Create a local cache and home directory within the workspace
    sh "mkdir -p ${WORKSPACE}/.trivy/cache"
    sh "mkdir -p ${WORKSPACE}/.trivy/home"
    
    echo "Security Scan: Checking ${imageName}:${imageTag} for ${severity} vulnerabilities..."
    
    // 2. Export HOME and use the cache-dir
    sh """
        export HOME=${WORKSPACE}/.trivy/home
        trivy image \
        --format template \
        --template "@/usr/local/share/trivy/templates/junit.tpl" \
        --output trivy-report.xml \
        --cache-dir ${WORKSPACE}/.trivy/cache \
        --severity ${severity} \
        --exit-code 1 \
        ${imageName}:${imageTag} || echo \$? > exit_code.txt
    """
    // Logic to check for Scan Results vs. System Errors
    if (fileExists('exit_code.txt')) {
        def exitCode = readFile('exit_code.txt').trim()
        // Trivy Exit Code 0 = Success (No vulnerabilities)
        // Trivy Exit Code 1 = Vulnerabilities found (but we want to pass)
        // Other codes (e.g., 127, 2) = System errors (command not found, network error)
        if (exitCode != "0" && exitCode != "1") {
            error "Trivy system error occurred (Exit Code: ${exitCode}). Pipeline stopped."
        }
    }
    // Fail only if the scan failed to even create the report
    if (!fileExists('trivy-report.xml')) {
        error "Security scan failed to generate results. Check Trivy installation."
    }
   // Record the XML as an Artifact and a Test Result
    archiveArtifacts artifacts: 'trivy-report.xml'
    // This allows Jenkins to show the vulnerabilities in the "Test Result" tab
    junit testResults: 'trivy-report.xml', allowEmptyResults: true
    
    echo "✅ Scan complete. Vulnerabilities recorded in trivy-report.xml"
}
