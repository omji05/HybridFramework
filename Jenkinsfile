pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    parameters {
        choice(
            name: 'SUITE',
            choices: ['default', 'smoke', 'regression', 'api'],
            description: 'BDD suite profile (default = all scenarios)'
        )
        choice(
            name: 'BROWSER',
            choices: ['chrome', 'firefox', 'edge'],
            description: 'Browser for UI tests'
        )
        booleanParam(
            name: 'HEADLESS',
            defaultValue: true,
            description: 'Run browser in headless mode'
        )
        string(
            name: 'TAGS',
            defaultValue: '',
            description: 'Cucumber tags filter (e.g., @smoke, @api and not @regression)'
        )
        choice(
            name: 'ENVIRONMENT',
            choices: ['qa', 'staging', 'prod'],
            description: 'Target environment profile (loads environments/config-{env}.properties)'
        )
    }

    environment {
        MAVEN_OPTS = '-Xmx1024m'
        API_LOG_VERBOSE = 'false'
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        stage('Build & Resolve Dependencies') {
            steps {
                bat 'mvn clean compile -DskipTests'
            }
        }

        stage('Run Tests') {
            steps {
                script {
                    def suiteArg = (params.SUITE == 'default')
                        ? ''
                        : "-P${params.SUITE}"

                    def tagsArg = params.TAGS?.trim()
                        ? "-Dcucumber.filter.tags=\"${params.TAGS}\""
                        : ''

                    bat """
                        mvn test ${suiteArg} \
                            -Denvironment=${params.ENVIRONMENT} \
                            -Dbrowser=${params.BROWSER} \
                            -Dheadless=${params.HEADLESS} \
                            ${tagsArg} \
                            || exit 0
                    """
                }
            }
            post {
                always {
                    // Archive TestNG results
                    testNG(reportFilenamePattern: '**/testng-results.xml')
                }
            }
        }

        stage('Generate Reports') {
            parallel {
                stage('Allure Report') {
                    steps {
                        bat 'mvn allure:report'
                    }
                }
                stage('Archive Extent Report') {
                    steps {
                        echo 'Extent Report generated in reports/extent/'
                    }
                }
            }
        }
    }

    post {
        always {
            // ── Allure Report (Jenkins plugin — interactive in Jenkins UI) ──
            allure([
                results: [[path: 'target/allure-results']],
                reportBuildPolicy: 'ALWAYS'
            ])

            // ── Allure static report (downloadable artifact) ──
            archiveArtifacts(
                artifacts: 'target/site/allure-maven-plugin/**',
                allowEmptyArchive: true,
                fingerprint: true
            )

            // ── Allure raw results (for re-generation elsewhere) ──
            archiveArtifacts(
                artifacts: 'target/allure-results/**',
                allowEmptyArchive: true
            )

            // ── Extent, Cucumber, and logs ──
            archiveArtifacts(
                artifacts: 'reports/extent/*.html, target/cucumber-reports/*, logs/*.log',
                allowEmptyArchive: true,
                fingerprint: true
            )

            // ── Screenshots on failure ──
            archiveArtifacts(
                artifacts: 'reports/screenshots/*.png',
                allowEmptyArchive: true
            )
        }

        failure {
            echo 'Pipeline FAILED — check the reports and logs.'
            // Uncomment for email notification:
            // mail to: 'team@yourorg.com',
            //      subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //      body: "Check: ${env.BUILD_URL}"
        }

        success {
            echo 'Pipeline PASSED — all tests executed successfully.'
        }

        cleanup {
            cleanWs()
        }
    }
}
