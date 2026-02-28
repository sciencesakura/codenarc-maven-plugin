// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven

import static org.assertj.core.api.Assertions.assertThat

import java.util.function.Function
import org.codenarc.results.DirectoryResults
import org.codenarc.results.FileResults
import org.codenarc.results.Results
import org.codenarc.rule.Violation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for {@link ResultsMerger}.
 *
 * @author sciencesakura
 */
class ResultsMergerTest {

  static final Violation DUMMY = new Violation()

  @Test
  void mergeEmptyDirectories() {
    // Arrange
    def tree1 = new DirectoryResults().tap {
      addChild(new DirectoryResults(''))
    }
    def tree2 = new DirectoryResults().tap {
      addChild(new DirectoryResults(''))
    }

    // Act
    def actual = ResultsMerger.merge([tree1, tree2])

    // Assert
    assertThat(actual).isInstanceOf(DirectoryResults)
        .extracting(Results::getPath)
        .isNull()
    assertThat(actual.children).hasSize(1)
        .first()
        .isInstanceOf(DirectoryResults)
        .extracting(Results::getPath)
        .isEqualTo('')
  }

  @Test
  void mergeTwoFiles() {
    // Arrange
    def file1 = new FileResults('Foo.groovy', [DUMMY])
    def file2 = new FileResults('Bar.groovy', [DUMMY, DUMMY])

    // Act
    def actual = ResultsMerger.merge([file1, file2])

    // Assert
    assertThat(actual).isInstanceOf(DirectoryResults)
        .extracting(Results::getPath)
        .isNull()
    assertThat(actual.children).hasSize(1)
        .first()
        .isInstanceOf(DirectoryResults)
        .extracting(Results::getPath)
        .isEqualTo('')

    actual.children[0].with { root ->
      assertThat(root.children).hasSize(2)
          .extracting(Results::getPath as Function)
          .containsExactlyInAnyOrder('Foo.groovy', 'Bar.groovy')
      assertThat(root.children.find { it.path == 'Foo.groovy' })
          .isSameAs(file1)
      assertThat(root.children.find { it.path == 'Bar.groovy' })
          .isSameAs(file2)
    }
  }

  @Nested
  class MergeTwoDirectories {

    @Test
    void merge() {
      // Act
      def actual = ResultsMerger.merge([tree1(), tree2()])

      // Assert
      assertMergedTree(actual)
    }

    @Test
    void mergeInReverseOrder() {
      // Act
      def actual = ResultsMerger.merge([tree2(), tree1()])

      // Assert
      assertMergedTree(actual)
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
      new DirectoryResults().tap {
        def root = new DirectoryResults('').tap {
          def org = new DirectoryResults('org').tap {
            def example = new DirectoryResults('org/example').tap {
              def util = new DirectoryResults('org/example/util').tap {
                addChild(new FileResults('org/example/util/Foo.groovy', [DUMMY]))
              }
              addChild(util)
              addChild(new FileResults('org/example/Bar.groovy', [DUMMY, DUMMY]))
            }
            addChild(example)
          }
          addChild(org)
        }
        addChild(root)
      }
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
      new DirectoryResults().tap {
        def root = new DirectoryResults('').tap {
          def org = new DirectoryResults('org').tap {
            def example = new DirectoryResults('org/example').tap {
              addChild(new FileResults('org/example/BarSpec', [DUMMY, DUMMY, DUMMY]))
            }
            addChild(example)
          }
          addChild(org)
        }
        addChild(root)
      }
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
      assertThat(actual).isInstanceOf(DirectoryResults)
          .extracting(Results::getPath)
          .isNull()
      assertThat(actual.children).hasSize(1)
          .first()
          .isInstanceOf(DirectoryResults)
          .extracting(Results::getPath)
          .isEqualTo('')

      actual.children[0].with { root ->
        assertThat(root.children).hasSize(1)
            .first()
            .isInstanceOf(DirectoryResults)
            .extracting(Results::getPath)
            .isEqualTo('org')

        root.children[0].with { org ->
          assertThat(org.children).hasSize(1)
              .first()
              .isInstanceOf(DirectoryResults)
              .extracting(Results::getPath)
              .isEqualTo('org/example')

          org.children[0].with { example ->
            assertThat(example.children).hasSize(3)
                .extracting(Results::getPath as Function)
                .containsExactlyInAnyOrder('org/example/util', 'org/example/Bar.groovy', 'org/example/BarSpec')

            example.children.find { it.path == 'org/example/util' }.with { util ->
              assertThat(util).isInstanceOf(DirectoryResults)
              assertThat(util.children).hasSize(1)
                  .first()
                  .isInstanceOf(FileResults)
                  .extracting(Results::getPath, Results::getViolations as Function)
                  .containsExactly('org/example/util/Foo.groovy', [DUMMY])
            }
            assertThat(example.children.find { it.path == 'org/example/Bar.groovy' })
                .isInstanceOf(FileResults)
                .extracting(Results::getViolations)
                .isEqualTo([DUMMY, DUMMY])
            assertThat(example.children.find { it.path == 'org/example/BarSpec' })
                .isInstanceOf(FileResults)
                .extracting(Results::getViolations)
                .isEqualTo([DUMMY, DUMMY, DUMMY])
          }
        }
      }
    }
  }

  @Nested
  class WhenConflictingFilePathsThenKeepFirst {

    @Test
    void merge() {
      // Act
      def actual = ResultsMerger.merge([tree1(), tree2()])

      // Assert
      assertThat(actual).isInstanceOf(DirectoryResults)
          .extracting(Results::getPath)
          .isNull()
      assertThat(actual.children).hasSize(1)
          .first()
          .isInstanceOf(DirectoryResults)
          .extracting(Results::getPath)
          .isEqualTo('')

      actual.children[0].with { root ->
        assertThat(root.children).hasSize(1)
            .first()
            .isInstanceOf(FileResults)
            .extracting(Results::getPath, Results::getViolations as Function)
            .containsExactly('Foo.groovy', [DUMMY])
      }
    }

    @Test
    void mergeInReverseOrder() {
      // Act
      def actual = ResultsMerger.merge([tree2(), tree1()])

      // Assert
      assertThat(actual).isInstanceOf(DirectoryResults)
          .extracting(Results::getPath)
          .isNull()
      assertThat(actual.children).hasSize(1)
          .first()
          .isInstanceOf(DirectoryResults)
          .extracting(Results::getPath)
          .isEqualTo('')

      actual.children[0].with { root ->
        assertThat(root.children).hasSize(1)
            .first()
            .isInstanceOf(FileResults)
            .extracting(Results::getPath, Results::getViolations as Function)
            .containsExactly('Foo.groovy', [DUMMY, DUMMY])
      }
    }

    /**
     * Returns a directory tree with the following structure:
     * <pre><code>
     * .
     * └─ Foo.groovy (1 violation)
     * </code></pre>
     */
    DirectoryResults tree1() {
      new DirectoryResults().tap {
        def root = new DirectoryResults('').tap {
          addChild(new FileResults('Foo.groovy', [DUMMY]))
        }
        addChild(root)
      }
    }

    /**
     * Returns a directory tree with the following structure:
     * <pre><code>
     * .
     * └─ Foo.groovy (2 violations)
     * </code></pre>
     */
    DirectoryResults tree2() {
      new DirectoryResults().tap {
        def root = new DirectoryResults('').tap {
          addChild(new FileResults('Foo.groovy', [DUMMY, DUMMY]))
        }
        addChild(root)
      }
    }
  }

  @Nested
  class WhenConflictingFilePathAndDirectoryPathThenKeepBoth {

    @Test
    void merge() {
      // Act
      def actual = ResultsMerger.merge([tree1(), tree2()])

      // Assert
      assertMergedTree(actual)
    }

    @Test
    void mergeInReverseOrder() {
      // Act
      def actual = ResultsMerger.merge([tree2(), tree1()])

      // Assert
      assertMergedTree(actual)
    }

    /**
     * Returns a directory tree with the following structure:
     * <pre><code>
     * .
     * └─ foo (1 violation)
     * </code></pre>
     */
    DirectoryResults tree1() {
      new DirectoryResults().tap {
        def root = new DirectoryResults('').tap {
          addChild(new FileResults('foo', [DUMMY]))
        }
        addChild(root)
      }
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
      new DirectoryResults().tap {
        def root = new DirectoryResults('').tap {
          def foo = new DirectoryResults('foo').tap {
            addChild(new FileResults('foo/bar', [DUMMY, DUMMY]))
          }
          addChild(foo)
        }
        addChild(root)
      }
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
      assertThat(actual).isInstanceOf(DirectoryResults)
          .extracting(Results::getPath)
          .isNull()
      assertThat(actual.children).hasSize(1)
          .first()
          .isInstanceOf(DirectoryResults)
          .extracting(Results::getPath)
          .isEqualTo('')

      actual.children[0].with { root ->
        assertThat(root.children).hasSize(2)
            .extracting(Results::getPath as Function)
            .containsExactlyInAnyOrder('foo', 'foo')
        assertThat(root.children.find { it instanceof FileResults })
            .extracting(Results::getViolations)
            .isEqualTo([DUMMY])

        root.children.find { it instanceof DirectoryResults }.with { foo ->
          assertThat(foo.children).hasSize(1)
              .first()
              .isInstanceOf(FileResults)
              .extracting(Results::getPath, Results::getViolations as Function)
              .containsExactly('foo/bar', [DUMMY, DUMMY])
        }
      }
    }
  }
}
