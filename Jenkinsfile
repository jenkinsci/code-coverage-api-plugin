def configurations = [
  [ platform: "linux", jdk: "21" ],
  [ platform: "windows", jdk: "17" ]
]

def params = [
    failFast: false,
    configurations: configurations,
    checkstyle: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]],
            filters:[includePackage('io.jenkins.plugins.coverage.metrics')]],
    pmd: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]],
            filters:[includePackage('io.jenkins.plugins.coverage.metrics')]],
    spotbugs: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]],
            filters:[includePackage('io.jenkins.plugins.coverage.metrics')]],
    jacoco: [sourceCodeRetention: 'MODIFIED', sourceDirectories: [[path: 'plugin/src/main/java']]]
]

buildPlugin(params)
