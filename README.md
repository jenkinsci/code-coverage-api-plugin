# Jenkins Code Coverage Plug-in

[![Gitter](https://badges.gitter.im/jenkinsci/code-coverage-api-plugin.svg)](https://gitter.im/jenkinsci/code-coverage-api-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/code-coverage-api.svg?color=red)](https://plugins.jenkins.io/code-coverage-api)
[![Jenkins](https://ci.jenkins.io/job/Plugins/job/code-coverage-api-plugin/job/master/badge/icon?subject=Jenkins%20CI)](https://ci.jenkins.io/job/Plugins/job/code-coverage-api-plugin/job/master/)
[![GitHub Actions](https://github.com/jenkinsci/code-coverage-api-plugin/workflows/GitHub%20CI/badge.svg?branch=master)](https://github.com/jenkinsci/code-coverage-api-plugin/actions)
[![Codecov](https://codecov.io/gh/jenkinsci/code-coverage-api/branch/master/graph/badge.svg)](https://codecov.io/gh/jenkinsci/code-coverage-api-plugin/branch/master)


This Jenkins plugin integrates and publishes multiple coverage report types.
It has been developed since [GSoC 2018](https://jenkins.io/projects/gsoc/2018/code-coverage-api-plugin/).

* [Supported coverage formats](#Supported-Coverage-Formats)
* [Release notes](#Release-Notes)
* [Features](#Features)
* [Usage](#Usage)


## Supported Coverage Formats
#### Embedded
- [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/)
- [Istanbul](https://istanbul.js.org/) - [Cobertura Reporter](https://istanbul.js.org/docs/advanced/alternative-reporters/#cobertura)
- [Cobertura](http://cobertura.github.io/cobertura/)

#### Other plugins as an Extension of Code Coverage API plugin
- [llvm-cov](https://github.com/llvm-mirror/clang/blob/master/docs/SourceBasedCodeCoverage.rst) ([llvm-cov plugin](https://github.com/jenkinsci/llvm-cov-plugin))
- [OpenCover](https://github.com/OpenCover/opencover) ([OpenCover Plugin](https://github.com/jenkinsci/opencover-plugin))

## Release Notes
See the [GitHub Releases](https://github.com/jenkinsci/code-coverage-api-plugin/releases).

## Features
### General Features
* Pipeline support
* Parallel execution in pipeline support
* Reports combining
* REST API
* Failed conditions and flexible threshold setting

### Coverage Analysis
* **Coverage analysis of whole projects and pull requests:**
  * complete code (Project Coverage)
  * code changes (Change Coverage)
  * coverage changes created by changed test cases (Indirect Coverage Changes)
* **Modernized coverage report visualization:**
  * Coverage overview and trend

    ![alt text](./images/reportOverview_screen.PNG "Coverage overview and trend")
  
  * Colored project coverage tree map for line and branch coverage

    ![alt text](./images/reportTree_screen.PNG "Colored project coverage tree map")
  
  * Source code navigation

    ![alt text](./images/reportFile_screen.PNG "Source code navigation")
  
  * Specific source code view for specifically analyzing the coverage of code changes (Change Coverage):

    ![alt text](./images/reportCC_screen.PNG "Specific source code view for Change Coverage")
   
  * Specific source code view for specifically analyzing the coverage after test changes (Indirect Coverage Changes):

    ![alt text](./images/reportICC_screen.PNG "Specific source code view for Indirect Coverage Changes")

* **Customizable coverage overview for the Jenkins dashboard view and for build results:**
  ![alt text](./images/dashboard_screen.PNG "Analysis overview for Jenkins dashboard")
  ![alt text](./images/buildview_screen.PNG "Analysis overview for Jenkins build result")

## Usage

###  1. Configure your coverage tool to generate reports

#### Cobertura based coverage

Configure Maven to generate Cobertura coverage reports:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>cobertura-maven-plugin</artifactId>
            <version>2.7</version>
            <configuration>
                <formats>
                    <format>xml</format>
                </formats>
                <check/>
            </configuration>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>cobertura</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
More information about [Cobertura](http://cobertura.github.io/cobertura/).

#### JaCoCo based coverage

Configure Maven to generate JaCoCo coverage reports:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.1</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>package</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
More Information about [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/).

#### llvm-cov based coverage

Use llvm-cov to generate JSON format report:
```
$ llvm-cov export -instr-profile /path/to/foo.profdata /path/to/foo
```
More Information  [llvm-cov](https://github.com/llvm-mirror/clang/blob/master/docs/SourceBasedCodeCoverage.rst#exporting-coverage-data).

### 2. (Optional) Install Jenkins plugins which implement Code Coverage API plugin (cobertura-plugin, llvm-cov-plugin).
### 3. Enable "Publish Coverage Report" publisher in the Post-build Actions.
### 4. Add your coverage tool adapter and specify reports path.
![alt text](./images/config-add-adapter.png "Add coverage adapter")
### 5. (Optional) Use the [forensics-api](https://github.com/jenkinsci/forensics-api-plugin) plugin to discover the reference build that is used to compute a delta report of the coverage results. 
### 6. (Optional) Specify Thresholds of each metrics in global or adapter level.
### 7. (Optional) Specify Source code storing level to enable source code navigation.
![alt text](./images/config.png "Config")

## Pipeline example
We also support pipeline configuration, you can generate pipeline code in Jenkins Snippet Generator.

```groovy

publishCoverage adapters: [jacocoAdapter('target/site/jacoco/jacoco.xml')]

```
You can also use `jacoco` instead of `jacocoAdapter` if you didn't install Jacoco-Plugin.

##### Parallel Pipeline Support
We support parallel pipeline. You can call the Code Coverage API plugin in different branches like this:
```groovy
node {
    parallel firstBranch: {
        publishCoverage adapters: [jacocoAdapter('target/site/jacoco/jacoco.xml')]
}, secondBranch: {
        publishCoverage adapters: [jacocoAdapter('jacoco.xml')]
    }
}
```
##### Reports Combining Support
You can add tag on publishCoverage and Code Coverage API plugin will combine reports have same tag:

```
node {
    parallel firstBranch: {
        publishCoverage adapters: [jacocoAdapter('target/site/jacoco/jacoco.xml')], tag: ‘t’
}, secondBranch: {
        publishCoverage adapters: [jacocoAdapter('jacoco.xml')], tag: ‘t’
    }
}
```
##### Merging Reports
There is also a possibility to merge multiple reports (e.g. from multiple xml files) into one using the `mergeToOneReport` option with an ant-style path pattern.
All reports found by the adapter will then be combined into a single report:

```
publishCoverage adapters: [jacocoAdapter(mergeToOneReport: true, path: '**/*.xml')]
```
## REST API
We provide a REST API to retrieve coverage data:
- Coverage result: `…​/{buildNumber}/coverage/…​/result/api/\{json|xml\}?depth={number}`
- Trend result: `…​/{buildNumber}/coverage/…​/trend/api/\{json|xml\}?depth={number}`
- Coverage result of last build: `…​/{buildNumber}/coverage/…​/last/result/api/\{json|xml\}?depth={number}`
- Trend result of last build: `…​/{buildNumber}/coverage/…​/last/trend/api/\{json|xml\}?depth={number}`

Note: The larger the number, the deeper of coverage information can be retrieved.
