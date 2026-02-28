// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven

import javax.inject.Inject
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.codenarc.CodeNarcRunner
import org.codenarc.analyzer.FilesystemSourceAnalyzer
import org.codenarc.analyzer.SourceAnalyzer
import org.codenarc.report.HtmlReportWriter
import org.codenarc.report.JsonReportWriter
import org.codenarc.report.ReportWriter
import org.codenarc.report.TextReportWriter
import org.codenarc.report.XmlReportWriter

/**
 * Runs CodeNarc analysis on the project's source code and generates a report of any violations found.
 *
 * Call {@code mvn codenarc:check} to execute the analysis.
 *
 * @author sciencesakura
 */
@Mojo(name = 'check', defaultPhase = LifecyclePhase.VERIFY)
class CheckMojo extends AbstractMojo {

  private final MavenProject project

  /**
   * Specifies the location of the source directories to analyze.
   * Default value is {@code ${project.compileSourceRoots}}.
   */
  @Parameter(defaultValue = '${project.compileSourceRoots}', required = true)
  private List<String> sourceDirectories

  /**
   * Specifies the location of the test source directories to analyze.
   * Default value is {@code ${project.testCompileSourceRoots}}.
   * This is only used if {@link #includeTests} is set to {@code true}.
   */
  @Parameter(defaultValue = '${project.testCompileSourceRoots}')
  private List<String> testSourceDirectories

  /**
   * Specifies the file patterns to include in the analysis.
   * Default value is <code>**&#47;*.groovy</code>.
   */
  @Parameter(property = 'codenarc.includes', defaultValue = '**/*.groovy', required = true)
  private List<String> includes

  /**
   * Specifies the file patterns to exclude from the analysis.
   */
  @Parameter(property = 'codenarc.excludes')
  private List<String> excludes

  /**
   * Specifies the location of the CodeNarc ruleset file.
   * Default value is {@code ${project.basedir}/config/codenarc/codenarc.xml}.
   * If the default file does not exist, it will fall back to using the built-in "rulesets/basic.xml".
   */
  @Parameter(property = 'codenarc.ruleset', defaultValue = '${project.basedir}/config/codenarc/codenarc.xml',
      required = true)
  private String ruleset

  /**
   * Specifies the location of the CodeNarc report file.
   * Default value is {@code ${project.build.directory}/CodeNarcXmlReport.xml}.
   * The report format is determined by the file extension: {@code .html} for HTML, {@code .json} for JSON,
   * {@code .txt} for plain text, and any other extension for XML.
   */
  @Parameter(property = 'codenarc.output.file', defaultValue = '${project.build.directory}/CodeNarcXmlReport.xml')
  private File outputFile

  /**
   * Whether to include test source directories in the analysis.
   * Default value is {@code false}.
   */
  @Parameter(property = 'codenarc.includeTests', defaultValue = 'false')
  private boolean includeTests

  /**
   * Whether to output the analysis results to the console.
   * Default value is {@code false}.
   */
  @Parameter(property = 'codenarc.consoleOutput', defaultValue = 'false')
  private boolean consoleOutput

  /**
   * Whether to fail the build if an error occurs during analysis.
   * Default value is {@code true}.
   */
  @Parameter(property = 'codenarc.failOnError', defaultValue = 'true')
  private boolean failOnError

  /**
   * Whether to fail the build if any violations are found.
   * Default value is {@code true}.
   */
  @Parameter(property = 'codenarc.failOnViolation', defaultValue = 'true')
  private boolean failOnViolation

  /**
   * Constructs a new {@code CheckMojo}.
   *
   * @param project the Maven project
   */
  @Inject
  CheckMojo(MavenProject project) {
    this.project = project
  }

  /**
   * {@inheritDoc}
   */
  @Override
  void execute() {
    if (log.debugEnabled) {
      debugProperties()
    }
    def runner = new CodeNarcRunner(
        ruleSetFiles: makeRuleSetFiles(),
        sourceAnalyzer: makeSourceAnalyzer(),
        reportWriters: makeReportWriters(),
    )
    def results = runner.execute()
    def violationCount = results.violations.size()
    if (violationCount) {
      def message = "CodeNarc analysis found ${violationCount} violation${violationCount == 1 ? '' : 's'}."
      if (outputFile != null) {
        message += " See ${outputFile.path} for details."
      }
      if (failOnViolation) throw new MojoExecutionException(message)
      log.warn(message)
    } else {
      log.info('CodeNarc analysis completed with no violations found.')
    }
  }

  private String makeRuleSetFiles() {
    def defaultRuleSet = new File(project.basedir, 'config/codenarc/codenarc.xml')
    if (ruleset == defaultRuleSet.path && !defaultRuleSet.exists()) {
      log.info('"${project.basedir}/config/codenarc/codenarc.xml" not found. Using "rulesets/basic.xml" instead.')
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

  private void debugProperties() {
    log.with {
      debug("Source directories: ${sourceDirectories}")
      debug("Test source directories: ${testSourceDirectories}")
      debug("Includes: ${includes}")
      debug("Excludes: ${excludes}")
      debug("Ruleset files: ${ruleSetFiles}")
      debug("Output file: ${outputFile}")
      debug("Include tests: ${includeTests}")
      debug("Console output: ${consoleOutput}")
      debug("Fail on error: ${failOnError}")
      debug("Fail on violation: ${failOnViolation}")
    }
  }
}
