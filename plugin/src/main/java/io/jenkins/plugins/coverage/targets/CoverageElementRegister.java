package io.jenkins.plugins.coverage.targets;


import java.util.*;
import java.util.stream.Collectors;

public class CoverageElementRegister {

    private static Map<String, LinkedList<CoverageElement>> typedRegisteredElements = new HashMap<>();

    static {
        addCoverageElement(CoverageElement.AGGREGATED_REPORT);
        addCoverageElement(CoverageElement.REPORT);
        addCoverageElement(CoverageElement.LINE);
        addCoverageElement(CoverageElement.CONDITIONAL);
    }

    public static boolean addCoverageElement(CoverageElement element) {
        return addCoverageElement(CoverageElement.COVERAGE_ELEMENT_TYPE_NONE, element);
    }

    public static boolean addCoverageElement(String type, CoverageElement element) {
        typedRegisteredElements.putIfAbsent(type, new LinkedList<>());
        return typedRegisteredElements.get(type).add(element);
    }

    public static boolean addCoverageElements(List<CoverageElement> elements) {
        return addCoverageElements(CoverageElement.COVERAGE_ELEMENT_TYPE_NONE, elements);
    }

    public static boolean addCoverageElements(String type, List<CoverageElement> elements) {
        typedRegisteredElements.putIfAbsent(type, new LinkedList<>());
        return typedRegisteredElements.get(type).addAll(elements);
    }

    public static CoverageElement get(String type, String name) {
        return typedRegisteredElements.get(type).stream().filter(c -> c.is(name)).findAny().orElse(null);
    }

    public static CoverageElement getDespiteType(String name) {
        return typedRegisteredElements.values().stream()
                .flatMap(Collection::stream)
                .filter(c -> c.is(name))
                .findAny().orElse(null);
    }

    public static CoverageElement[] all() {
        return typedRegisteredElements.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .collect(Collectors.toList())
                .toArray(new CoverageElement[]{});
    }

    public static CoverageElement[] listByType(String type) {
        return typedRegisteredElements.get(type).toArray(new CoverageElement[]{});
    }


    public static CoverageElement[] listCommonsAndSpecificType(String type) {
        CoverageElement[] elements =  typedRegisteredElements.entrySet().stream()
                .filter(e -> e.getKey().equals(CoverageElement.COVERAGE_ELEMENT_TYPE_NONE) || e.getKey().equals(type))
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .collect(Collectors.toList())
                .toArray(new CoverageElement[]{});

        Arrays.sort(elements);
        return elements;
    }
}
