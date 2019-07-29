Changelog
===
# 1.0.13
Release date: Jul 29, 2019

Enhancemens:
* [PR #104](https://github.com/jenkinsci/code-coverage-api-plugin/pull/104) - add a interface to provide default value for MergeToOneReport for adapter.

Fix issues:
* [PR #103](https://github.com/jenkinsci/code-coverage-api-plugin/pull/103) - NullPointerException when merge report between action.

# 1.0.12
Release date: Jul 15, 2019

Features:
* [PR #95](https://github.com/jenkinsci/code-coverage-api-plugin/pull/95) - Support view columu for coverage.
* [PR #98](https://github.com/jenkinsci/code-coverage-api-plugin/pull/98) - Support merge report in adapter level.

# 1.0.11
Release date: May 22, 2019

Features:
* [PR #83](https://github.com/jenkinsci/code-coverage-api-plugin/pull/83) - Show diff in coverage for change request builds.

Fix issues:
* [PR #85](https://github.com/jenkinsci/code-coverage-api-plugin/pull/85) - Globals Thresholds show repeated optional value.
* [PR #86](https://github.com/jenkinsci/code-coverage-api-plugin/pull/86) - Source files partially copied when transient agent is destroyed.

# 1.0.10
Release date: Apr 29, 2019

Enhancements:
* Add source file path support
* Improve performance of copying source files

Fix issues:
* Add serialVersionUID to CoverageElement and use custom ObjectInputStream to keep backwards compatibility.

# 1.0.9
Release date: Mar 27, 2019

Enhancements:
* Make the chart sorted.
* Other trivial improvements.


# 1.0.8
Release date: Mar 19, 2019

Enhancements:
* Add filter for each Threshold type.

Fix issues:
* Fixed JellyTagException.

# 1.0.7
Release date: Jan 22, 2019

Fix issues:
* [PR #66](https://github.com/jenkinsci/code-coverage-api-plugin/pull/66) - Fixed Threshold cannot be serialized.
* [PR #67](https://github.com/jenkinsci/code-coverage-api-plugin/pull/67) - Fixed Threhsold set in global level will be copied to adapter level.

# 1.0.6
Release date: Jan 9, 2019

Features:
* [PR #62](https://github.com/jenkinsci/code-coverage-api-plugin/pull/62) - Add Istanbul (Cobertura reporter) support.

Enhancements:
* Add Chinese localization.
* Add more console logs.

Fix issues:
* [PR #63](https://github.com/jenkinsci/code-coverage-api-plugin/pull/63) - Fixed source files not visible for jobs build on slaves.


# 1.0.5
Release date: Aug 28, 2018

Fix issues:
* [JENKINS-53242](https://issues.jenkins-ci.org/browse/JENKINS-53242) - Can't open "Coverage Report" of multibranch pipeline job because of a JellyTagException


# 1.0.4
Release date: Aug 24, 2018

Fix issues:
* [PR #49](https://github.com/jenkinsci/code-coverage-api-plugin/pull/49) - Fixed support for multiple coverage files.
* [JENKINS-53181](https://issues.jenkins-ci.org/browse/JENKINS-53181) - Jacoco adapter does not work if Jacoco plugin is installed too.

# 1.0.3
Release date: Aug 22, 2018

Fix issues:
* [JENKINS-53183](https://issues.jenkins-ci.org/browse/JENKINS-53183) - Throw NotSerializableException when used in master/slave set up
* SECURITY-1119 - potential xss vulnerability

# 1.0.2
Release date: Aug 21, 2018

Fix issues:
* Fix incorrect JaCoCo Branch calculating.

# 1.0.1
Release date: Aug 21, 2018

Fix issues:
* [JENKINS-53130](https://issues.jenkins-ci.org/browse/JENKINS-53130) - Code coverage 0% in jenkins with Jacoco.

Enhancements:
* Add INSTRUCTION metric in JaCoCo adapter.
* Improve coverage calculating to make it more accurate.

# 1.0.0

Release date: Aug 16, 2018

Features: 

* [JENKINS-51926](https://issues.jenkins-ci.org/browse/JENKINS-51926) - Supporting combining reports within a build(e.g. after parallel() execution in Pipeline).
* [JENKINS-52666](https://issues.jenkins-ci.org/browse/JENKINS-52666) - Show coverage at the top of the project page.

Enhancements:

* [JENKINS-52839](https://issues.jenkins-ci.org/browse/JENKINS-52839) - Make Code Coverage API plugin more generic.

Fix issues:

* [JENKINS-53023](https://issues.jenkins-ci.org/browse/JENKINS-53023) - Fix incorrect coverage calculating. 
* [JENKINS-52809](https://issues.jenkins-ci.org/browse/JENKINS-52809) - Invalid Jacoco report files also be parsed and add to aggregated report.
* Other trivial bugs.

# 1.0.0-rc-1

Release date: Jul 30, 2018

Features:

* [JENKINS-51422](https://issues.jenkins-ci.org/browse/JENKINS-51422) - Offer REST API to retrieve the coverage reports in machine-readable format.
* [JENKINS-51423](https://issues.jenkins-ci.org/browse/JENKINS-51423) - Offer REST API to retrieve the coverage trends in machine-readable format.
* [JENKINS-51736](https://issues.jenkins-ci.org/browse/JENKINS-51736) - Add support of llvm-cov Report converter.
* [JENKINS-51814](https://issues.jenkins-ci.org/browse/JENKINS-51814) - Create a Report Detector extension point.
* [JENKINS-51988](https://issues.jenkins-ci.org/browse/JENKINS-51988) - Add source code navigation.
* [JENKINS-51424](https://issues.jenkins-ci.org/browse/JENKINS-51424) - Prototype integration between Code Coverage API and Cobertura plugin.
* [JENKINS-52630](https://issues.jenkins-ci.org/browse/JENKINS-52630) - Migrate LLVM coverage out into new plugin.

Enhancements:

* [JENKINS-51927](https://issues.jenkins-ci.org/browse/JENKINS-51927) - Refactoring the configuration page to make it more user-friendly.

Fix issues:
* [JENKINS-52628](https://issues.jenkins-ci.org/browse/JENKINS-52628) - Change current REST API to the standard format.

# 1.0.0-alpha-1

Release date: Jun 14 , 2018

Features:

* Data model based on [Cobertura plugin](https://github.com/jenkinsci/cobertura-plugin).
* Add Cobertura support.
* Add Jacoco support.
* [JENKINS-51363](https://issues.jenkins-ci.org/browse/JENKINS-51363) - Pipeline support.
* [JENKINS-51364](https://issues.jenkins-ci.org/browse/JENKINS-51364) - Add Threshold conditions so that the reporter can fail the build depending on conditions.
* [JENKINS-51368](https://issues.jenkins-ci.org/browse/JENKINS-51368) - Modernize report chart.
* [JENKINS-51609](https://issues.jenkins-ci.org/browse/JENKINS-51609) - Add filter handler so that we can show coverage code in ranged.
* [JENKINS-51366](https://issues.jenkins-ci.org/browse/JENKINS-51366) - Add auto detector so we can find report automatically.
* [JENKINS-51611](https://issues.jenkins-ci.org/browse/JENKINS-51611) - Add unit tests.
