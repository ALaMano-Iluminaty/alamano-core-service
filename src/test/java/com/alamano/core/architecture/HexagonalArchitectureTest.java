package com.alamano.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class HexagonalArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.alamano.core");

    @Test
    void domainDoesNotDependOnOuterLayersOrSpring() {
        noClasses()
                .that().resideInAPackage("com.alamano.core.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.alamano.core.application..",
                        "com.alamano.core.infrastructure..",
                        "org.springframework..")
                .check(classes);
    }

    @Test
    void applicationDoesNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("com.alamano.core.application..")
                .should().dependOnClassesThat().resideInAPackage("com.alamano.core.infrastructure..")
                .check(classes);
    }
}
