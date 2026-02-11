def call(Map config = [:]) {
    // 1. Get values from config or environment
    def imageName = config.image ?: env.APP_NAME
    def imageTag = config.tag ?: env.IMAGE_TAG

    echo "Building Docker Image: ${imageName}:${imageTag}"

    // 2. Execute the build
    // Using --pull ensures we always have the latest base image security patches
    sh "docker build --pull -t ${imageName}:${imageTag} ."
    sh "docker build --pull -t ${imageName}:latest ."
}
