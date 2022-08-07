package io.jenkins.plugins.coverage.model;

import java.util.List;
import java.util.stream.Collectors;

import edu.hm.hafner.util.FilteredLog;

import one.util.streamex.StreamEx;

/**
 * Validates the file paths of the processed project files.
 *
 * @author Florian Orendi
 */
class FilePathValidator {

    static final String AMBIGUOUS_FILES_MESSAGE =
            "There are ambiguous file paths which might lead to faulty coverage reports:";
    static final String REMOVED_MESSAGE =
            "-> These files have been removed from the report in order to guarantee flawless coverage visualizations";
    static final String PACKAGE_INFO_MESSAGE =
            "-> In order to avoid this make sure package names are unique within the whole project";

    /**
     * Private default constructor.
     */
    private FilePathValidator() {
        // hides the public constructor
    }

    /**
     * Verifies that the passed coverage tree only contains files with unique paths. Duplicate paths are logged and
     * removed from the coverage tree.
     *
     * @param root
     *         The {@link CoverageNode root} of the coverage tree
     * @param log
     *         The log
     */
    static void verifyPathUniqueness(final CoverageNode root, final FilteredLog log) {
        List<String> duplicates = StreamEx.of(root.getAllFileCoverageNodes())
                .map(FileCoverageNode::getPath)
                .distinct(2)
                .collect(Collectors.toList());
        if (!duplicates.isEmpty()) {
            root.getAllFileCoverageNodes().stream()
                    .filter(node -> duplicates.contains(node.getPath()))
                    .forEach(CoverageNode::remove);

            String message = AMBIGUOUS_FILES_MESSAGE + System.lineSeparator()
                    + String.join("," + System.lineSeparator(), duplicates);
            log.logError(message);
            log.logError(REMOVED_MESSAGE);
            log.logError(PACKAGE_INFO_MESSAGE);
        }
    }

}
