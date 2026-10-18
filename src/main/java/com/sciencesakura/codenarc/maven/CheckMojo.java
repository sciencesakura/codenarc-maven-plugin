// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codenarc.CodeNarcRunner;
import org.codenarc.analyzer.FilesystemSourceAnalyzer;
import org.codenarc.analyzer.SourceAnalyzer;
import org.codenarc.report.AbstractReportWriter;
import org.codenarc.report.HtmlReportWriter;
import org.codenarc.report.JsonReportWriter;
import org.codenarc.report.ReportWriter;
import org.codenarc.report.TextReportWriter;
import org.codenarc.report.XmlReportWriter;
import org.codenarc.results.Results;

/**
 * Runs CodeNarc analysis on the project's source code.
 * Run {@code mvn codenarc:check} to execute the analysis.
 *
 * @author sciencesakura
 */
@Mojo(name = "check", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.VERIFY)
public class CheckMojo extends AbstractMojo {

  private final MavenProject project;

  /**
   * Specifies the source directories to analyze.
   * The plugin adds <code>${project.basedir}/src/main/groovy</code> to this list if it exists and is not already included.
   */
  @Parameter(defaultValue = "${project.compileSourceRoots}", required = true)
  private List<String> sourceDirectories;

  /**
   * Specifies the test source directories to analyze.
   * The plugin adds <code>${project.basedir}/src/test/groovy</code> to this list if it exists and is not already included.
   * This parameter is used only if {@link #includeTests} is set to {@code true}.
   */
  @Parameter(defaultValue = "${project.testCompileSourceRoots}")
  private List<String> testSourceDirectories;

  /**
   * Specifies the file patterns to include in the analysis.
   */
  @Parameter(property = "codenarc.includes", defaultValue = "**/*.groovy", required = true)
  private List<String> includes;

  /**
   * Specifies the file patterns to exclude from the analysis.
   */
  @Parameter(property = "codenarc.excludes")
  private List<String> excludes;

  /**
   * Specifies the location of the CodeNarc ruleset file.
   * If the default file does not exist, the plugin falls back to the built-in {@code rulesets/basic.xml} ruleset.
   */
  @Parameter(property = "codenarc.ruleset", defaultValue = "${project.basedir}/config/codenarc/codenarc.xml",
      required = true)
  private String ruleset;

  /**
   * Specifies the location of the CodeNarc report file.
   * The report format is determined by the file extension:
   * <ul>
   * <li>{@code .html} or {@code .htm} for HTML</li>
   * <li>{@code .json} for JSON</li>
   * <li>{@code .txt} for plain text</li>
   * <li>any other extension for XML</li>
   * </ul>
   */
  @Parameter(property = "codenarc.output.file", defaultValue = "${project.build.directory}/CodeNarcXmlReport.xml")
  private File outputFile;

  /**
   * Whether to include test source directories in the analysis.
   */
  @Parameter(property = "codenarc.includeTests", defaultValue = "false")
  private boolean includeTests;

  /**
   * Whether to output the analysis results to the console.
   */
  @Parameter(property = "codenarc.consoleOutput", defaultValue = "false")
  private boolean consoleOutput;

  /**
   * Whether to fail the build if an error occurs during analysis.
   */
  @Parameter(property = "codenarc.failOnError", defaultValue = "true")
  private boolean failOnError;

  /**
   * Whether to fail the build if any violations are found.
   */
  @Parameter(property = "codenarc.failOnViolation", defaultValue = "true")
  private boolean failOnViolation;

  /**
   * Whether to skip the analysis.
   *
   * @since 1.0.3
   */
  @Parameter(property = "codenarc.skip", defaultValue = "false")
  private boolean skip;

  /**
   * Constructs a new {@code CheckMojo}.
   *
   * @param project the Maven project
   */
  @Inject
  public CheckMojo(MavenProject project) {
    this.project = project;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute() throws MojoFailureException {
    if (skip) {
      getLog().info("Skipping CodeNarc analysis.");
      return;
    }
    prepareSourceDirectories();
    var results = analyze();
    var violationCount = results.getViolations().size();
    if (0 < violationCount) {
      var message = String.format("CodeNarc analysis found %d violation%s.", violationCount,
          violationCount == 1 ? "" : "s");
      if (outputFile != null) {
        message += String.format(" See %s for details.", outputFile);
      }
      if (failOnViolation) {
        throw new MojoFailureException(message);
      }
      getLog().warn(message);
    } else {
      getLog().info("CodeNarc analysis completed successfully with no violations.");
    }
  }

  private void prepareSourceDirectories() {
    var groovySourceDir = new File(project.getBasedir(), "src/main/groovy");
    if (groovySourceDir.isDirectory() && !sourceDirectories.contains(groovySourceDir.getPath())) {
      sourceDirectories = new ArrayList<>(sourceDirectories);
      sourceDirectories.add(groovySourceDir.getPath());
    }
    if (includeTests) {
      var groovyTestSourceDir = new File(project.getBasedir(), "src/test/groovy");
      var testDirs = testSourceDirectories == null ? List.<String>of() : testSourceDirectories;
      if (groovyTestSourceDir.isDirectory() && !testDirs.contains(groovyTestSourceDir.getPath())) {
        testDirs = new ArrayList<>(testDirs);
        testDirs.add(groovyTestSourceDir.getPath());
        testSourceDirectories = testDirs;
      }
    }
  }

  private Results analyze() {
    var runner = new CodeNarcRunner();
    runner.setRuleSetFiles(makeRuleSetFiles());
    runner.setSourceAnalyzer(makeSourceAnalyzer());
    runner.setReportWriters(makeReportWriters());
    return runner.execute();
  }

  private String makeRuleSetFiles() {
    var defaultRuleSet = new File(project.getBasedir(), "config/codenarc/codenarc.xml");
    if (ruleset.equals(defaultRuleSet.getPath()) && !defaultRuleSet.exists()) {
      return "rulesets/basic.xml";
    }
    return Arrays.stream(ruleset.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(this::resolveRuleSetFile)
        .collect(Collectors.joining(","));
  }

  private String resolveRuleSetFile(String path) {
    var file = new File(path);
    if (!file.isAbsolute()) {
      file = new File(project.getBasedir(), path);
    }
    // an existing file takes precedence; otherwise pass the path to CodeNarc as is (classpath resource or URL)
    return file.isFile() ? file.toURI().toString() : path;
  }

  private SourceAnalyzer makeSourceAnalyzer() {
    var sourceAnalyzers = new ArrayList<SourceAnalyzer>();
    addSourceAnalyzers(sourceAnalyzers, sourceDirectories);
    if (includeTests && testSourceDirectories != null) {
      addSourceAnalyzers(sourceAnalyzers, testSourceDirectories);
    }
    return sourceAnalyzers.size() == 1 ? sourceAnalyzers.get(0) : new AggregateSourceAnalyzer(sourceAnalyzers);
  }

  private void addSourceAnalyzers(List<SourceAnalyzer> sourceAnalyzers, List<String> directories) {
    var includesPattern = String.join(",", includes);
    var excludesPattern = excludes == null ? null : String.join(",", excludes);
    for (var dir : directories) {
      if (!new File(dir).isDirectory()) {
        continue;
      }
      var analyzer = new FilesystemSourceAnalyzer();
      analyzer.setBaseDirectory(dir);
      analyzer.setIncludes(includesPattern);
      analyzer.setExcludes(excludesPattern);
      analyzer.setFailOnError(failOnError);
      sourceAnalyzers.add(analyzer);
    }
  }

  private List<ReportWriter> makeReportWriters() {
    var writers = new ArrayList<ReportWriter>();
    if (outputFile != null) {
      writers.add(makeOutputFileWriter(outputFile));
    }
    if (consoleOutput) {
      var consoleWriter = new TextReportWriter();
      consoleWriter.setWriteToStandardOut(true);
      writers.add(consoleWriter);
    }
    return writers;
  }

  private ReportWriter makeOutputFileWriter(File file) {
    var title = project.getArtifactId() + " " + project.getVersion();
    AbstractReportWriter writer;
    switch (extensionOf(file)) {
      case "htm":
      case "html":
        var htmlWriter = new HtmlReportWriter();
        htmlWriter.setTitle(title);
        writer = htmlWriter;
        break;
      case "json":
        var jsonWriter = new JsonReportWriter();
        jsonWriter.setTitle(title);
        writer = jsonWriter;
        break;
      case "txt":
        var textWriter = new TextReportWriter();
        textWriter.setTitle(title);
        writer = textWriter;
        break;
      default:
        var xmlWriter = new XmlReportWriter();
        xmlWriter.setTitle(title);
        writer = xmlWriter;
        break;
    }
    writer.setOutputFile(file.getPath());
    return writer;
  }

  private static String extensionOf(File file) {
    var name = file.getName();
    var dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1);
  }
}
