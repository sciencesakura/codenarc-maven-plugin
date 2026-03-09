// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven

import org.apache.maven.plugin.Mojo
import org.apache.maven.project.MavenProject
import org.codehaus.groovy.runtime.InvokerHelper
import org.codenarc.CodeNarcRunner
import org.codenarc.analyzer.FilesystemSourceAnalyzer
import org.codenarc.analyzer.SourceAnalyzer
import org.codenarc.report.HtmlReportWriter
import org.codenarc.report.JsonReportWriter
import org.codenarc.report.ReportWriter
import org.codenarc.report.TextReportWriter
import org.codenarc.report.XmlReportWriter
import org.codenarc.results.Results

/**
 * A helper class that encapsulates the logic for invoking CodeNarc analysis and generating reports.
 *
 * @author sciencesakura
 */
class CodeNarcHelper {

  MavenProject project

  List<String> sourceDirectories

  List<String> testSourceDirectories

  List<String> includes

  List<String> excludes

  String ruleset

  File outputFile

  boolean includeTests

  boolean consoleOutput

  boolean failOnError

  static CodeNarcHelper make(Mojo mojo) {
    def instance = new CodeNarcHelper()
    InvokerHelper.setProperties(instance, mojo.properties)
    instance
  }

  Results analyze() {
    def runner = new CodeNarcRunner(
        ruleSetFiles: makeRuleSetFiles(),
        sourceAnalyzer: makeSourceAnalyzer(),
        reportWriters: makeReportWriters(),
    )
    runner.execute()
  }

  private String makeRuleSetFiles() {
    def defaultRuleSet = new File(project.basedir, 'config/codenarc/codenarc.xml')
    if (ruleset == defaultRuleSet.path && !defaultRuleSet.exists()) {
      return 'rulesets/basic.xml'
    }
    new File(ruleset).exists() ? "file:${ruleset}" : ruleset
  }

  private SourceAnalyzer makeSourceAnalyzer() {
    def includes = includes.join(',')
    def excludes = excludes?.join(',')
    def sourceAnalyzers = []
    sourceDirectories.findAll(Files::isDirectory).each {
      sourceAnalyzers << new FilesystemSourceAnalyzer(
          baseDirectory: it,
          includes: includes,
          excludes: excludes,
          failOnError: failOnError
      )
    }
    if (includeTests && testSourceDirectories != null) {
      testSourceDirectories.findAll(Files::isDirectory).each {
        sourceAnalyzers << new FilesystemSourceAnalyzer(
            baseDirectory: it,
            includes: includes,
            excludes: excludes,
            failOnError: failOnError
        )
      }
    }
    sourceAnalyzers.size() == 1 ? sourceAnalyzers.first() : new AggregateSourceAnalyzer(sourceAnalyzers)
  }

  private List<ReportWriter> makeReportWriters() {
    def writers = []
    if (outputFile != null) {
      def outputFileWriter = switch (Files.extension(outputFile)) {
        case ~/html?/ -> new HtmlReportWriter()
        case 'json' -> new JsonReportWriter()
        case 'txt' -> new TextReportWriter()
        default -> new XmlReportWriter()
      }
      outputFileWriter.title = "${project.artifactId} ${project.version}"
      outputFileWriter.outputFile = outputFile.path
      writers << outputFileWriter
    }
    if (consoleOutput) {
      writers << new TextReportWriter(writeToStandardOut: true)
    }
    writers
  }
}
