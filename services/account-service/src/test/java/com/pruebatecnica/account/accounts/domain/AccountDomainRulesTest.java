package com.pruebatecnica.account.accounts.domain;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class AccountDomainRulesTest {

    @Test
    @DisplayName("DEC-05 el dominio de cuenta no depende de la web ni de JPA")
    void domainStaysFreeOfInfrastructure() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.pruebatecnica.account.accounts.domain");
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..",
                        "jakarta.persistence..",
                        "org.springframework.data..");
        rule.check(classes);
    }
}
