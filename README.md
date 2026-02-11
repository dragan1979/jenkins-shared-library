# Jenkins Shared Library

This repository contains reusable Jenkins pipeline logic (Global Variables) designed to standardize CI/CD workflows across multiple projects. By centralizing logic here, we ensure consistent testing, security scanning, and deployment patterns for both **Infrastructure (Terraform)** and **Applications (Node.js/Docker/Helm)**.

---

## Directory Structure

The `vars/` directory contains Groovy scripts that act as custom steps in any `Jenkinsfile`:

* **`dockerBuild.groovy`**: Handles container image creation with idempotent checks to prevent "container name already in use" errors.
* **`postToGitHub.groovy`**: Automates posting build summaries, test results, and coverage reports to GitHub Pull Request comments.
* **`helmDeploy.groovy`**: Standardizes application deployment to **EKS clusters** using Helm charts.
* **`terraformPlan.groovy`**: Executes terraform plans and captures no-color output for clean PR reviews.

---

## How to Use

### 1. Register the Library in Jenkins
1.  Navigate to **Manage Jenkins** > **System**.
2.  Under **Global Pipeline Libraries**, click **Add**.
3.  **Name**: `jenkins-shared-library`
4.  **Default version**: `main`
5.  **Retrieval method**: Modern SCM (Git) pointing to this repository URL.

### 2. Call the Library in your Jenkinsfile
Add the library reference at the top of your `Jenkinsfile`. You can then call the scripts in your `vars/` folder as if they were built-in Jenkins commands.

```groovy
@Library('jenkins-shared-library') _

pipeline {
    agent any
    stages {
        stage('Build & Test') {
            steps {
                // Using shared library logic
                dockerBuild(imageName: "my-app", port: "27017")
            }
        }
    }
    post {
        always {
            // Automatically posts results to the PR conversation
            postToGitHub()
        }
    }
}
