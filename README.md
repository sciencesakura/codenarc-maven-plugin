# CodeNarc Maven Plugin

![](https://github.com/sciencesakura/codenarc-maven-plugin/actions/workflows/build.yaml/badge.svg) [![Maven Central](https://maven-badges.sml.io/sonatype-central/com.sciencesakura/codenarc-maven-plugin/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.sciencesakura/codenarc-maven-plugin)

A Maven plugin that integrates [CodeNarc](https://codenarc.org/), a static analysis tool for Groovy.

## Requirements

* Java 11+
* Maven 3.6.3+

## Installation and usage

The CodeNarc Maven Plugin is available on Maven Central. Add the `<plugin>` element to your `pom.xml` as shown below:

```xml
<build>
  <plugins>
    ...
    <plugin>
      <groupId>com.sciencesakura</groupId>
      <artifactId>codenarc-maven-plugin</artifactId>
      <version>1.0.0</version>
      <executions>
        <execution>
          <!-- by default, codenarc:check goal is bound to the verify phase -->
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

### Configuring rules

By default, the CodeNarc Maven Plugin looks for a ruleset file at `${project.basedir}/config/codenarc/codenarc.xml`. If the file does not exist, the plugin falls back to the built-in `rulesets/basic.xml` ruleset.

You can specify the ruleset by setting the `ruleset` parameter.

```xml
<plugin>
  <groupId>com.sciencesakura</groupId>
  <artifactId>codenarc-maven-plugin</artifactId>
  <version>1.0.0</version>
  <configuration>
    <!-- specify the built-in rulesets -->
    <ruleset>rulesets/basic.xml,rulesets/groovyism.xml</ruleset>
    <!-- or specify your own ruleset file -->
    <ruleset>${project.basedir}/my-codenarc-ruleset.xml</ruleset>
  </configuration>
</plugin>
```

### Override CodeNarc version

You can override the CodeNarc version by adding a dependency to the `<plugin>` element.

**CAUTION**: The CodeNarc Maven Plugin is built against Groovy 4. Make sure to use a compatible CodeNarc version (e.g., `x.x.x-groovy-4.x`).

```xml
<plugin>
  <groupId>com.sciencesakura</groupId>
  <artifactId>codenarc-maven-plugin</artifactId>
  <version>1.0.0</version>
  <dependencies>
    <dependency>
      <groupId>org.codenarc</groupId>
      <artifactId>CodeNarc</artifactId>
      <version>${codenarc.version}</version>
    </dependency>
  </dependencies>
</plugin>
```

### Other configurations

The following configuration parameters and their default values are available:

```xml
<plugin>
  <groupId>com.sciencesakura</groupId>
  <artifactId>codenarc-maven-plugin</artifactId>
  <version>1.0.0</version>
  <configuration>
    <sourceDirectories><!-- ${project.compileSourceRoots} --></sourceDirectories>
    <testSourceDirectories><!-- ${project.testCompileSourceRoots} --></testSourceDirectories>
    <includes>
      <include>**/*.groovy</include>
    </includes>
    <excludes></excludes>
    <ruleset>${project.basedir}/config/codenarc/codenarc.xml</ruleset>
    <outputFile>${project.build.directory}/CodeNarcXmlReport.xml</outputFile>
    <includeTests>false</includeTests>
    <consoleOutput>false</consoleOutput>
    <failOnError>true</failOnError>
    <failOnViolation>true</failOnViolation>
  </configuration>
</plugin>
```

For more information, see the [Maven site](https://www.sciencesakura.com/codenarc-maven-plugin/plugin-info.html).

## Acknowledgments

* [gleclaire/codenarc-maven-plugin](https://github.com/gleclaire/codenarc-maven-plugin) - The original CodeNarc Maven Plugin.

## License

This plugin is licensed under the Apache License 2.0.

Copyright (c) 2026 sciencesakura.
