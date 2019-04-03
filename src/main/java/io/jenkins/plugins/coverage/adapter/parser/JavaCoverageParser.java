package io.jenkins.plugins.coverage.adapter.parser;

import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile("\\((.*)\\)(.*)");
    private static final Pattern METHOD_ARGS_PATTERN = Pattern.compile("\\[*([TL][^;]*;)|([ZCBSIFJDV])");


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
                result = new CoverageResult(CoverageElement.get("Group"), parentResult,
                        getAttribute(current, "name", "project"));
                break;
            case "package":
                result = new CoverageResult(CoverageElement.get("Package"), parentResult,
                        getAttribute(current, "name", "<default>"));
                break;
            case "file":
                result = new CoverageResult(CoverageElement.get("File"), parentResult,
                        getAttribute(current, "name", ""));
                result.setRelativeSourcePath(getAttribute(current, "name", null));
                break;
            case "class":
                result = new CoverageResult(CoverageElement.get("Class"), parentResult,
                        getAttribute(current, "name", ""));
                break;
            case "method":
                String name = getAttribute(current, "name", "");
                String signature = getAttribute(current, "signature", "");

                String methodName = buildMethodName(name, signature);

                result = new CoverageResult(CoverageElement.get("Method"), parentResult, methodName);

                break;
            case "line":
                processLine(current, parentResult);
                break;
            case "additionalProperty":
                String propertyName = getAttribute(current, "name", "");
                if (StringUtils.isEmpty(propertyName)) {
                    break;
                }

                String propertyValue = getAttribute(current, "value", "");
                parentResult.addAdditionalProperty(propertyName, propertyValue);
                break;
            default:
                break;
        }
        return result;
    }


    /**
     * convert method type signature and name to Java method name.
     * <p>
     * For example, signature (ILjava/lang/String;[I)J with name test will be converted to Java method <code>long test (int, String, int[]);</code>
     *
     * @param name      method name
     * @param signature method type signature
     * @return Java method name
     */
    private String buildMethodName(String name, String signature) {
        Matcher signatureMatcher = METHOD_SIGNATURE_PATTERN.matcher(signature);
        StringBuilder methodName = new StringBuilder();
        if (signatureMatcher.matches()) {
            String returnType = signatureMatcher.group(2);
            Matcher matcher = METHOD_ARGS_PATTERN.matcher(returnType);
            if (matcher.matches()) {
                methodName.append(parseMethodArg(matcher.group()));
                methodName.append(' ');
            }
            methodName.append(name);
            String args = signatureMatcher.group(1);
            matcher = METHOD_ARGS_PATTERN.matcher(args);
            methodName.append('(');
            boolean first = true;
            while (matcher.find()) {
                if (!first) {
                    methodName.append(',');
                }
                methodName.append(parseMethodArg(matcher.group()));
                first = false;
            }
            methodName.append(')');
        } else {
            methodName.append(name);
        }
        return methodName.toString();
    }

    /**
     * Convert type signature to Java type.
     * <p>
     * The type signature is Java VM's representation, more details see Java JNI document.
     *
     * @param s type signature
     * @return Java type
     */
    private String parseMethodArg(String s) {
        char c = s.charAt(0);
        int end;
        switch (c) {
            case 'Z':
                return "boolean";
            case 'C':
                return "char";
            case 'B':
                return "byte";
            case 'S':
                return "short";
            case 'I':
                return "int";
            case 'F':
                return "float";
            case 'J':
                return "long";
            case 'D':
                return "double";
            case 'V':
                return "void";
            case '[':
                return parseMethodArg(s.substring(1)) + "[]";
            case 'T':
            case 'L':
                end = s.indexOf(';');
                return s.substring(1, end).replace('/', '.');
        }
        return s;
    }
}
