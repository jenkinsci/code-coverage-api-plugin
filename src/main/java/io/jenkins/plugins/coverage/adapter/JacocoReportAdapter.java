package io.jenkins.plugins.coverage.adapter;

import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.kohsuke.stapler.DataBoundConstructor;
import org.jenkinsci.Symbol;
import hudson.Extension;

import io.jenkins.plugins.coverage.adapter.parser.JavaCoverageParser;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;

/**
 * Reads JaCoCo results.
 */
public final class JacocoReportAdapter extends JavaXMLCoverageReportAdapter {
    @DataBoundConstructor
    public JacocoReportAdapter(final String path) {
        super(path);
    }

    @Override
    public String getXSL() {
        return "jacoco-to-standard.xsl";
    }

    @Override
    public String getXSD() {
        return null;
    }

    @Override
    public CoverageResult parseToResult(final Document document, final String reportName) throws CoverageException {
        return new JacocoCoverageParser(reportName).parse(document);
    }

    @Symbol(value = {"jacocoAdapter", "jacoco"})
    @Extension
    public static final class JacocoReportAdapterDescriptor extends JavaCoverageReportAdapterDescriptor {
        public static final CoverageElement INSTRUCTION = new CoverageElement("Instruction", 5, true);

        public JacocoReportAdapterDescriptor() {
            super(JacocoReportAdapter.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.JacocoReportAdapter_displayName();
        }

        @Override
        public List<CoverageElement> getCoverageElements() {
            List<CoverageElement> registerCoverageElements = super.getCoverageElements();
            registerCoverageElements.add(INSTRUCTION);
            return registerCoverageElements;
        }
    }

    public static final class JacocoCoverageParser extends JavaCoverageParser {
        private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile("\\((.*)\\)(.*)");
        private static final Pattern METHOD_ARGS_PATTERN = Pattern.compile("\\[*([TL][^;]*;)|([ZCBSIFJDV])");
        
        public JacocoCoverageParser(final String reportName) {
            super(reportName);
        }

        @Override
        protected CoverageResult processElement(final Element current, final CoverageResult parentResult) {
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
                    String rawClassName = getAttribute(current, "name", "");
                    String packageName = replacePathOrDollarWithDots(getAttribute(current, "name", "-"));
                    result = new CoverageResult(CoverageElement.get("Package"), parentResult, packageName);
                    result.setPackageSourcePath(rawClassName);
                    break;
                case "file":
                    // walk up the parent nodes and get the package path 
                    CoverageResult parent = parentResult;
                    while (parent != null && parent.getElement() != CoverageElement.get("Package")){
                        parent = parent.getParent();
                    }

                    if (parent != null){
                        result = new CoverageResult(CoverageElement.get("File"), parentResult,
                            parentResult.getPackageSourcePath() + '/' + getAttribute(current, "name", null));
                        result.setRelativeSourcePath(parentResult.getPackageSourcePath() + '/' + getAttribute(current, "name", null));
                    }
                    else{
                        result = new CoverageResult(CoverageElement.get("File"), parentResult,
                            getAttribute(current, "name", null));
                        result.setRelativeSourcePath(getAttribute(current, "name", null));
                    }
                    break;
                case "class":
                    String className = replacePathOrDollarWithDots(getAttribute(current, "name", "-"));
                    result = new CoverageResult(CoverageElement.get("Class"), parentResult, className);                    
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


            if (result == null) {
                return null;
            }
            if (getAttribute(current, "attr-mode", null) != null) {
                String lineCoveredAttr = getAttribute(current, "line-covered");
                String lineMissedAttr = getAttribute(current, "line-missed");

                String branchCoveredAttr = getAttribute(current, "br-covered");
                String branchMissedAttr = getAttribute(current, "br-missed");

                String instructionCoveredAttr = getAttribute(current, "instruction-covered");
                String instructionMissedAttr = getAttribute(current, "instruction-missed");

                if (StringUtils.isNumeric(lineCoveredAttr) && StringUtils.isNumeric(lineMissedAttr)) {
                    int covered = Integer.parseInt(lineCoveredAttr);
                    int missed = Integer.parseInt(lineMissedAttr);

                    result.updateCoverage(CoverageElement.LINE, Ratio.create(covered, covered + missed));
                }

                if (StringUtils.isNumeric(branchCoveredAttr) && StringUtils.isNumeric(branchMissedAttr)) {
                    int covered = Integer.parseInt(branchCoveredAttr);
                    int missed = Integer.parseInt(branchMissedAttr);

                    result.updateCoverage(CoverageElement.CONDITIONAL, Ratio.create(covered, covered + missed));
                }

                if (StringUtils.isNumeric(instructionCoveredAttr) && StringUtils.isNumeric(instructionMissedAttr)) {
                    int covered = Integer.parseInt(instructionCoveredAttr);
                    int missed = Integer.parseInt(instructionMissedAttr);

                    result.updateCoverage(CoverageElement.get("Instruction"), Ratio.create(covered, covered + missed));
                }

            }

            return result;
        }

        private String replacePathOrDollarWithDots(final String name) {
            if (StringUtils.isNotBlank(name)) {
                return name.replaceAll("[\\\\/$]", ".");
            }
            return name;
        }

        private String buildMethodName(final String name, final String signature) {
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
        private String parseMethodArg(final String s) {
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
}
