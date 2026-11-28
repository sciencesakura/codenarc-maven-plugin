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
    // └── Cherry.groovy (1 violation)
    var result1 = newResults(file("Cherry.groovy", violations(1)));
    var sa1 = newStubSourceAnalyzer(result1, "src/main/groovy");
    // .
    // └── Apple.groovy (2 violations)
    var result2 = newResults(file("Apple.groovy", violations(2)));
    var sa2 = newStubSourceAnalyzer(result2, "src/test/groovy");
    var sut = new AggregateSourceAnalyzer(List.of(sa1, sa2));

    // Act
    var actual = sut.analyze(DUMMY_RULE_SET);

    // Assert
    // .
    // ├── Apple.groovy (2 violations)
    // └── Cherry.groovy (1 violation)
    var expected = newResults(
        file("Apple.groovy", violations(2)),
        file("Cherry.groovy", violations(1)));
    assertThat(actual).usingEquals(TestResults::equals).isEqualTo(expected);
  }

  @Test
  void mergeTwoDirectoryResults() {
    // Arrange
    // .
    // └── org
    //     └── example
    //         ├── Apple.groovy (1 violation)
    //         └── util
    //             └── Cherry.groovy (2 violations)
    var results1 = newResults(
        dir("org",
            dir("org/example",
                file("org/example/Apple.groovy", violations(1)),
                dir("org/example/util",
                    file("org/example/util/Cherry.groovy", violations(2))))));
    var sa1 = newStubSourceAnalyzer(results1, "src/main/groovy");
    // .
    // └── org
    //     └── example
    //         ├── AppleSpec.groovy (3 violations)
    //         ├── test
    //         │   └── Banana.groovy (4 violations)
    //         └── util
    //             └── CherrySpec.groovy (5 violations)
    var results2 = newResults(
        dir("org",
            dir("org/example",
                file("org/example/AppleSpec.groovy", violations(3)),
                dir("org/example/test",
                    file("org/example/test/Banana.groovy", violations(4))),
                dir("org/example/util",
                    file("org/example/util/CherrySpec.groovy", violations(5))))));
    var sa2 = newStubSourceAnalyzer(results2, "src/test/groovy");
    var sut = new AggregateSourceAnalyzer(List.of(sa1, sa2));

    // Act
    var actual = sut.analyze(DUMMY_RULE_SET);

    // Assert
    // .
    // └── org
    //     └── example
    //         ├── Apple.groovy (1 violation)
    //         ├── AppleSpec.groovy (3 violations)
    //         ├── test
    //         │   └── Banana.groovy (4 violations)
    //         └── util
    //             ├── Cherry.groovy (2 violations)
    //             └── CherrySpec.groovy (5 violations)
    var expected = newResults(
        dir("org",
            dir("org/example",
                file("org/example/Apple.groovy", violations(1)),
                file("org/example/AppleSpec.groovy", violations(3)),
                dir("org/example/test",
                    file("org/example/test/Banana.groovy", violations(4))),
                dir("org/example/util",
                    file("org/example/util/Cherry.groovy", violations(2)),
                    file("org/example/util/CherrySpec.groovy", violations(5))))));
    assertThat(actual).usingEquals(TestResults::equals).isEqualTo(expected);
  }

  @Test
  void childrenAreSortedByName() {
    // Arrange
    // CodeNarc lists files in the order the file system returns them, which is not necessarily sorted.
    // .
    // ├── b
    // │   ├── Cherry.groovy (1 violation)
    // │   └── Apple.groovy (2 violations)
    // └── a
    //     └── Banana.groovy (3 violations)
    var results = newResults(
        dir("b",
            file("b/Cherry.groovy", violations(1)),
            file("b/Apple.groovy", violations(2))),
        dir("a",
            file("a/Banana.groovy", violations(3))));
    var sa1 = newStubSourceAnalyzer(results, "src/main/groovy");
    var sa2 = newStubSourceAnalyzer(newResults(), "src/test/groovy");
    var sut = new AggregateSourceAnalyzer(List.of(sa1, sa2));

    // Act
    var actual = sut.analyze(DUMMY_RULE_SET);

    // Assert
    // .
    // ├── a
    // │   └── Banana.groovy (3 violations)
    // └── b
    //     ├── Apple.groovy (2 violations)
    //     └── Cherry.groovy (1 violation)
    var expected = newResults(
        dir("a",
            file("a/Banana.groovy", violations(3))),
        dir("b",
            file("b/Apple.groovy", violations(2)),
            file("b/Cherry.groovy", violations(1))));
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
    // └── Apple.groovy (1 violation)
    var result1 = newResults(file("Apple.groovy", violations(1)));
    var sa1 = newStubSourceAnalyzer(result1, "src/main/groovy");
    // .
    // └── Apple.groovy (2 violations)
    var result2 = newResults(file("Apple.groovy", violations(2)));
    var sa2 = newStubSourceAnalyzer(result2, "src/test/groovy");
    var sut = new AggregateSourceAnalyzer(List.of(sa1, sa2));

    // Act
    var actual = sut.analyze(DUMMY_RULE_SET);

    // Assert
    // .
    // └── Apple.groovy (1 violation)
    var expected = newResults(file("Apple.groovy", violations(1)));
    assertThat(actual).usingEquals(TestResults::equals).isEqualTo(expected);
  }

  @Test
  void whenConflictingFilePathAndDirectoryPathThenDirectoryComesFirst() {
    // Arrange
    // .
    // └── apple (1 violation)
    var result1 = newResults(file("apple", violations(1)));
    var sa1 = newStubSourceAnalyzer(result1, "src/main/groovy");
    // .
    // └── apple
    //     └── banana (2 violations)
    var result2 = newResults(dir("apple", file("apple/banana", violations(2))));
    var sa2 = newStubSourceAnalyzer(result2, "src/test/groovy");
    var sut = new AggregateSourceAnalyzer(List.of(sa1, sa2));

    // Act
    var actual = sut.analyze(DUMMY_RULE_SET);

    // Assert
    // .
    // ├── apple
    // │   └── banana (2 violations)
    // └── apple (1 violation)
    var expected = newResults(
        dir("apple", file("apple/banana", violations(2))),
        file("apple", violations(1)));
    assertThat(actual).usingEquals(TestResults::equals).isEqualTo(expected);
  }

  @Test
  void whenConflictingDirectoryPathAndFilePathThenDirectoryComesFirst() {
    // Arrange
    // .
    // └── apple
    //     └── banana (2 violations)
    var result1 = newResults(dir("apple", file("apple/banana", violations(2))));
    var sa1 = newStubSourceAnalyzer(result1, "src/main/groovy");
    // .
    // └── apple (1 violation)
    var result2 = newResults(file("apple", violations(1)));
    var sa2 = newStubSourceAnalyzer(result2, "src/test/groovy");
    var sut = new AggregateSourceAnalyzer(List.of(sa1, sa2));

    // Act
    var actual = sut.analyze(DUMMY_RULE_SET);

    // Assert
    // the same as whenConflictingFilePathAndDirectoryPathThenDirectoryComesFirst regardless of the input order
    // .
    // ├── apple
    // │   └── banana (2 violations)
    // └── apple (1 violation)
    var expected = newResults(
        dir("apple", file("apple/banana", violations(2))),
        file("apple", violations(1)));
    assertThat(actual).usingEquals(TestResults::equals).isEqualTo(expected);
  }

  static SourceAnalyzer newStubSourceAnalyzer(Results results, String... sourceDirs) {
    var stub = mock(SourceAnalyzer.class);
    when(stub.getSourceDirectories()).thenReturn(List.of(sourceDirs));
    when(stub.analyze(any(RuleSet.class))).thenReturn(results);
    return stub;
  }
}
