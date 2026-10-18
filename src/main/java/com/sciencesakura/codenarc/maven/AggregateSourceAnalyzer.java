// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.codenarc.analyzer.SourceAnalyzer;
import org.codenarc.results.Results;
import org.codenarc.ruleset.RuleSet;

/**
 * A {@link SourceAnalyzer} implementation that aggregates multiple source analyzers and combines their results.
 *
 * @author sciencesakura
 */
public class AggregateSourceAnalyzer implements SourceAnalyzer {

  private final List<Object> sourceDirectories;

  private final List<SourceAnalyzer> delegates;

  /**
   * Constructs a new {@code AggregateSourceAnalyzer} with the specified source analyzers.
   *
   * @param sourceAnalyzers the source analyzers to aggregate
   */
  public AggregateSourceAnalyzer(Collection<? extends SourceAnalyzer> sourceAnalyzers) {
    var dirs = new ArrayList<>(sourceAnalyzers.size());
    for (var analyzer : sourceAnalyzers) {
      dirs.addAll(analyzer.getSourceDirectories());
    }
    this.sourceDirectories = Collections.unmodifiableList(dirs);
    this.delegates = List.copyOf(sourceAnalyzers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Object> getSourceDirectories() {
    return sourceDirectories;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Results analyze(RuleSet ruleSet) {
    return ResultsMerger.merge(delegates.stream()
        .map(d -> d.analyze(ruleSet))
        .collect(Collectors.toList()));
  }
}
