package io.jenkins.plugins.coverage.adapter;

import com.google.common.collect.Lists;
import hudson.Extension;
import io.jenkins.plugins.coverage.adapter.parser.CoverageParser;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.List;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DListingReportAdapter extends CoverageReportAdapter {

    private static final String UNCOVERED = "0000000";
    private static final String NON_EXECUTABLE_CODE = "";

    @DataBoundConstructor
    public DListingReportAdapter(String path) {

        super(path == null || path.equals("") ? "*.lst" : path);
        setMergeToOneReport(true);
    }

    @Override
    public Document convert(File source) throws CoverageException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new CoverageException(e);
        }

        Document document = builder.newDocument();
        Element rootElement = document.createElement("report");
        rootElement.setAttribute("name", "D Listing Coverage");
        document.appendChild(rootElement);

        Element fileElement = document.createElement("file");

        try (BufferedReader br = Files.newBufferedReader(Paths.get(source.getPath()))) {
            List<String> list = br.lines().collect(Collectors.toList());

            if (list.size() == 0) {
                return document;
            }

            String lastLine = list.get(list.size() - 1);
            String moduleFileName = extractModuleFileName(lastLine);

            if (moduleFileName == null) {
                return document;
            }

            fileElement.setAttribute("name", moduleFileName);

            int i = 0;
            for (String line : list) {
                String[] parts = line.split("\\|");
                if (parts.length > 1) {
                    String lineStatistic = parts[0].trim();

                    if (!lineStatistic.equals(NON_EXECUTABLE_CODE)) {
                        Element lineElement = document.createElement("line");
                        lineElement.setAttribute("number", Integer.toString(i + 1));

                        String hits = (lineStatistic.equals(UNCOVERED)) ? "0" : lineStatistic.trim();
                        lineElement.setAttribute("hits", hits);
                        fileElement.appendChild(lineElement);
                    }
                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        rootElement.appendChild(fileElement);
        return document;
    }

    private String extractModuleFileName(String line) {
        Pattern patternIsCovered = Pattern.compile("(.*) is \\d+% covered");
        Pattern patternErrorIsCovered = Pattern.compile("Error: (.*) is \\d+% covered");
        Pattern patternHasNoCode = Pattern.compile("(.*) has no code");

        Matcher matcher = patternErrorIsCovered.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = patternHasNoCode.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = patternIsCovered.matcher(line);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    @Override
    public CoverageResult parseToResult(Document document, String reportName) throws CoverageException {
        return new DListingCoverageParser(reportName).parse(document);
    }

    @Symbol(value = {"dListingAdapter", "dListing"})
    @Extension
    public static final class DListingReportAdapterDescriptor extends CoverageReportAdapterDescriptor<CoverageReportAdapter> {

        public DListingReportAdapterDescriptor() {
            super(DListingReportAdapter.class);
        }

        @Override
        public List<CoverageElement> getCoverageElements() {
            return Lists.newArrayList(new CoverageElement("File", 0));
        }

        @Override
        public String getCoverageElementType() {
            return CoverageElement.COVERAGE_ELEMENT_TYPE_D;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.DListingReportAdapter_displayName();
        }
    }

    public static final class DListingCoverageParser extends CoverageParser {

        public DListingCoverageParser(String reportName) {
            super(reportName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected CoverageResult processElement(Element current, CoverageResult parentResult) {
            CoverageResult result = null;

            switch (current.getTagName()) {
                case "report":
                    result = new CoverageResult(CoverageElement.REPORT, null,
                            getAttribute(current, "name", "") + ": " + getReportName());
                    break;
                case "file":
                    result = new CoverageResult(CoverageElement.get("File"), parentResult,
                            getAttribute(current, "name", ""));
                    result.setRelativeSourcePath(getAttribute(current, "name", null));
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
}
