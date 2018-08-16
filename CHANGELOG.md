Changelog
===

# 1.0.0

Release data: Aug 16, 2018

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