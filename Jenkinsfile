node {
    stage('Prepare') {
        checkout scm
    }

    properties([
        [$class: 'jenkins.model.BuildDiscarderProperty',
            strategy: [
                $class: 'LogRotator',
                artifactDaysToKeepStr: '8',
                artifactNumToKeepStr: '3',
                daysToKeepStr: '15',
                numToKeepStr: '5']
        ]
    ]);

    withEnv([
        "JAVA_HOME=${ tool 'OpenJDK11' }",
        "PATH+MAVEN=${tool 'Maven CURRENT'}/bin:${ tool 'OpenJDK11' }/bin"
    ]) {

            stage('Build') {
                echo "Building branch: ${env.BRANCH_NAME}"
                sh "mvn clean package -Dmaven.test.skip=true -B -V -e -fae -q --global-toolchains .jenkins/toolchains.xml"
            }

            stage('Check keystore') {
                echo "Check welke certificaten de komende 90 dagen verlopen"
                sh "./jks-certificate-expiry-checker.sh --keystore ./src/main/resources/pkioverheid.jks --password changeit -t90"
            }

            lock('http-8088') {
                stage('Test') {
                    echo "Running unit tests"
                    sh "mvn -e test -B --global-toolchains .jenkins/toolchains.xml"
                }
            }

            stage('Publish Test Results') {
                junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml, **/target/failsafe-reports/TEST-*.xml'
            }

            stage('Test Coverage results') {
                jacoco exclusionPattern: '**/*Test.class', execPattern: '**/target/**.exec'
            }

            stage('OWASP Dependency Check') {
                echo "Uitvoeren OWASP dependency check"
                sh "mvn org.owasp:dependency-check-maven:check"
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml', failedNewCritical: 1, failedNewHigh: 1, failedTotalCritical: 1, failedTotalHigh: 3, unstableTotalHigh: 2
            }
        }
    }
