/*
 * Sonar Cxx Plugin, open source software quality management tool.
 * Copyright (C) 2010 - 2011, Neticoa SAS France - Tous droits reserves.
 * Author(s) : Franck Bonin, Neticoa SAS France.
 *
 * Sonar Cxx Plugin is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar Cxx Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar Cxx Plugin; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.cxx.valgrind;

import java.io.File;
import java.util.Set;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.RuleFinder;
import org.sonar.plugins.cxx.utils.CxxReportSensor;

/**
 * {@inheritDoc}
 */
public class CxxValgrindSensor extends CxxReportSensor {
  public static final String REPORT_PATH_KEY = "sonar.cxx.valgrind.reportPath";
  private static final String DEFAULT_REPORT_PATH = "valgrind-reports/valgrind-result-*.xml";
  private RulesProfile profile;
  
  /**
   * {@inheritDoc}
   */
  public CxxValgrindSensor(RuleFinder ruleFinder, Settings conf, RulesProfile profile) {
    super(ruleFinder, conf);
    this.profile = profile;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return super.shouldExecuteOnProject(project)
      && !profile.getActiveRulesByRepository(CxxValgrindRuleRepository.KEY).isEmpty();
  }
  
  @Override
  protected String reportPathKey() {
    return REPORT_PATH_KEY;
  }
  
  @Override
  protected String defaultReportPath() {
    return DEFAULT_REPORT_PATH;
  }

  @Override
  protected void processReport(final Project project, final SensorContext context, File report)
    throws javax.xml.stream.XMLStreamException
  {
    ValgrindReportParser parser = new ValgrindReportParser();
    saveErrors(project, context, parser.parseReport(report));
  }

  void saveErrors(Project project, SensorContext context, Set<ValgrindError> valgrindErrors) {
    for (ValgrindError error: valgrindErrors) {
      ValgrindFrame frame = error.getLastOwnFrame(project.getFileSystem().getBasedir().getPath());
      if(frame != null) {
        saveViolation(project, context, CxxValgrindRuleRepository.KEY,
                      frame.getPath(), frame.getLine(), error.getKind(), error.toString());
      }
    }
  }
}
