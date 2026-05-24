pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    triggers {
        cron('H H * * 1')
    }

    environment {
        BASE_URL = 'https://jeevan-04.github.io/Atithya'
        SELENIUM_HEADLESS = 'true'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare Browser') {
            steps {
                script {
                    env.CHROME_BINARY = sh(
                        script: '''
                            for candidate in \
                                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
                                "/Applications/Chromium.app/Contents/MacOS/Chromium"; do
                                if [ -x "$candidate" ]; then
                                    printf '%s' "$candidate"
                                    exit 0
                                fi
                            done
                            echo 'No Chrome-compatible browser found' >&2
                            exit 1
                        ''',
                        returnStdout: true
                    ).trim()
                }
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
    }

    post {
        always {
            junit 'target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'target/selenium-artifacts/**', allowEmptyArchive: true
        }
    }
}