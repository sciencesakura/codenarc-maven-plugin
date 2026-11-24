// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven;

import static com.sciencesakura.codenarc.maven.TestResults.dir;
import static com.sciencesakura.codenarc.maven.TestResults.file;
import static com.sciencesakura.codenarc.maven.TestResults.newResults;
import static com.sciencesakura.codenarc.maven.TestResults.violations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.codenarc.analyzer.SourceAnalyzer;
import org.codenarc.results.Results;
import org.codenarc.ruleset.RuleSet;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AggregateSourceAnalyzer}.
 */
class AggregateSourceAnalyzerTest {

  static final RuleSet DUMMY_RULE_SET = Collections::emptyList;

  @Test
  void mergeTwoFileResults() {
    // Arrange
    // .
    // └── Foo.groovy (1 violation)
    var result1 = newResults(file("Foo.groovy", violations(1)));
    var sa1 = newStubSourceAnalyzer(result1, "src/main/groovy");
    // .
    // └── Bar.groovy (2 violation)
    var result2 = newResults(file("Bar.groovy", violations(2)));
    var sa2 = newStubSourceAnalyzer(result2, "src/test/groovy");
    var sut = new AggregateSourceAnalyzer(List.of(sa1, sa2));

    // Act
    var actual = sut.analyze(DUMMY_RULE_SET);

    // Assert
    // .
    // ├── Foo.groovy (1 violation)
    // └── Bar.groovy (2 violation)
    var expected = newResults(
        file("Foo.groovy", violations(1)),
        file("Bar.groovy", violations(2)));
    assertThat(actual).usingEquals(TestResults::equals).isEqualTo(expected);
  }

  @Test
  void mergeTwoDirectoryResults() {
    // Arrange
    // .
    // └── org
    //     └── example
    //         ├── util
    //         │   └── Foo.groovy (1 violation)
    //         └── Bar.groovy (2 violations)
    var results1 = newResults(
        dir("org",
            dir("org/example",
                dir("org/example/util",
                    file("org/example/util/Foo.groovy", violations(1))),
                file("org/example/Bar.groovy", violations(2)))));
    var sa1 = newStubSourceAnalyzer(results1, "src/main/groovy");
    // .
    // └── org
    //     └── example
    //         ├── test
    //         │   └── Baz.groovy (3 violations)
    //         ├── util
    //         │   └── FooSpec.groovy (4 violation)
    //         └── BarSpec.groovy (5 violations)
    var results2 = newResults(
        dir("org",
            dir("org/example",
                dir("org/example/test",
                    file("org/example/test/Baz.groovy", violations(3))),
                dir("org/example/util",
                    file("org/example/util/FooSpec.groovy", violations(4))),
                file("org/example/BarSpec.groovy", violations(5)))));
    var sa2 = newStubSourceAnalyzer(results2, "src/test/groovy");
    var sut = new AggregateSourceAnalyzer(List.of(sa1, sa2));

    // Act
    var actual = sut.analyze(DUMMY_RULE_SET);

    // Assert
    // .
    // └── org
    //     └── example
    //         ├── util
    //         │   ├── Foo.groovy (1 violation)
    //         │   └── FooSpec.groovy (4 violation)
    //         ├── Bar.groovy (2 violations)
    //         ├── test
    //         │   └── Baz.groovy (3 violations)
    //         └── BarSpec.groovy (5 violations)
    var expected = newResults(
        dir("org",
            dir("org/example",
                dir("org/example/util",
                    file("org/example/util/Foo.groovy", violations(1)),
                    file("org/example/util/FooSpec.groovy", violations(4))),
                file("org/example/Bar.groovy", violations(2)),
                dir("org/example/test",
                    file("org/example/test/Baz.groovy", violations(3))),
                file("org/example/BarSpec.groovy", violations(5)))));
    assertThat(actual).usingEquals(TestResults::equals).isEqualTo(expected);
  }

  @Test
  void mergeTwoEmptyDirectoryResults() {
    // Arrange
    var sa1 = newStubSourceAnalyzer(newResults(), "src/main/groovy");
    var sa2 = newStubSourceAnalyzer(newResults(), "src/test/groovy");
    var sut = new AggregateSourceAnalyzer(List.of(sa1, sa2));

    // Act
    var actual = sut.analyze(DUMMY_RULE_SET);

    // Assert
    var expected = newResults();
    assertThat(actual).usingEquals(TestResults::equals).isEqualTo(expected);
  }

  @Test
  void whenConflictingFilePathsThenKeepFirst() {
    // Arrange
    // .
    // └── Foo.groovy (1 violation)
    var result1 = newResults(file("Foo.groovy", violations(1)));
    var sa1 = newStubSourceAnalyzer(result1, "src/main/groovy");
    // .
    // └── Foo.groovy (2 violation)
    var result2 = newResults(file("Foo.groovy", violations(2)));
    var sa2 = newStubSourceAnalyzer(result2, "src/test/groovy");
    var sut = new AggregateSourceAnalyzer(List.of(sa1, sa2));

    // Act
    var actual = sut.analyze(DUMMY_RULE_SET);

    // Assert
    // .
    // └── Foo.groovy (1 violation)
    var expected = newResults(file("Foo.groovy", violations(1)));
    assertThat(actual).usingEquals(TestResults::equals).isEqualTo(expected);
  }

  @Test
  void whenConflictingFilePathAndDirectoryPathThenKeepBoth() {
    // Arrange
    // .
    // └── foo (1 violation)
    var result1 = newResults(file("foo", violations(1)));
    var sa1 = newStubSourceAnalyzer(result1, "src/main/groovy");
    // .
    // └── foo
    //     └── bar (2 violation)
    var result2 = newResults(dir("foo", file("foo/bar", violations(2))));
    var sa2 = newStubSourceAnalyzer(result2, "src/test/groovy");
    var sut = new AggregateSourceAnalyzer(List.of(sa1, sa2));

    // Act
    var actual = sut.analyze(DUMMY_RULE_SET);

    // Assert
    // .
    // ├── foo (1 violation)
    // └── foo
    //     └── bar (2 violation)
    var expected = newResults(
        file("foo", violations(1)),
        dir("foo", file("foo/bar", violations(2))));
    assertThat(actual).usingEquals(TestResults::equals).isEqualTo(expected);
  }

  static SourceAnalyzer newStubSourceAnalyzer(Results results, String... sourceDirs) {
    var stub = mock(SourceAnalyzer.class);
    when(stub.getSourceDirectories()).thenReturn(List.of(sourceDirs));
    when(stub.analyze(any(RuleSet.class))).thenReturn(results);
    return stub;
  }
}
