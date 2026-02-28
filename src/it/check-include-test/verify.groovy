#!/usr/bin/env groovy

import groovy.xml.XmlSlurper

def report = new File(basedir, 'target/CodeNarcXmlReport.xml')
assert report.file

def root = new XmlSlurper().parse(report)

def packageSummary = root.PackageSummary
assert packageSummary.@totalFiles == 2
assert packageSummary.@filesWithViolations == 2

def examplePackage = root.Package.find { it.@path == 'org/example' }
def targetGroovy = examplePackage.File.find { it.@name == 'Target.groovy' }
assert targetGroovy.Violation.@ruleName == 'ClassJavadoc'
def targetTestGroovy = examplePackage.File.find { it.@name == 'TargetTest.groovy' }
assert targetTestGroovy.Violation.@ruleName == 'ClassJavadoc'
