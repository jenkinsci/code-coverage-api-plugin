package io.jenkins.plugins.coverage.adapter.parser;

import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.w3c.dom.Element;

/**
 * <p>parse Java standard format coverage report to {@link CoverageResult}.</p>
 * <p>
 * The standard format should be like this
 * <pre>
 *
 * {@code
 * <report name="cobertura">
 *     <group name="io.jenkins.plugins.coverage">
 *         <package name="io.jenkins.plugins.coverage.adapter.parser">
 *             <file name="JavaCoverageParser.java">
 *                 <class name="io.jenkins.plugins.coverage.adapter.parser.JavaCoverageParser">
 *                     <method name="processElement" signature="...">
 *                         <line number="1" hits="1"/>
 *                     </method>
 *                     ...
 *                     <line number="1" hits="1" branch="false"/>
 *                     <line number="2" hits="11" branch="false"/>
 *                     ...
 *                 </class>
 *                 ...
 *             </file>
 *             ...
 *         </package>
 *         ...
 *     </group>
 *     ...
 * </report>
 * }
 * </pre>
 */
public class JavaCoverageParser extends CoverageParser {

    public JavaCoverageParser(String reportName) {
        super(reportName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CoverageResult processElement(Element current, CoverageResult parentResult) {
        CoverageResult result = null;
        switch (current.getLocalName()) {
            case "report":
                result = new CoverageResult(CoverageElement.REPORT, null,
                        getAttribute(current, "name", "") + ": " + getReportName());
                break;
            case "group":
                result = new CoverageResult(CoverageElement.get("Java Group"), parentResult,
                        getAttribute(current, "name", "project"));
                break;
            case "package":
                result = new CoverageResult(CoverageElement.get("Java Package"), parentResult,
                        getAttribute(current, "name", "<default>"));
                break;
            case "file":
                result = new CoverageResult(CoverageElement.get("Java File"), parentResult,
                        getAttribute(current, "name", ""));
                result.setRelativeSourcePath(getAttribute(current, "name", null));
                break;
            case "class":
                result = new CoverageResult(CoverageElement.get("Java Class"), parentResult,
                        getAttribute(current, "name", ""));
                break;
            case "method":
                result = new CoverageResult(CoverageElement.get("Java Method"), parentResult,
                        getAttribute(current, "name", ""));
                break;
            case "line":
                processLine(current, parentResult);
                break;
            default:
                break;
        }
        return result;
    }


}
