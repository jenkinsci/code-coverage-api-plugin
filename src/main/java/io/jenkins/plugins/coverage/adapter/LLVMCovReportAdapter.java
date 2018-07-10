package io.jenkins.plugins.coverage.adapter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import hudson.Extension;
import io.jenkins.plugins.coverage.adapter.converter.JSONDocumentConverter;
import io.jenkins.plugins.coverage.adapter.parser.LLVMCoverageParser;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LLVMCovReportAdapter extends JSONCoverageReportAdapter {

    /**
     * @param path Ant-style path of report files.
     */
    @DataBoundConstructor
    public LLVMCovReportAdapter(String path) {
        super(path);
    }

    @Override
    protected JSONDocumentConverter getConverter() {
        return new LLVMCovDocumentConverter();
    }

    @CheckForNull
    @Override
    protected CoverageResult parseToResult(Document document, String reportName) {
        return new LLVMCoverageParser(reportName).parse(document);
    }


    @Symbol("llvm")
    @Extension
    public static class LLVMCovReportAdapterDescriptor extends CoverageReportAdapterDescriptor<CoverageReportAdapter> {

        public LLVMCovReportAdapterDescriptor() {
            super(LLVMCovReportAdapter.class);
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.LLVMCovReportAdapter_displayName();
        }
    }


    public static class LLVMCovDocumentConverter extends JSONDocumentConverter {

        @Override
        protected Document convert(JSONObject report, Document document) throws CoverageException {
            // only support 2.0.0 version now
            if (!report.getString("version").equals("2.0.0") || !report.getString("type").equals("llvm.coverage.json.export")) {
                throw new CoverageException("Unsupported Json file");
            }

            Element reportEle = document.createElement("report");
            reportEle.setAttribute("name", "llvm-cov");
            document.appendChild(reportEle);

            JSONArray dataArr = report.getJSONArray("data");

            for (int i = 0; i < dataArr.size(); i++) {
                Element dataEle = document.createElement("data");
                dataEle.setAttribute("name", "data" + i);
                reportEle.appendChild(dataEle);

                JSONObject dataObj = dataArr.getJSONObject(i);

                processDataObj(dataObj, dataEle, document);
            }

            return document;
        }

        /**
         * parse each data object in JSON, and convert it to data element and them to document.
         *
         * @param dataObj  data object in JSON
         * @param dataEle  data element added to document
         * @param document document
         */
        private void processDataObj(JSONObject dataObj, Element dataEle, Document document) {
            JSONArray files = dataObj.getJSONArray("files");
            JSONArray functions = dataObj.getJSONArray("functions");

            List<Element> fileElements = processFiles(files, document);

            // group file elements by its parent path
            fileElements.stream().collect(Collectors.groupingBy(f -> {
                String filename = f.getAttribute("filename");

                File path = new File(filename);
                if (StringUtils.isEmpty(path.getParent())) {
                    return ".";
                } else {
                    return path.getParent();
                }
            })).forEach((parentPath, fileEles) -> {
                Element directoryEle = document.createElement("directory");
                directoryEle.setAttribute("name", parentPath);
                fileEles.forEach(directoryEle::appendChild);
                dataEle.appendChild(directoryEle);
            });


            processFunctions(functions, fileElements, document);

        }

        /**
         * parse file objects in JSON format report, and convert them to file element.
         *
         * @param files    files array
         * @param document document
         * @return list of file elements
         */
        private List<Element> processFiles(JSONArray files, Document document) {
            List<Element> fileElements = new LinkedList<>();
            for (int i = 0; i < files.size(); i++) {
                JSONObject file = files.getJSONObject(i);

                Element fileEle = document.createElement("file");
                fileEle.setAttribute("filename", file.getString("filename"));

                JSONArray segments = file.getJSONArray("segments");

                Arrays.stream(segments.toArray())
                        .map(o -> (JSONArray) o)
                        .filter(s ->
                                s.getInteger(4) != 1) // if segment is a region, skip it.
                        .collect(Collectors.groupingBy(s -> s.getInteger(0))) // group by segment's line number
                        .forEach((line, segs) -> {
                            processLine(segs, line, fileEle, document);
                        });

                fileElements.add(fileEle);
            }
            return fileElements;
        }


        /**
         * parse function objects in JSON, and parse them and them to its correspond file element.
         *
         * @param functions    functions array
         * @param fileElements file elements
         * @param document     document
         */
        private void processFunctions(JSONArray functions, List<Element> fileElements, Document document) {
            for (int i = 0; i < functions.size(); i++) {
                Element functionEle = document.createElement("function");

                JSONObject function = functions.getJSONObject(i);
                String name = function.getString("name");
                JSONArray regions = function.getJSONArray("regions");
                JSONArray filenames = function.getJSONArray("filenames");

                functionEle.setAttribute("name", name);

                for (int j = 0; j < filenames.size(); j++) {
                    String filename = filenames.getString(j);

                    Optional<Element> correspondFileOptional = fileElements.stream()
                            .filter(f -> f.getAttribute("filename").equals(filename))
                            .findAny();

                    if (!correspondFileOptional.isPresent()) {
                        continue;
                    }

                    Element correspondFile = correspondFileOptional.get();

                    correspondFile.appendChild(functionEle);
                    Arrays.stream(regions.toArray())
                            .map(o -> (JSONArray) o)
                            .forEach(r -> {
                                NodeList lines = correspondFile.getElementsByTagName("line");

                                for (int k = 0; k < lines.getLength(); k++) {
                                    Element lineEleInFile = (Element) lines.item(k);
                                    int line = Integer.parseInt(lineEleInFile.getAttribute("number"));

                                    if (line >= r.getInteger(0) && line <= r.getInteger(2)) {
                                        Node n = lineEleInFile.cloneNode(true);
                                        functionEle.appendChild(n);
                                        break;
                                    }
                                }
                            });
                }

            }
        }

        /**
         * parse segments to lines, and add them to file element.
         *
         * @param segments segments
         * @param line     line number of segments
         * @param fileEle  file element that segments belong to
         * @param document document
         */
        private void processLine(List<JSONArray> segments, Integer line, Element fileEle, Document document) {
            if (segments.size() == 0) return;

            Element lineEle = document.createElement("line");

            if (segments.size() == 1) {
                JSONArray seg = segments.get(0);
                int count = seg.getInteger(2);
                int hasCount = seg.getInteger(3);

                if (hasCount == 0) {
                    count = 1;
                }


                lineEle.setAttribute("number", line + "");
                lineEle.setAttribute("hits", count + "");
                lineEle.setAttribute("branch", "false");
            } else if (segments.size() > 1) {
                // if one line map several segments, this line has branch
                int covered = 0;
                int uncovered = 0;
                int maxCount = 0;

                for (JSONArray segment : segments) {
                    int count = segment.getInteger(2);
                    int hasCount = segment.getInteger(3);

                    if (hasCount == 0) {
                        count = 1;
                    }

                    if (count != 0) {
                        covered++;
                    } else {
                        uncovered++;
                    }

                    maxCount = Math.max(maxCount, count);
                }

                int total = covered + uncovered;
                int coverage = (int) (((double) covered) / total * 100);

                lineEle.setAttribute("number", line + "");
                lineEle.setAttribute("hits", maxCount + "");
                lineEle.setAttribute("branch", "true");
                lineEle.setAttribute("condition-coverage", coverage + "% (" + covered + "/" + total + ")");
            }

            fileEle.appendChild(lineEle);
        }

    }

}
