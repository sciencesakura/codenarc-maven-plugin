// SPDX-License-Identifier: WTFPL

package com.sciencesakura.codenarc.maven;

import java.io.File;
import java.util.ArrayList;
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
   * Specifies the source directories to analyze.
   * The plugin adds {@code ${project.basedir}/src/main/groovy} to this list if it exists and is not yet included.
   */
  @Parameter(defaultValue = "${project.compileSourceRoots}", required = true)
  private List<String> sourceDirectories;

  /**
   * Specifies the test source directories to analyze.
   * The plugin adds {@code ${project.basedir}/src/test/groovy} to this list if it exists and is not yet included.
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
    prepareSourceDirectories();
    var helper = CodeNarcHelper.make(this);
    var results = helper.analyze();
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
      if (groovyTestSourceDir.isDirectory() && !testSourceDirectories.contains(groovyTestSourceDir.getPath())) {
        testSourceDirectories = new ArrayList<>(testSourceDirectories);
        testSourceDirectories.add(groovyTestSourceDir.getPath());
      }
    }
  }
}
