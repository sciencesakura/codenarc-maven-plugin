// SPDX-License-Identifier: WTFPL

package com.sciencesakura.codenarc.maven;

import java.io.File;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Runs CodeNarc analysis on the project's source code.
 * Call {@code mvn codenarc:check} to execute the analysis.
 *
 * @author sciencesakura
 */
@Mojo(name = "check", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.VERIFY)
@Getter
public class CheckMojo extends AbstractMojo {

  private final MavenProject project;

  /**
   * Specifies the location of the source directories to analyze.
   */
  @Parameter(defaultValue = "${project.compileSourceRoots}", required = true)
  private List<String> sourceDirectories;

  /**
   * Specifies the location of the test source directories to analyze.
   * This is only used if {@link #includeTests} is set to {@code true}.
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
   * If the default file does not exist, it will fall back to using the built-in {@code rulesets/basic.xml}.
   */
  @Parameter(property = "codenarc.ruleset", defaultValue = "${project.basedir}/config/codenarc/codenarc.xml",
      required = true)
  private String ruleset;

  /**
   * Specifies the location of the CodeNarc report file.
   * The report format is determined by the file extension: {@code .html} for HTML, {@code .json} for JSON,
   * {@code .txt} for plain text, and any other extension for XML.
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
    var helper = CodeNarcHelper.make(this);
    var results = helper.analyze();
    var violationCount = results.getViolations().size();
    if (0 < violationCount) {
      var message = String.format("CodeNarc analysis found %d violation%s.", violationCount, violationCount == 1 ? "" : "s");
      if (outputFile != null) {
        message += String.format(" See %s for details.", outputFile);
      }
      if (failOnViolation) {
        throw new MojoFailureException(message);
      }
      getLog().warn(message);
    } else {
      getLog().info("CodeNarc analysis completed with no violations found.");
    }
  }
}
