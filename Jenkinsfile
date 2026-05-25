pipeline {
    agent any
    // Note: we intentionally do not require a named Jenkins JDK tool here because
    // some agents may not have a matching tool configured. The pipeline will
    // attempt to set JAVA_HOME in the 'Prepare Environment' stage instead.

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
        ARTIFACT_DIR = 'target/selenium-artifacts'
    }

    stages {
        stage('Checkout') {
            steps {
                // Ensure we do a clean checkout and initialize/update submodules if present
                checkout([$class: 'GitSCM', branches: scm.branches, userRemoteConfigs: scm.userRemoteConfigs, extensions: scm.extensions])
                sh 'git submodule update --init --recursive || true'
            }
        }

        stage('Prepare Environment') {
            steps {
                script {
                    // Try to set JAVA_HOME to a JDK 17 if available on the agent
                    env.JAVA_HOME = sh(script: "(command -v java >/dev/null && java -XshowSettings:properties -version 2>&1 | sed -n 's/.*java.home = //p' | head -1) || true", returnStdout: true).trim()
                    // On macOS agents prefer /usr/libexec/java_home
                    if (!env.JAVA_HOME) {
                        env.JAVA_HOME = sh(script: "( /usr/libexec/java_home -v 17 2>/dev/null || true)", returnStdout: true).trim()
                    }
                    if (env.JAVA_HOME) {
                        sh 'echo "Using JAVA_HOME=$JAVA_HOME"'
                        sh 'export JAVA_HOME="$JAVA_HOME" && java -version'
                    } else {
                        sh 'echo "WARNING: JAVA_HOME not set to JDK17; ensure agent has JDK17 available"'
                    }
                }
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
                    // Report chrome and chromedriver versions for diagnostics
                    sh 'echo "Chrome binary: $CHROME_BINARY"'
                    sh 'if command -v chromedriver >/dev/null 2>&1; then chromedriver --version || true; else echo "chromedriver not found"; fi'
                    sh 'if [ -x "$CHROME_BINARY" ]; then "$CHROME_BINARY" --version || true; fi'
                }
            }
        }

        stage('Test') {
            steps {
                // Run tests and capture surefire output. Allow failures to be recorded for investigation.
                sh 'mvn -B -DskipTests=false test'
            }
        }
    }

    post {
        always {
            junit 'target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'target/surefire-reports/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: "${env.ARTIFACT_DIR}/**", allowEmptyArchive: true
            // Also archive the full surefire-reports directory
            archiveArtifacts artifacts: 'target/surefire-reports/**', allowEmptyArchive: true
        }
    }
}