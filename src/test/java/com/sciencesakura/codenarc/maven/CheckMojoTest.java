// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven;

import static com.sciencesakura.codenarc.maven.TestResults.file;
import static com.sciencesakura.codenarc.maven.TestResults.newResults;
import static com.sciencesakura.codenarc.maven.TestResults.violations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import javax.inject.Inject;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.codenarc.analyzer.FilesystemSourceAnalyzer;
import org.codenarc.analyzer.SourceAnalyzer;
import org.codenarc.report.HtmlReportWriter;
import org.codenarc.report.JsonReportWriter;
import org.codenarc.report.ReportWriter;
import org.codenarc.report.TextReportWriter;
import org.codenarc.report.XmlReportWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CheckMojo}.
 */
@ExtendWith(MockitoExtension.class)
@MojoTest
@Basedir("/projects/standard")
class CheckMojoTest {

  @Mock
  CodeNarcExecutor executor;

  @Captor
  ArgumentCaptor<String> acRuleSetFiles;

  @Captor
  ArgumentCaptor<SourceAnalyzer> acSourceAnalyzer;

  @Captor
  ArgumentCaptor<List<ReportWriter>> acReportWriters;

  @Inject
  MavenProject project;

  @Inject
  Log log;

  @Provides
  CodeNarcExecutor executor() {
    return executor;
  }

  @Nested
  class RuleSetFiles {

    @BeforeEach
    void setUp() {
      when(executor.execute(anyString(), any(SourceAnalyzer.class), anyList())).thenReturn(newResults());
    }

    @Test
    @InjectMojo(goal = "check")
    void defaultFileIfExists(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).ruleSetFiles();

      assertThat(actual).isEqualTo(uriOf("config/codenarc/codenarc.xml"));
    }

    @Test
    @Basedir("/projects/minimal")
    @InjectMojo(goal = "check")
    void builtinFileIfDefaultFileDoesNotExist(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).ruleSetFiles();

      assertThat(actual).isEqualTo("rulesets/basic.xml");
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "ruleset", value = "config/codenarc/extra.xml")
    void relativePathIsResolvedAgainstBasedir(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).ruleSetFiles();

      assertThat(actual).isEqualTo(uriOf("config/codenarc/extra.xml"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "ruleset", value = "${project.basedir}/config/codenarc/extra.xml")
    void absolutePath(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).ruleSetFiles();

      assertThat(actual).isEqualTo(uriOf("config/codenarc/extra.xml"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "ruleset", value = "rulesets/imports.xml")
    void nonExistentPathIsPassedAsIs(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).ruleSetFiles();

      assertThat(actual).isEqualTo("rulesets/imports.xml");
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "ruleset", value = " config/codenarc/extra.xml , ,rulesets/imports.xml ")
    void commaSeparatedPaths(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).ruleSetFiles();

      assertThat(actual).isEqualTo(uriOf("config/codenarc/extra.xml") + ",rulesets/imports.xml");
    }
  }

  @Nested
  class SourceAnalyzers {

    @BeforeEach
    void setUp() {
      when(executor.execute(anyString(), any(SourceAnalyzer.class), anyList())).thenReturn(newResults());
    }

    @Test
    @InjectMojo(goal = "check")
    void groovySourceDirectoryIsAdded(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).sourceAnalyzer();

      assertThat(actual)
          .asInstanceOf(InstanceOfAssertFactories.type(AggregateSourceAnalyzer.class))
          .extracting(AggregateSourceAnalyzer::getDelegates)
          .asInstanceOf(InstanceOfAssertFactories.list(FilesystemSourceAnalyzer.class))
          .extracting(FilesystemSourceAnalyzer::getBaseDirectory)
          .containsExactlyInAnyOrder(pathOf("src/main/java"), pathOf("src/main/groovy"));
    }

    @Test
    @Basedir("/projects/minimal")
    @InjectMojo(goal = "check")
    void singleDirectoryIsNotAggregated(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).sourceAnalyzer();

      assertThat(actual)
          .asInstanceOf(InstanceOfAssertFactories.type(FilesystemSourceAnalyzer.class))
          .extracting(FilesystemSourceAnalyzer::getBaseDirectory)
          .isEqualTo(pathOf("src/main/java"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "sourceDirectories", value = "${project.basedir}/src/main/groovy")
    void groovySourceDirectoryIsNotDuplicated(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).sourceAnalyzer();

      assertThat(actual)
          .asInstanceOf(InstanceOfAssertFactories.type(FilesystemSourceAnalyzer.class))
          .extracting(FilesystemSourceAnalyzer::getBaseDirectory)
          .isEqualTo(pathOf("src/main/groovy"));
    }

    @Test
    @Basedir("/projects/minimal")
    @InjectMojo(goal = "check")
    @MojoParameter(name = "sourceDirectories", value = "${project.basedir}/no-such-dir,${project.basedir}/src/main/java")
    void nonExistentDirectoryIsSkipped(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).sourceAnalyzer();

      assertThat(actual)
          .asInstanceOf(InstanceOfAssertFactories.type(FilesystemSourceAnalyzer.class))
          .extracting(FilesystemSourceAnalyzer::getBaseDirectory)
          .isEqualTo(pathOf("src/main/java"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "includeTests", value = "true")
    void testSourceDirectoriesAreAddedIfIncludeTests(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).sourceAnalyzer();

      assertThat(actual)
          .asInstanceOf(InstanceOfAssertFactories.type(AggregateSourceAnalyzer.class))
          .extracting(AggregateSourceAnalyzer::getDelegates)
          .asInstanceOf(InstanceOfAssertFactories.list(FilesystemSourceAnalyzer.class))
          .extracting(FilesystemSourceAnalyzer::getBaseDirectory)
          .containsExactlyInAnyOrder(pathOf("src/main/java"), pathOf("src/main/groovy"),
              pathOf("src/test/java"), pathOf("src/test/groovy"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "includeTests", value = "true")
    @MojoParameter(name = "testSourceDirectories", value = "${project.basedir}/src/test/groovy")
    void groovyTestSourceDirectoryIsNotDuplicated(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).sourceAnalyzer();

      assertThat(actual)
          .asInstanceOf(InstanceOfAssertFactories.type(AggregateSourceAnalyzer.class))
          .extracting(AggregateSourceAnalyzer::getDelegates)
          .asInstanceOf(InstanceOfAssertFactories.list(FilesystemSourceAnalyzer.class))
          .extracting(FilesystemSourceAnalyzer::getBaseDirectory)
          .containsExactlyInAnyOrder(pathOf("src/main/java"), pathOf("src/main/groovy"),
              pathOf("src/test/groovy"));
    }

    @Test
    @InjectMojo(goal = "check")
    void defaultFilterSettings(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).sourceAnalyzer();

      assertThat(actual)
          .asInstanceOf(InstanceOfAssertFactories.type(AggregateSourceAnalyzer.class))
          .extracting(AggregateSourceAnalyzer::getDelegates)
          .asInstanceOf(InstanceOfAssertFactories.list(FilesystemSourceAnalyzer.class))
          .isNotEmpty()
          .allSatisfy(a -> {
            assertThat(a.getIncludes()).isEqualTo("**/*.groovy");
            assertThat(a.getExcludes()).isEmpty();
            assertThat(a.isFailOnError()).isTrue();
          });
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "includeTests", value = "true")
    @MojoParameter(name = "includes", value = "**/*.groovy,**/*.gvy")
    @MojoParameter(name = "excludes", value = "**/generated/**,**/*Spec.groovy")
    @MojoParameter(name = "failOnError", value = "false")
    void customFilterSettingsAreAppliedToAllDirectories(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).sourceAnalyzer();

      assertThat(actual)
          .asInstanceOf(InstanceOfAssertFactories.type(AggregateSourceAnalyzer.class))
          .extracting(AggregateSourceAnalyzer::getDelegates)
          .asInstanceOf(InstanceOfAssertFactories.list(FilesystemSourceAnalyzer.class))
          .isNotEmpty()
          .allSatisfy(a -> {
            assertThat(a.getIncludes()).isEqualTo("**/*.groovy,**/*.gvy");
            assertThat(a.getExcludes()).isEqualTo("**/generated/**,**/*Spec.groovy");
            assertThat(a.isFailOnError()).isFalse();
          });
    }
  }

  @Nested
  class ReportWriters {

    @BeforeEach
    void setUp() {
      project.setArtifactId("unit-test");
      project.setVersion("1.2.3");
      when(executor.execute(anyString(), any(SourceAnalyzer.class), anyList())).thenReturn(newResults());
    }

    @Test
    @InjectMojo(goal = "check")
    void xmlReportByDefault(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).reportWriters();

      assertThat(actual)
          .singleElement()
          .asInstanceOf(InstanceOfAssertFactories.type(XmlReportWriter.class))
          .extracting(XmlReportWriter::getTitle, XmlReportWriter::getOutputFile)
          .containsExactly("unit-test 1.2.3", pathOf("target/CodeNarcXmlReport.xml"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "outputFile", value = "${project.build.directory}/report.html")
    void htmlReport(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).reportWriters();

      assertThat(actual)
          .singleElement()
          .asInstanceOf(InstanceOfAssertFactories.type(HtmlReportWriter.class))
          .extracting(HtmlReportWriter::getTitle, HtmlReportWriter::getOutputFile)
          .containsExactly("unit-test 1.2.3", pathOf("target/report.html"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "outputFile", value = "${project.build.directory}/report.htm")
    void htmReport(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).reportWriters();

      assertThat(actual)
          .singleElement()
          .asInstanceOf(InstanceOfAssertFactories.type(HtmlReportWriter.class))
          .extracting(HtmlReportWriter::getTitle, HtmlReportWriter::getOutputFile)
          .containsExactly("unit-test 1.2.3", pathOf("target/report.htm"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "outputFile", value = "${project.build.directory}/report.json")
    void jsonReport(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).reportWriters();

      assertThat(actual)
          .singleElement()
          .asInstanceOf(InstanceOfAssertFactories.type(JsonReportWriter.class))
          .extracting(JsonReportWriter::getTitle, JsonReportWriter::getOutputFile)
          .containsExactly("unit-test 1.2.3", pathOf("target/report.json"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "outputFile", value = "${project.build.directory}/report.txt")
    void textReport(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).reportWriters();

      assertThat(actual)
          .singleElement()
          .asInstanceOf(InstanceOfAssertFactories.type(TextReportWriter.class))
          .extracting(TextReportWriter::getTitle, TextReportWriter::getOutputFile)
          .containsExactly("unit-test 1.2.3", pathOf("target/report.txt"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "outputFile", value = "${project.build.directory}/report")
    void xmlReportIfNoExtension(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).reportWriters();

      assertThat(actual)
          .singleElement()
          .asInstanceOf(InstanceOfAssertFactories.type(XmlReportWriter.class))
          .extracting(XmlReportWriter::getTitle, XmlReportWriter::getOutputFile)
          .containsExactly("unit-test 1.2.3", pathOf("target/report"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "consoleOutput", value = "true")
    void consoleReportIsAddedIfConsoleOutput(CheckMojo mojo) throws Exception {
      var actual = execute(mojo).reportWriters();

      assertThat(actual).hasSize(2);
      assertThat(actual).element(0).isInstanceOf(XmlReportWriter.class);
      assertThat(actual)
          .element(1, InstanceOfAssertFactories.type(TextReportWriter.class))
          .extracting(TextReportWriter::isWriteToStandardOut)
          .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
          .isTrue();
    }
  }

  @Nested
  class ResultsHandling {

    @Test
    @InjectMojo(goal = "check")
    void succeedIfNoViolations(CheckMojo mojo) throws Exception {
      when(executor.execute(anyString(), any(SourceAnalyzer.class), anyList())).thenReturn(newResults());
      mojo.execute();

      verify(log).info("CodeNarc analysis completed successfully with no violations.");
      verify(log, never()).warn(anyString());
    }

    @Test
    @InjectMojo(goal = "check")
    void failIfOneViolation(CheckMojo mojo) {
      var results = newResults(file("Foo.groovy", violations(1)));
      when(executor.execute(anyString(), any(), any())).thenReturn(results);

      assertThatThrownBy(mojo::execute)
          .isInstanceOf(MojoFailureException.class)
          .hasMessage("CodeNarc analysis found 1 violation. See %s for details.",
              pathOf("target/CodeNarcXmlReport.xml"));
    }

    @Test
    @InjectMojo(goal = "check")
    void failIfMultipleViolations(CheckMojo mojo) {
      var results = newResults(
          file("Foo.groovy", violations(1)),
          file("Bar.groovy", violations(2)));
      when(executor.execute(anyString(), any(), any())).thenReturn(results);

      assertThatThrownBy(mojo::execute)
          .isInstanceOf(MojoFailureException.class)
          .hasMessage("CodeNarc analysis found 3 violations. See %s for details.",
              pathOf("target/CodeNarcXmlReport.xml"));
    }

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "failOnViolation", value = "false")
    void warnIfViolationsAndNotFailOnViolation(CheckMojo mojo) throws Exception {
      var results = newResults(file("Foo.groovy", violations(2)));
      when(executor.execute(anyString(), any(), any())).thenReturn(results);
      mojo.execute();

      verify(log).warn(String.format("CodeNarc analysis found 2 violations. See %s for details.",
          pathOf("target/CodeNarcXmlReport.xml")));
    }
  }

  @Nested
  class Skip {

    @Test
    @InjectMojo(goal = "check")
    @MojoParameter(name = "skip", value = "true")
    void doNothingIfSkip(CheckMojo mojo) throws Exception {
      mojo.execute();

      verify(executor, never()).execute(anyString(), any(SourceAnalyzer.class), anyList());
      verify(log).info("Skipping CodeNarc analysis.");
    }
  }

  private Invocation execute(CheckMojo mojo) throws Exception {
    mojo.execute();
    verify(executor).execute(acRuleSetFiles.capture(), acSourceAnalyzer.capture(), acReportWriters.capture());
    return new Invocation(acRuleSetFiles.getValue(), acSourceAnalyzer.getValue(), acReportWriters.getValue());
  }

  private String pathOf(String relativePath) {
    return new File(project.getBasedir(), relativePath).getPath();
  }

  private String uriOf(String relativePath) {
    return new File(project.getBasedir(), relativePath).toURI().toString();
  }

  private record Invocation(String ruleSetFiles, SourceAnalyzer sourceAnalyzer, List<ReportWriter> reportWriters) {
  }
}
