package com.robertosrjr.pedidos;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Regra de dependência da Arquitetura Hexagonal (ver skill architecture-guidance). */
@AnalyzeClasses(packages = "com.robertosrjr.pedidos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule domain_should_not_depend_on_outer_layers =
		noClasses().that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..");

	@ArchTest
	static final ArchRule domain_should_not_depend_on_frameworks =
		noClasses().that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
				"org.springframework..", "jakarta..", "io.micrometer..", "io.opentelemetry..", "org.slf4j..");

	@ArchTest
	static final ArchRule application_should_not_depend_on_infrastructure =
		noClasses().that().resideInAPackage("..application..")
			.should().dependOnClassesThat().resideInAPackage("..infrastructure..");

	@ArchTest
	static final ArchRule application_should_not_depend_on_frameworks =
		noClasses().that().resideInAPackage("..application..")
			.should().dependOnClassesThat().resideInAnyPackage(
				"org.springframework..", "jakarta.persistence..", "io.micrometer..");
}
