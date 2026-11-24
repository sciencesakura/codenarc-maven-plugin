// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.codenarc.results.DirectoryResults;
import org.codenarc.results.FileResults;
import org.codenarc.results.Results;
import org.codenarc.rule.Violation;

/**
 * Helpers for building and comparing CodeNarc {@link Results} in tests.
 */
final class TestResults {

  static final Violation DUMMY_VIOLATION = new Violation();

  private TestResults() {
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
