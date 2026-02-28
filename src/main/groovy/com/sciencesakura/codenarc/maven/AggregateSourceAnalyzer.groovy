// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven

import groovy.transform.CompileStatic
import org.codenarc.analyzer.SourceAnalyzer
import org.codenarc.results.Results
import org.codenarc.ruleset.RuleSet

/**
 * A {@link SourceAnalyzer} implementation that aggregates multiple source analyzers and combines their results.
 *
 * @author sciencesakura
 */
@CompileStatic
class AggregateSourceAnalyzer implements SourceAnalyzer {

  /** The list of source directories to analyze. */
  final List sourceDirectories

  private final List<SourceAnalyzer> delegates

  /**
   * Constructs a new {@code AggregateSourceAnalyzer} with the specified source analyzers.
   *
   * @param sourceAnalyzers the source analyzers to aggregate
   */
  AggregateSourceAnalyzer(Collection<SourceAnalyzer> sourceAnalyzers) {
    this.sourceDirectories = sourceAnalyzers.inject([]) { acc, cur ->
      acc.addAll(cur.sourceDirectories)
      acc
    }
    this.delegates = List.copyOf(sourceAnalyzers)
  }

  /**
   * {@inheritDoc}
   */
  @Override
  Results analyze(RuleSet ruleSet) {
    ResultsMerger.merge(delegates*.analyze(ruleSet))
  }
}
