// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.codenarc.analyzer.SourceAnalyzer;
import org.codenarc.results.DirectoryResults;
import org.codenarc.results.FileResults;
import org.codenarc.results.Results;
import org.codenarc.rule.Violation;
import org.codenarc.ruleset.RuleSet;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AggregateSourceAnalyzer}.
 */
class AggregateSourceAnalyzerTest {

  static final Violation DUMMY_VIOLATION = new Violation();

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
    assertThat(actual).usingEquals(ResultsUtil::equals).isEqualTo(expected);
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
    assertThat(actual).usingEquals(ResultsUtil::equals).isEqualTo(expected);
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
    assertThat(actual).usingEquals(ResultsUtil::equals).isEqualTo(expected);
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
    assertThat(actual).usingEquals(ResultsUtil::equals).isEqualTo(expected);
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
    assertThat(actual).usingEquals(ResultsUtil::equals).isEqualTo(expected);
  }

  static SourceAnalyzer newStubSourceAnalyzer(Results results, String... sourceDirs) {
    var stub = mock(SourceAnalyzer.class);
    when(stub.getSourceDirectories()).thenReturn(List.of(sourceDirs));
    when(stub.analyze(any(RuleSet.class))).thenReturn(results);
    return stub;
  }

  static Results newResults(Results... children) {
    return dir(null, dir("", children));
  }

  static Results dir(String path, Results... children) {
    var dir = path == null ? new DirectoryResults() : new DirectoryResults(path);
    for (var child : children) {
      dir.addChild(child);
    }
    return dir;
  }

  static Results file(String path, List<Violation> violations) {
    return new FileResults(path, violations);
  }

  static List<Violation> violations(int count) {
    return Collections.nCopies(count, DUMMY_VIOLATION);
  }

  static class ResultsUtil {

    static boolean equals(Results a, Results b) {
      if (a == b) {
        return true;
      }
      if (!Objects.equals(a.getPath(), b.getPath())) {
        return false;
      }
      if (a instanceof FileResults && b instanceof FileResults) {
        return Objects.equals(a.getViolations(), b.getViolations());
      }
      if (a instanceof DirectoryResults && b instanceof DirectoryResults) {
        var childrenA = a.getChildren();
        var childrenB = b.getChildren();
        if (childrenA == null || childrenB == null) {
          return childrenA == null && childrenB == null;
        }
        if (childrenA.size() != childrenB.size()) {
          return false;
        }
        for (int i = 0; i < childrenA.size(); ++i) {
          if (!equals(childrenA.get(i), childrenB.get(i))) {
            return false;
          }
        }
        return true;
      }
      return false;
    }
  }
}
