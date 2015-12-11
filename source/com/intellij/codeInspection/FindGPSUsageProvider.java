package com.intellij.codeInspection;

/**
 * @author max
 */
public class FindGPSUsageProvider implements InspectionToolProvider {
    public Class[] getInspectionClasses() {
        return new Class[] { FindGPSUsageInspection.class};
    }
}
