#!/usr/bin/env groovy

import groovy.xml.XmlSlurper

def report = new File(basedir, 'module-a/target/CodeNarcXmlReport.xml')
assert report.file

def root = new XmlSlurper().parse(report)

def packageSummary = root.PackageSummary
assert packageSummary.@totalFiles == 1
assert packageSummary.@filesWithViolations == 1

// only the rule in custom-ruleset.xml is applied
def examplePackage = root.Package.find { it.@path == 'org/example' }
def targetGroovy = examplePackage.File.find { it.@name == 'Target.groovy' }
assert targetGroovy.Violation.size() == 1
assert targetGroovy.Violation.@ruleName == 'ClassJavadoc'
