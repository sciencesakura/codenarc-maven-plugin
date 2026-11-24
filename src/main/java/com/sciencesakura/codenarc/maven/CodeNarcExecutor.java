// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven;

import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;
import org.codenarc.CodeNarcRunner;
import org.codenarc.analyzer.SourceAnalyzer;
import org.codenarc.report.ReportWriter;
import org.codenarc.results.Results;

@Named
@Singleton
class CodeNarcExecutor {

  Results execute(String ruleSetFiles, SourceAnalyzer sourceAnalyzer, List<ReportWriter> reportWriters) {
    var runner = new CodeNarcRunner();
    runner.setRuleSetFiles(ruleSetFiles);
    runner.setSourceAnalyzer(sourceAnalyzer);
    runner.setReportWriters(reportWriters);
    return runner.execute();
  }
}
