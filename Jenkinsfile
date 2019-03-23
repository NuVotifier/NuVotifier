pipeline {
    agent {
        docker { image 'cogniteev/oracle-java' }
    }

    environment {
        SNAPSHOT_REPO   = credentials('ibj-nexus-snapshot-repo')
        RELEASE_REPO    = credentials('ibj-nexus-release-repo')
        RAW_UPLOAD_PATH = credentials('ibj-nexus-raw-path')
        REPO            = credentials('ibj-nexus-access')
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
