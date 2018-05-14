package io.jenkins.plugins.coverage.adapter.parser;

import io.jenkins.plugins.coverage.targets.Ratio;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageMetric;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * parse Java standard format coverage report to {@link CoverageResult}. <br/>
 *
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
 *
 */
public class JavaCoverageParser extends CoverageParser {

    private static final Pattern CONDITION_COVERAGE_PATTERN = Pattern.compile("(\\d*)\\s*\\%\\s*\\((\\d*)/(\\d*)\\)");


    /**
     * {@inheritDoc}
     */
    @Override
    protected CoverageResult processElement(Element current, CoverageResult parentResult) {
        CoverageResult result = null;
        switch (current.getLocalName()) {
            case "report":
                result = new CoverageResult(CoverageElement.REPORT, null, getNameAttribute(current, "report"));
                break;
            case "group":
                result = new CoverageResult(CoverageElement.JAVA_GROUP, parentResult, getNameAttribute(current, "project"));
                break;
            case "package":
                result = new CoverageResult(CoverageElement.JAVA_PACKAGE, parentResult, getNameAttribute(current, "<default>"));
                break;
            case "file":
                result = new CoverageResult(CoverageElement.JAVA_FILE, parentResult, getNameAttribute(current, ""));
                break;
            case "class":
                result = new CoverageResult(CoverageElement.JAVA_CLASS, parentResult, getNameAttribute(current, ""));
                break;
            case "method":
                result = new CoverageResult(CoverageElement.JAVA_METHOD, parentResult, getNameAttribute(current, ""));
                break;
            case "line":
                String hitsString = current.getAttribute("hits");
                String lineNumber = current.getAttribute("number");
                int denominator = 0;
                int numerator = 0;
                if (Boolean.parseBoolean(current.getAttribute("branch"))) {
                    final String conditionCoverage = current.getAttribute("condition-coverage");
                    if (conditionCoverage != null) {
                        // some cases in the wild have branch = true but no condition-coverage attribute

                        // should be of the format xxx% (yyy/zzz),
                        // or xxx % (yyy/zzz) for French,
                        // because cobertura uses the default locale as said in
                        // http://sourceforge.net/tracker/?func=detail&aid=3296149&group_id=130558&atid=720015
                        Matcher matcher = CONDITION_COVERAGE_PATTERN.matcher(conditionCoverage);
                        if (matcher.matches()) {
                            assert matcher.groupCount() == 3;
                            final String numeratorStr = matcher.group(2);
                            final String denominatorStr = matcher.group(3);
                            try {
                                numerator = Integer.parseInt(numeratorStr);
                                denominator = Integer.parseInt(denominatorStr);
                                parentResult.updateMetric(CoverageMetric.CONDITIONAL, Ratio.create(numerator, denominator));
                            } catch (NumberFormatException e) {
                                // ignore
                            }
                        }
                    }
                }
                try {
                    int hits = Integer.parseInt(hitsString);
                    int number = Integer.parseInt(lineNumber);
                    if (denominator == 0) {
                        parentResult.paint(number, hits);
                    } else {
                        parentResult.paint(number, hits, numerator, denominator);
                    }
                    parentResult.updateMetric(CoverageMetric.LINE, Ratio.create((hits == 0) ? 0 : 1, 1));
                } catch (NumberFormatException e) {
                    // ignore
                }
                break;
            default:
                break;
        }
        return result;
    }

    /**
     * Get value of name attribute from a element, if name attribute not exist or is empty, return default name.
     * @param e Element want to get name
     * @param defaultName default name
     * @return value of name attribute
     */
    private String getNameAttribute(Element e, String defaultName) {
        String name = e.getAttribute("name");
        return StringUtils.isEmpty(name) ? defaultName : name;
    }

}
