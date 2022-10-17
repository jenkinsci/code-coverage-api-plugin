package io.jenkins.plugins.coverage.metrics;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.parser.XmlParser;
import edu.hm.hafner.util.FilteredLog;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;

/**
 * Scans files that match a specified Ant files pattern for issues and aggregates the coverage files into a single {@link
 * Node node} instance. This callable will be invoked on an agent so all fields and the returned node need to
 * be {@link Serializable}.
 *
 * @author Ullrich Hafner
 */
public class FilesScanner extends MasterToSlaveFileCallable<AggregatedResult> {
    private static final long serialVersionUID = -4242755766101768715L;

    private final String filePattern;
    private final CoverageParser parser;
    private final String encoding;
    private final boolean followSymbolicLinks;

    /**
     * Creates a new instance of {@link FilesScanner}.
     *
     * @param filePattern
     *         ant file-set pattern to scan for files to parse
     * @param encoding
     *         encoding of the files to parse
     * @param followSymbolicLinks
     *         if the scanner should traverse symbolic links
     * @param parser
     *         the parser to use
     */
    public FilesScanner(final String filePattern, final String encoding,
            final boolean followSymbolicLinks, final CoverageParser parser) {
        super();

        this.filePattern = filePattern;
        this.parser = parser;
        this.encoding = encoding;
        this.followSymbolicLinks = followSymbolicLinks;
    }

    @Override
    public AggregatedResult invoke(final File workspace, final VirtualChannel channel) {
        FilteredLog log = new FilteredLog("Errors while parsing with " + parser.getDisplayName());
        log.logInfo("Searching for all files in '%s' that match the pattern '%s'",
                workspace.getAbsolutePath(), filePattern);

        String[] fileNames = new FileFinder(filePattern, StringUtils.EMPTY, followSymbolicLinks).find(workspace);
        if (fileNames.length == 0) {
            log.logError("No files found for pattern '%s'. Configuration error?", filePattern);

            return new AggregatedResult(log, new ModuleNode(filePattern));
        }
        else {
            log.logInfo("-> found %s", plural(fileNames.length, "file"));

            return new AggregatedResult(log, scanFiles(workspace, fileNames, log));
        }

    }

    private Node scanFiles(final File workspace, final String[] fileNames, final FilteredLog log) {
        List<Node> nodes = new ArrayList<>();
        for (String fileName : fileNames) {
            Path file = workspace.toPath().resolve(fileName);

            if (!Files.isReadable(file)) {
                log.logError("Skipping file '%s' because Jenkins has no permission to read the file", fileName);
            }
            else if (isEmpty(file)) {
                log.logError("Skipping file '%s' because it's empty", fileName);
            }
            else {
                nodes.add(aggregateIssuesOfFile(file, log));
            }
        }
        return Node.merge(nodes);
    }

    private boolean isEmpty(final Path file) {
        try {
            return Files.size(file) <= 0;
        }
        catch (IOException e) {
            return true;
        }
    }

    private ModuleNode aggregateIssuesOfFile(final Path file, final FilteredLog log) {
        try {
            // FIXME: encoding?
            XmlParser xmlParser = parser.createParser();
            ModuleNode node = xmlParser.parse(Files.newBufferedReader(file));
            log.logInfo("Successfully parsed file '%s'", file);
            node.getMetricsDistribution().values().forEach(v -> log.logInfo("%s", v));
            return node;
        }
        catch (IOException exception) {
            log.logException(exception, "Parsing of file '%s' failed due to an exception:", file);
            return new ModuleNode(file.toString());
        }
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private String plural(final int count, final String itemName) {
        StringBuilder builder = new StringBuilder(itemName);
        if (count != 1) {
            builder.append('s');
        }
        builder.insert(0, ' ');
        builder.insert(0, count);
        return builder.toString();
    }
}
