# weasis-core-img #

[![License](https://img.shields.io/badge/License-EPL%202.0-blue.svg)](https://opensource.org/licenses/EPL-2.0) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Maven build](https://github.com/nroduit/weasis-core-img/workflows/Build/badge.svg?branch=master)

[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=ncloc)](https://sonarcloud.io/component_measures?id=weasis-core-img) [![Sonar](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=reliability_rating)](https://sonarcloud.io/component_measures?id=weasis-core-img) [![Sonar](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=sqale_rating)](https://sonarcloud.io/component_measures?id=weasis-core-img) [![Sonar](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=security_rating)](https://sonarcloud.io/component_measures?id=weasis-core-img) [![Sonar](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=alert_status)](https://sonarcloud.io/dashboard?id=weasis-core-img) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=coverage)](https://sonarcloud.io/summary/new_code?id=weasis-core-img)

This project provides a Java wrapping of OpenCV with DICOM capabilities.

Code formatter: [google-java-format](https://github.com/google/google-java-format)

## Build weasis-core-img ##

Prerequisites: JDK 17 or higher and Maven 3

Execute the maven command `mvn clean install` in the root directory of the project.

Note: This project has a native library dependency which must be included in your application and launched with a specific JVM option (e.g. '-Djava.library.path="path/of/native/lib"'). Additional systems and architectures of the native library are available directly from [this Maven repository](https://github.com/nroduit/mvn-repo/tree/master/org/weasis/thirdparty/org/opencv/libopencv_java). 