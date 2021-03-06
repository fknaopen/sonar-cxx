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

package org.sonar.plugins.cxx;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration.Configuration;
import org.apache.tools.ant.DirectoryScanner;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;

public class TestUtils{
  public static RuleFinder mockRuleFinder(){
    Rule ruleMock = Rule.create("", "", "");
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey((String) anyObject(),
        (String) anyObject())).thenReturn(ruleMock);
    when(ruleFinder.find((RuleQuery) anyObject())).thenReturn(ruleMock);
    return ruleFinder;
  }

  public static File loadResource(String resourceName) {
    URL resource = TestUtils.class.getResource(resourceName);
    File resourceAsFile = null;
    try{
      resourceAsFile = new File(resource.toURI());
    } catch (URISyntaxException e) {
      System.out.println("Cannot load resource: " + resourceName);
    }
    
    return resourceAsFile;
  }

  /**
   * @return  default mock project
   */
  public static Project mockProject() {
    File baseDir;
    baseDir = loadResource("/org/sonar/plugins/cxx/");  //we skip "SampleProject" dir because report dirs as here
    
    List<File> sourceDirs = new ArrayList<File>();
    sourceDirs.add(loadResource("/org/sonar/plugins/cxx/SampleProject/sources/application/") );
    sourceDirs.add(loadResource("/org/sonar/plugins/cxx/SampleProject/sources/utils/")); 
    
    List<File> testDirs = new ArrayList<File>();      
    testDirs.add(loadResource("/org/sonar/plugins/cxx/SampleProject/sources/tests/"));
    
    return mockProject(baseDir, sourceDirs, testDirs);
  }
  
  /**
   * Mock project
   * @param baseDir project base dir
   * @param sourceFiles project source files
   * @return  mocked project
   */
  public static Project mockProject(File baseDir, List<File> sourceDirs, List<File> testDirs) {
    List<File> mainSourceFiles = scanForSourceFiles(sourceDirs);
    List<File> testSourceFiles = scanForSourceFiles(testDirs);
    
    List<InputFile> mainFiles = fromSourceFiles(mainSourceFiles);
    List<InputFile> testFiles = fromSourceFiles(testSourceFiles);
    
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getBasedir()).thenReturn(baseDir);
    when(fileSystem.getSourceCharset()).thenReturn(Charset.defaultCharset());
    when(fileSystem.getSourceFiles(mockCxxLanguage())).thenReturn(mainSourceFiles);
    when(fileSystem.getTestFiles(mockCxxLanguage())).thenReturn(testSourceFiles);
    when(fileSystem.mainFiles(CxxLanguage.KEY)).thenReturn(mainFiles);
    when(fileSystem.testFiles(CxxLanguage.KEY)).thenReturn(testFiles);
    when(fileSystem.getSourceDirs()).thenReturn(sourceDirs);
    when(fileSystem.getTestDirs()).thenReturn(testDirs);

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(fileSystem);
    CxxLanguage lang = mockCxxLanguage();
    when(project.getLanguage()).thenReturn(lang);
    when(project.getLanguageKey()).thenReturn(lang.getKey());
    // only for testing, Configuration is deprecated
    Configuration configuration = mock(Configuration.class);
    when(configuration.getBoolean(CoreProperties.CORE_IMPORT_SOURCES_PROPERTY,
        CoreProperties.CORE_IMPORT_SOURCES_DEFAULT_VALUE)).thenReturn(true);
    when(project.getConfiguration()).thenReturn(configuration);
    
    return project;
  }

  private static List<InputFile> fromSourceFiles(List<File> sourceFiles){
    List<InputFile> result = new ArrayList<InputFile>();
    for(File file: sourceFiles) {
      InputFile inputFile = mock(InputFile.class);
      when(inputFile.getFile()).thenReturn(new File(file, ""));
      result.add(inputFile);
    }
    return result;
  }

  public static CxxLanguage mockCxxLanguage(){
    return new CxxLanguage(new Settings());
  }
  
  private static List<File> scanForSourceFiles(List<File> sourceDirs) {
    List<File> result = new ArrayList<File>();
    String[] suffixes = mockCxxLanguage().getFileSuffixes();
    String[] includes = new String[ suffixes.length ];
    for(int i = 0; i < includes.length; ++i) {
      includes[i] = "**/*." + suffixes[i];
    }
    
    DirectoryScanner scanner = new DirectoryScanner();
    for(File baseDir : sourceDirs) {
      scanner.setBasedir(baseDir);
      scanner.setIncludes(includes);  
      scanner.scan();
      for (String relPath : scanner.getIncludedFiles()) {
        File f = new File(baseDir, relPath);
        result.add(f);
      }  
    }
    
    return result;
  }
}
