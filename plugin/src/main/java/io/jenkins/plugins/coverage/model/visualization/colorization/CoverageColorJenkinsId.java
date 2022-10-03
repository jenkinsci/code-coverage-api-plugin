package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains color IDs which represent the keys of a JSON object that is dynamically filled with the currently set
 * Jenkins colors.
 *
 * @author Florian Orendi
 */
public enum CoverageColorJenkinsId {

    GREEN("--green"),
    LIGHT_GREEN("--light-green"),
    YELLOW("--yellow"),
    LIGHT_YELLOW("--light-yellow"),
    ORANGE("--orange"),
    LIGHT_ORANGE("--light-orange"),
    RED("--red"),
    LIGHT_RED("--light-red");

    private final String jenkinsColorId;

    CoverageColorJenkinsId(final String colorId) {
        this.jenkinsColorId = colorId;
    }

    public String getJenkinsColorId() {
        return jenkinsColorId;
    }

    public static Set<String> getAll() {
        return Arrays.stream(values())
                .map(id -> id.jenkinsColorId)
                .collect(Collectors.toSet());
    }
}
