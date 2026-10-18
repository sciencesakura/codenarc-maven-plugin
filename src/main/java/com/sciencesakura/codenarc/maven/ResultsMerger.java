// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven;

import java.util.ArrayList;
import java.util.List;
import org.codenarc.results.DirectoryResults;
import org.codenarc.results.Results;

/**
 * Utility class to merge multiple {@link Results} into a single {@link DirectoryResults}.
 *
 * @author sciencesakura
 */
public final class ResultsMerger {

  private ResultsMerger() {
  }

  /**
   * Merges multiple {@link Results} into a single {@link DirectoryResults}.
   *
   * @param results the results to merge
   * @return a {@link DirectoryResults} containing all the merged results
   */
  public static Results merge(Iterable<? extends Results> results) {
    var root = new DirectoryResults("");
    mergeInternal(root, results);
    var merged = new DirectoryResults();
    merged.addChild(root);
    return merged;
  }

  private static void mergeInternal(DirectoryResults root, Iterable<? extends Results> results) {
    for (var r : results) {
      if (r instanceof DirectoryResults && (r.getPath() == null || r.getPath().isEmpty())) {
        mergeInternal(root, r.getChildren());
      } else {
        insertSubNode(root, r);
      }
    }
  }

  private static void insertSubNode(DirectoryResults root, Results subNode) {
    var segments = splitPath(subNode.getPath());
    if (segments.isEmpty()) {
      return;
    }

    var parent = ensureDirectory(root, segments.subList(0, segments.size() - 1));
    var existing = findChildByName(parent, segments.get(segments.size() - 1));
    if (subNode instanceof DirectoryResults) {
      if (existing == null) {
        parent.addChild(new DirectoryResults(subNode.getPath()));
      }
      for (var child : subNode.getChildren()) {
        insertSubNode(root, child);
      }
    } else {
      // keep the first occurrence of a file if there are duplicates
      if (existing == null || existing instanceof DirectoryResults) {
        parent.addChild(subNode);
      }
    }
  }

  private static DirectoryResults ensureDirectory(DirectoryResults root, List<String> segments) {
    var current = root;
    var acc = new ArrayList<String>(segments.size());
    for (var s : segments) {
      acc.add(s);
      var existing = findChildByName(current, s);
      if (existing instanceof DirectoryResults) {
        current = (DirectoryResults) existing;
      } else {
        var newDir = new DirectoryResults(String.join("/", acc));
        current.addChild(newDir);
        current = newDir;
      }
    }
    return current;
  }

  private static Results findChildByName(DirectoryResults parent, String name) {
    for (var child : parent.getChildren()) {
      var segments = splitPath(child.getPath());
      if (!segments.isEmpty() && segments.get(segments.size() - 1).equals(name)) {
        return child;
      }
    }
    return null;
  }

  private static List<String> splitPath(String path) {
    var segments = new ArrayList<String>();
    for (var s : path.split("/")) {
      if (!s.isEmpty()) {
        segments.add(s);
      }
    }
    return segments;
  }
}
