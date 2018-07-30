# code-coverage-api-plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/code-coverage-api-plugin/job/dev/badge/icon)](https://ci.jenkins.io/job/Plugins/job/code-coverage-api-plugin/job/dev/)
[![Gitter](https://badges.gitter.im/jenkinsci/code-coverage-api-plugin.svg)](https://gitter.im/jenkinsci/code-coverage-api-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)


This plugin serves as API to integrate and publish multiple coverage report types. 
More information see [https://jenkins.io/projects/gsoc/2018/code-coverage-api-plugin/](https://jenkins.io/projects/gsoc/2018/code-coverage-api-plugin/).

## Feature
- Pipeline support.
- Modernized coverage chart.
- Coverage trend.
- Multi-report aggregated.
- Rest API.
- Auto detect report.
- Easy to implement.

## How to use it

Code Coverage API plugin now supports Cobertura, Jacoco and llvm-cov.

1. Config coverage tool to generate reports.
2. Install Jenkins plugins which implement Code Coverage API plugin (cobertura-plugin, llvm-cov-plugin).
3. Enable "Publish Coverage Report" publisher in the Post-build Actions
4. Add your coverage tool adapter and specify reports path.
![alt text](./images/config-add-adapter.png "Add coverage adapter")
5. (Optional) Specify Thresholds of each metrics in global or adapter level.
6. (Optional) Specify Source code storing level to enable source code navigation.
![alt text](./images/config.png "Config")
