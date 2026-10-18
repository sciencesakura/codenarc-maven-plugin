// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.codenarc.results.DirectoryResults;
import org.codenarc.results.FileResults;
import org.codenarc.results.Results;
import org.codenarc.rule.Violation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ResultsMerger}.
 *
 * @author sciencesakura
 */
class ResultsMergerTest {

  static final Violation DUMMY = new Violation();

  static DirectoryResults dir(String path, Results... children) {
    var dir = path == null ? new DirectoryResults() : new DirectoryResults(path);
    for (var child : children) {
      dir.addChild(child);
    }
    return dir;
  }

  static FileResults file(String path, int violationCount) {
    return new FileResults(path, violations(violationCount));
  }

  static List<Violation> violations(int count) {
    return Collections.nCopies(count, DUMMY);
  }

  static Results childByPath(Results parent, String path) {
    return parent.getChildren().stream()
        .filter(c -> c.getPath().equals(path))
        .findFirst()
        .orElseThrow();
  }

  static Results assertRoot(Results actual) {
    assertThat(actual).isInstanceOf(DirectoryResults.class)
        .extracting(Results::getPath)
        .isNull();
    assertThat(actual.getChildren()).hasSize(1)
        .first()
        .isInstanceOf(DirectoryResults.class)
        .extracting(Results::getPath)
        .isEqualTo("");
    return actual.getChildren().get(0);
  }

  @Test
  void mergeEmptyDirectories() {
    // Arrange
    var tree1 = dir(null, dir(""));
    var tree2 = dir(null, dir(""));

    // Act
    var actual = ResultsMerger.merge(List.of(tree1, tree2));

    // Assert
    assertRoot(actual);
  }

  @Test
  void mergeTwoFiles() {
    // Arrange
    var file1 = file("Foo.groovy", 1);
    var file2 = file("Bar.groovy", 2);

    // Act
    var actual = ResultsMerger.merge(List.of(file1, file2));

    // Assert
    var root = assertRoot(actual);
    assertThat(root.getChildren()).hasSize(2)
        .extracting(Results::getPath)
        .containsExactlyInAnyOrder("Foo.groovy", "Bar.groovy");
    assertThat(childByPath(root, "Foo.groovy"))
        .isSameAs(file1);
    assertThat(childByPath(root, "Bar.groovy"))
        .isSameAs(file2);
  }

  @Nested
  class MergeTwoDirectories {

    @Test
    void merge() {
      // Act
      var actual = ResultsMerger.merge(List.of(tree1(), tree2()));

      // Assert
      assertMergedTree(actual);
    }

    @Test
    void mergeInReverseOrder() {
      // Act
      var actual = ResultsMerger.merge(List.of(tree2(), tree1()));

      // Assert
      assertMergedTree(actual);
    }

    /**
     * Returns a directory tree with the following structure:
     * <pre><code>
     * .
     * └─ org
     *    └─ example
     *       ├─ util
     *       │  └─ Foo.groovy (1 violation)
     *       └─ Bar.groovy (2 violations)
     * </code></pre>
     */
    DirectoryResults tree1() {
      return dir(null,
          dir("",
              dir("org",
                  dir("org/example",
                      dir("org/example/util",
                          file("org/example/util/Foo.groovy", 1)),
                      file("org/example/Bar.groovy", 2)))));
    }

    /**
     * Returns a directory tree with the following structure:
     * <pre><code>
     * .
     * └─ org
     *    └─ example
     *       └─ BarSpec.groovy (3 violations)
     * </code></pre>
     */
    DirectoryResults tree2() {
      return dir(null,
          dir("",
              dir("org",
                  dir("org/example",
                      file("org/example/BarSpec.groovy", 3)))));
    }

    /**
     * Asserts that the given Results tree has the expected merged structure and violations.
     * The expected structure is:
     * <pre><code>
     * .
     * └─ org
     *    └─ example
     *       ├─ util
     *       │  └─ Foo.groovy (1 violation)
     *       ├─ Bar.groovy (2 violations)
     *       └─ BarSpec.groovy (3 violations)
     * </code></pre>
     */
    void assertMergedTree(Results actual) {
      var root = assertRoot(actual);
      assertThat(root.getChildren()).hasSize(1)
          .first()
          .isInstanceOf(DirectoryResults.class)
          .extracting(Results::getPath)
          .isEqualTo("org");

      var org = root.getChildren().get(0);
      assertThat(org.getChildren()).hasSize(1)
          .first()
          .isInstanceOf(DirectoryResults.class)
          .extracting(Results::getPath)
          .isEqualTo("org/example");

      var example = org.getChildren().get(0);
      assertThat(example.getChildren()).hasSize(3)
          .extracting(Results::getPath)
          .containsExactlyInAnyOrder("org/example/util", "org/example/Bar.groovy", "org/example/BarSpec.groovy");

      var util = childByPath(example, "org/example/util");
      assertThat(util).isInstanceOf(DirectoryResults.class);
      assertThat(util.getChildren()).hasSize(1)
          .first()
          .isInstanceOf(FileResults.class)
          .extracting(Results::getPath, Results::getViolations)
          .containsExactly("org/example/util/Foo.groovy", violations(1));
      assertThat(childByPath(example, "org/example/Bar.groovy"))
          .isInstanceOf(FileResults.class)
          .extracting(Results::getViolations)
          .isEqualTo(violations(2));
      assertThat(childByPath(example, "org/example/BarSpec.groovy"))
          .isInstanceOf(FileResults.class)
          .extracting(Results::getViolations)
          .isEqualTo(violations(3));
    }
  }

  @Nested
  class WhenConflictingFilePathsThenKeepFirst {

    @Test
    void merge() {
      // Act
      var actual = ResultsMerger.merge(List.of(tree1(), tree2()));

      // Assert
      var root = assertRoot(actual);
      assertThat(root.getChildren()).hasSize(1)
          .first()
          .isInstanceOf(FileResults.class)
          .extracting(Results::getPath, Results::getViolations)
          .containsExactly("Foo.groovy", violations(1));
    }

    @Test
    void mergeInReverseOrder() {
      // Act
      var actual = ResultsMerger.merge(List.of(tree2(), tree1()));

      // Assert
      var root = assertRoot(actual);
      assertThat(root.getChildren()).hasSize(1)
          .first()
          .isInstanceOf(FileResults.class)
          .extracting(Results::getPath, Results::getViolations)
          .containsExactly("Foo.groovy", violations(2));
    }

    /**
     * Returns a directory tree with the following structure:
     * <pre><code>
     * .
     * └─ Foo.groovy (1 violation)
     * </code></pre>
     */
    DirectoryResults tree1() {
      return dir(null, dir("", file("Foo.groovy", 1)));
    }

    /**
     * Returns a directory tree with the following structure:
     * <pre><code>
     * .
     * └─ Foo.groovy (2 violations)
     * </code></pre>
     */
    DirectoryResults tree2() {
      return dir(null, dir("", file("Foo.groovy", 2)));
    }
  }

  @Nested
  class WhenConflictingFilePathAndDirectoryPathThenKeepBoth {

    @Test
    void merge() {
      // Act
      var actual = ResultsMerger.merge(List.of(tree1(), tree2()));

      // Assert
      assertMergedTree(actual);
    }

    @Test
    void mergeInReverseOrder() {
      // Act
      var actual = ResultsMerger.merge(List.of(tree2(), tree1()));

      // Assert
      assertMergedTree(actual);
    }

    /**
     * Returns a directory tree with the following structure:
     * <pre><code>
     * .
     * └─ foo (1 violation)
     * </code></pre>
     */
    DirectoryResults tree1() {
      return dir(null, dir("", file("foo", 1)));
    }

    /**
     * Returns a directory tree with the following structure:
     * <pre><code>
     * .
     * └─ foo
     *    └─ bar (2 violations)
     * </code></pre>
     */
    DirectoryResults tree2() {
      return dir(null, dir("", dir("foo", file("foo/bar", 2))));
    }

    /**
     * Asserts that the given Results tree has the expected merged structure and violations.
     * The expected structure is:
     * <pre><code>
     * .
     * ├─ foo (1 violation)
     * └─ foo
     *    └─ bar (2 violations)
     * </code></pre>
     */
    void assertMergedTree(Results actual) {
      var root = assertRoot(actual);
      assertThat(root.getChildren()).hasSize(2)
          .extracting(Results::getPath)
          .containsExactlyInAnyOrder("foo", "foo");
      assertThat(root.getChildren().stream().filter(FileResults.class::isInstance).findFirst().orElseThrow())
          .extracting(Results::getViolations)
          .isEqualTo(violations(1));

      var foo = root.getChildren().stream().filter(DirectoryResults.class::isInstance).findFirst().orElseThrow();
      assertThat(foo.getChildren()).hasSize(1)
          .first()
          .isInstanceOf(FileResults.class)
          .extracting(Results::getPath, Results::getViolations)
          .containsExactly("foo/bar", violations(2));
    }
  }
}
