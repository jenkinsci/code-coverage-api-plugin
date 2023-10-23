package io.jenkins.plugins.coverage.model;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import edu.hm.hafner.util.ArchitectureRules;

import io.jenkins.plugins.util.PluginArchitectureRules;

/**
 * Defines several architecture rules for the static analysis model and parsers.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("hideutilityclassconstructor")
@AnalyzeClasses(packages = "io.jenkins.plugins.coverage.model")
class PluginArchitectureTest {
    @ArchTest
    static final ArchRule NO_EXCEPTIONS_WITH_NO_ARG_CONSTRUCTOR = ArchitectureRules.NO_EXCEPTIONS_WITH_NO_ARG_CONSTRUCTOR;

    @ArchTest
    static final ArchRule NO_PUBLIC_TEST_CLASSES = ArchitectureRules.NO_PUBLIC_TEST_CLASSES;

    @ArchTest
    static final ArchRule NO_PUBLIC_TEST_METHODS = ArchitectureRules.ONLY_PACKAGE_PRIVATE_TEST_METHODS;

    @ArchTest
    static final ArchRule NO_TEST_API_CALLED = ArchitectureRules.NO_TEST_API_CALLED;

    @ArchTest
    static final ArchRule NO_FORBIDDEN_ANNOTATION_USED = ArchitectureRules.NO_FORBIDDEN_ANNOTATION_USED;

    @ArchTest
    static final ArchRule NO_FORBIDDEN_CLASSES_CALLED = ArchitectureRules.NO_FORBIDDEN_CLASSES_CALLED;

    @ArchTest
    static final ArchRule ONLY_PACKAGE_PRIVATE_ARCHITECTURE_TESTS = ArchitectureRules.ONLY_PACKAGE_PRIVATE_ARCHITECTURE_TESTS;

    @ArchTest
    static final ArchRule NO_JENKINS_INSTANCE_CALL = PluginArchitectureRules.NO_JENKINS_INSTANCE_CALL;

    @ArchTest
    static final ArchRule NO_FORBIDDEN_PACKAGE_ACCESSED = PluginArchitectureRules.NO_FORBIDDEN_PACKAGE_ACCESSED;

    @ArchTest
    static final ArchRule AJAX_PROXY_METHOD_MUST_BE_IN_PUBLIC_CLASS = PluginArchitectureRules.AJAX_PROXY_METHOD_MUST_BE_IN_PUBLIC_CLASS;

    @ArchTest
    static final ArchRule DATA_BOUND_CONSTRUCTOR_MUST_BE_IN_PUBLIC_CLASS = PluginArchitectureRules.DATA_BOUND_CONSTRUCTOR_MUST_BE_IN_PUBLIC_CLASS;

    @ArchTest
    static final ArchRule DATA_BOUND_SETTER_MUST_BE_IN_PUBLIC_CLASS = PluginArchitectureRules.DATA_BOUND_SETTER_MUST_BE_IN_PUBLIC_CLASS;

    @ArchTest
    static final ArchRule USE_POST_FOR_VALIDATION_END_POINTS = PluginArchitectureRules.USE_POST_FOR_VALIDATION_END_POINTS;

    @ArchTest
    static final ArchRule USE_POST_FOR_LIST_MODELS_RULE = PluginArchitectureRules.USE_POST_FOR_LIST_AND_COMBOBOX_FILL;
}
