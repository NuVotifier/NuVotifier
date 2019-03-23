pipeline {
    agent {
        docker { image 'cogniteev/oracle-java' }
    }

    environment {
        SNAPSHOT_REPO   = credentials('ibj-nexus-snapshot-repo')
        RELEASE_REPO    = credentials('ibj-nexus-release-repo')
        RAW_UPLOAD_PATH = credentials('ibj-nexus-raw-path')
    }

    withCredentials([[$class: 'UsernamePasswordMultiBinding', 
        credentialsId: 'jenkins-ibj-io',
        usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
        REPO_USERNAME   = credentials('ibj-nexus-repo-username')
        REPO_PASSWORD   = credentials('ibj-nexus-repo-password')
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew build --no-daemon'            
            }
        }
        
        stage('Test') {
            steps {
                sh './gradlew test --no-daemon'            
            }
        }
        
        stage('Publish') {
            when {
                branch "master"
            }
            steps {
                sh './gradlew publishversionedPublicationToIbjBlockRepository'
                sh './gradlew publishLatestToIbjBlockRaw'
            }
        }
    }
}
