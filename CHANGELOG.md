# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- feat: add the `skip` parameter (`codenarc.skip` property) to skip the analysis.

### Changed
- Bump org.apache.maven.plugins:maven-invoker-plugin from 3.9.1 to 3.10.1 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/19).
- Bump org.junit.jupiter:junit-jupiter from 6.0.3 to 6.1.2 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/20).
- build: Upgrade the Java used for building from 17 to 21 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/23).
- build(deps): Bump com.puppycrawl.tools:checkstyle from 12.3.1 to 13.9.0 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/21).
- Bump org.apache.maven.plugins:maven-jar-plugin from 3.5.0 to 3.5.1 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/22).
- build(deps): Bump org.apache.groovy:groovy-bom from 5.0.7 to 5.0.8 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/24).
- refactor: Rewrite the plugin implementation from Groovy to Java (https://github.com/sciencesakura/codenarc-maven-plugin/pull/35).
- build(deps): Bump org.apache.maven:apache-maven from 3.9.11 to 3.9.16 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/25).
- build(deps-dev): Bump org.junit.jupiter:junit-jupiter from 6.1.2 to 6.1.3 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/26).
- build(deps): Bump actions/setup-java from 5 to 6 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/30).
- build(deps): Bump com.puppycrawl.tools:checkstyle from 13.9.0 to 14.1.0 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/34).
- ci: prevent parallel runs (https://github.com/sciencesakura/codenarc-maven-plugin/pull/37).
- build: remove explicit maven-compiler-plugin setting (https://github.com/sciencesakura/codenarc-maven-plugin/pull/38).

### Fixed
- fix: resolve relative file paths in the `ruleset` parameter against `${project.basedir}` instead of the working directory.

## [1.0.2] - 2026-07-23
### Changed
- Upgrade CodeNarc dependency to 4.0.0 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/7).
- build: Add Dependabot (https://github.com/sciencesakura/codenarc-maven-plugin/pull/8).
- Bump actions/checkout from 6 to 7.0.0 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/9).
- Bump org.apache.maven.plugins:maven-enforcer-plugin from 3.6.2 to 3.6.3 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/10).
- Bump org.apache.maven.plugins:maven-site-plugin from 3.21.0 to 3.22.0 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/11).
- Bump org.jacoco:jacoco-maven-plugin from 0.8.14 to 0.8.15 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/13).
- Bump org.projectlombok:lombok from 1.18.42 to 1.18.46 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/15).
- Bump org.sonatype.central:central-publishing-maven-plugin from 0.10.0 to 0.11.0 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/16).
- chore(deps): Upgrade Groovy from 4.0.30 to 5.0.7 (https://github.com/sciencesakura/codenarc-maven-plugin/pull/17).
- chore: Update ruleset-schema.xsd's URL path from /v3.7.0/ to /v4.0.0/ (https://github.com/sciencesakura/codenarc-maven-plugin/pull/18).

## [1.0.1] - 2026-03-08
### Fixed
- Add `src/main/groovy` to `sourceDirectories` if it exists and is not yet included (https://github.com/sciencesakura/codenarc-maven-plugin/pull/1).

## [1.0.0] - 2026-03-02
### Added
- Initial release.
