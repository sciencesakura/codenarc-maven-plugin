# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Add a reporting goal that generates a report for the project site.
- Support the `codenarc.properties` file.
- Add `excludeGeneratedSources` parameter.

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
