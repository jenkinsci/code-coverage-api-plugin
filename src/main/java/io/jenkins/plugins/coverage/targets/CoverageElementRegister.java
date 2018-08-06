package io.jenkins.plugins.coverage.targets;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoverageElementRegister {

    private static Set<CoverageElement> registeredElements = new HashSet<>();

    static {
        registeredElements.add(CoverageElement.AGGREGATED_REPORT);
        registeredElements.add(CoverageElement.REPORT);
        registeredElements.add(CoverageElement.LINE);
        registeredElements.add(CoverageElement.CONDITIONAL);
    }

    public static boolean addCoverageElement(CoverageElement element) {
        return registeredElements.add(element);
    }

    public static boolean addCoverageElements(List<CoverageElement> elements) {
        return registeredElements.addAll(elements);
    }

    public static CoverageElement[] all() {
        return registeredElements.toArray(new CoverageElement[]{});
    }

    public static CoverageElement get(String name) {
        return registeredElements.stream().filter(c -> c.is(name)).findAny().orElse(null);
    }

}
