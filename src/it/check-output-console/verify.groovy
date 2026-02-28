#!/usr/bin/env groovy

import groovy.xml.XmlSlurper

def report = new File(basedir, 'target/CodeNarcXmlReport.xml')
assert report.file

def root = new XmlSlurper().parse(report)

def packageSummary = root.PackageSummary[0]
assert packageSummary.@totalFiles == 1
assert packageSummary.@filesWithViolations == 0
