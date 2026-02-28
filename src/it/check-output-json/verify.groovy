#!/usr/bin/env groovy

import groovy.json.JsonSlurper

def report = new File(basedir, 'target/report.json')
assert report.file

def root = new JsonSlurper().parse(report)

def summary = root.summary
assert summary.totalFiles == 1
assert summary.filesWithViolations == 0
