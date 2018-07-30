# code-coverage-api-plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/code-coverage-api-plugin/job/dev/badge/icon)](https://ci.jenkins.io/job/Plugins/job/code-coverage-api-plugin/job/dev/)
[![Gitter](https://badges.gitter.im/jenkinsci/code-coverage-api-plugin.svg)](https://gitter.im/jenkinsci/code-coverage-api-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)


This plugin serves as API to integrate and publish multiple coverage report types. 
More information see [https://jenkins.io/projects/gsoc/2018/code-coverage-api-plugin/](https://jenkins.io/projects/gsoc/2018/code-coverage-api-plugin/).

## Features
- Pipeline support
- Modernized coverage chart
- Coverage trend support
- Multi-report aggregated
- Rest API
- Auto detect report
- Easy to implement

## Supported Tools
#### Embedded tools
- [Jacoco](https://www.jacoco.org/jacoco/trunk/doc/)
#### Other plugins as an Extension of Code Coverage API plugin
- [Cobertura](http://cobertura.github.io/cobertura/) ([Cobertura Plugin](https://github.com/jenkinsci/cobertura-plugin))
- [llvm-cov](https://github.com/llvm-mirror/clang/blob/master/docs/SourceBasedCodeCoverage.rst) ([llvm-cov plugin](https://github.com/jenkinsci/llvm-cov-plugin))

## Release Notes
See the [CHANGELOG](CHANGELOG.md).

## How to use it

#####  1. Config coverage tool to generate reports.

Config maven to generate Cobertura coverage reports:
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

Config maven to generate Jacoco coverage reports:

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
More Information about [Jacoco](https://www.jacoco.org/jacoco/trunk/doc/).

Use llvm-cov to generate JSON format report:
```
$ llvm-cov export -instr-profile /path/to/foo.profdata /path/to/foo
```
More Information - [llvm-cov](https://github.com/llvm-mirror/clang/blob/master/docs/SourceBasedCodeCoverage.rst#exporting-coverage-data).

##### 2. Install Jenkins plugins which implement Code Coverage API plugin (cobertura-plugin, llvm-cov-plugin).
##### 3. Enable "Publish Coverage Report" publisher in the Post-build Actions.
##### 4. Add your coverage tool adapter and specify reports path.
![alt text](./images/config-add-adapter.png "Add coverage adapter")
##### 5. (Optional) Specify Thresholds of each metrics in global or adapter level.
##### 6. (Optional) Specify Source code storing level to enable source code navigation.
![alt text](./images/config.png "Config")
