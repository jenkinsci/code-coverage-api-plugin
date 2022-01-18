buildPlugin(failFast: false,
    checkstyle: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]],
                 filters:[includePackage('io.jenkins.plugins.coverage.model')]],
    pmd: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]],
          filters:[includePackage('io.jenkins.plugins.coverage.model')]],
    spotbugs: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]],
               filters:[includePackage('io.jenkins.plugins.coverage.model')]])
