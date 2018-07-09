package io.jenkins.plugins.coverage.adapter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import hudson.Extension;
import io.jenkins.plugins.coverage.adapter.convert.JSONToDocumentConverter;
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
    protected JSONToDocumentConverter getConverter() {
        return new LLVMCovToDocumentConverter();
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
    }


    public static class LLVMCovToDocumentConverter extends JSONToDocumentConverter {

        @Override
        protected Document convert(JSONObject jsonObject, Document document) throws CoverageException {
            if (!jsonObject.getString("version").equals("2.0.0") || !jsonObject.getString("type").equals("llvm.coverage.json.export")) {
                throw new CoverageException("Unsupported Json file");
            }

            Element reportEle = document.createElement("report");
            reportEle.setAttribute("name", "llvm-cov");
            document.appendChild(reportEle);

            JSONArray dataArr = jsonObject.getJSONArray("data");

            for (int i = 0; i < dataArr.size(); i++) {
                Element dataEle = document.createElement("data");
                dataEle.setAttribute("name", "data" + i);
                reportEle.appendChild(dataEle);

                JSONObject dataObj = dataArr.getJSONObject(i);

                processDataObj(dataObj, dataEle, document);
            }

            return document;
        }

        private void processDataObj(JSONObject dataObj, Element dataEle, Document document) {
            JSONArray files = dataObj.getJSONArray("files");
            JSONArray functions = dataObj.getJSONArray("functions");

            List<Element> fileElements = processFiles(files, document);

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
                                    if (Integer.parseInt(lineEleInFile.getAttribute("number")) == r.getInteger(0)) {
                                        Node n = lineEleInFile.cloneNode(true);
                                        functionEle.appendChild(n);
                                        break;
                                    }
                                }
                            });
                }

            }

        }

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
