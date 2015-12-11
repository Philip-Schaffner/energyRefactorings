package com.intellij.codeInspection;

/**
 * @author max
 */
public class FindHTTPCallInLoopProvider implements InspectionToolProvider {
  public Class[] getInspectionClasses() {
    return new Class[] { FindHTTPCallInLoopInspection.class};
  }
}
