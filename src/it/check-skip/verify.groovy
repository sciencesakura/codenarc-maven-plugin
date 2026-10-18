#!/usr/bin/env groovy

def report = new File(basedir, 'target/CodeNarcXmlReport.xml')
assert !report.exists()

def buildLog = new File(basedir, 'build.log')
assert buildLog.text.contains('Skipping CodeNarc analysis.')
