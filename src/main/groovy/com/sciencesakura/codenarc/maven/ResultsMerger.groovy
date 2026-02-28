// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven

import groovy.transform.CompileStatic
import org.codenarc.results.DirectoryResults
import org.codenarc.results.Results

/**
 * Utility class to merge multiple {@link Results} into a single {@link DirectoryResults}.
 *
 * @author sciencesakura
 */
@CompileStatic
final class ResultsMerger {

  private ResultsMerger() {
  }

  /**
   * Merges multiple {@link Results} into a single {@link DirectoryResults}.
   *
   * @param results the results to merge
   * @return a {@link DirectoryResults} containing all the merged results
   */
  static Results merge(Iterable<Results> results) {
    def root = new DirectoryResults('')
    mergeInternal(root, results)
    new DirectoryResults().tap {
      addChild(root)
    }
  }

  private static void mergeInternal(DirectoryResults root, Iterable<Results> results) {
    results.each { r ->
      if (r instanceof DirectoryResults && !r.path) {
        mergeInternal(root, r.children)
      } else {
        insertSubNode(root, r)
      }
    }
  }

  private static void insertSubNode(DirectoryResults root, Results subNode) {
    def segments = splitPath(subNode.path)
    if (segments.empty) return

    def parent = ensureDirectory(root, segments.init())
    def existing = findChildByName(parent, segments.last())
    if (subNode instanceof DirectoryResults) {
      if (existing == null) {
        parent.addChild(new DirectoryResults(subNode.path))
      }
      subNode.children.each { insertSubNode(root, it) }
    } else {
      // keep first occurrence of a file if there are duplicates
      if (existing == null || existing instanceof DirectoryResults) {
        parent.addChild(subNode)
      }
    }
  }

  private static DirectoryResults ensureDirectory(DirectoryResults root, List<String> segments) {
    def current = root
    def acc = []
    segments.each { s ->
      acc << s
      def existing = findChildByName(current, s)
      if (existing instanceof DirectoryResults) {
        current = existing
      } else {
        def newDir = new DirectoryResults(acc.join('/'))
        current.addChild(newDir)
        current = newDir
      }
    }
    current
  }

  private static Results findChildByName(DirectoryResults parent, String name) {
    parent.children.find {
      def segments = splitPath(it.path)
      !segments.empty && segments.last() == name
    }
  }

  private static List<String> splitPath(String path) {
    path.split('/').findAll { !it.empty }
  }
}
