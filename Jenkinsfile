pipeline {

    agent {
        label 'general-purpose'
    }
    tools {
        jdk "zulu-25"
    }
    environment {
        JAVA_HOME = tool("zulu-25")
        payaraBuildNumber = "${BUILD_NUMBER}"
    }
    stages {
        stage('Build') {
            steps {
                script {
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    sh '''
                    ./gradlew clean check buildPlugin verifyPlugin
                    '''
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                }
            }
        }
    }
}
