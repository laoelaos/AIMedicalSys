package com.aimedical.common.pom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Disabled
class ParentPomVersionTest {

    private static Document doc;
    private static XPath xpath;

    @BeforeAll
    static void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.parse(new File("../pom.xml"));
        xpath = XPathFactory.newInstance().newXPath();
    }

    private String xpath(String expr) throws Exception {
        return xpath.evaluate(expr, doc);
    }

    @Test
    void webStarterShouldHaveVersionInDependencyManagement() throws Exception {
        String version = xpath("/project/dependencyManagement/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter-web']/version");
        assertFalse(version.isEmpty());
    }

    @Test
    void dataJpaStarterShouldHaveVersionInDependencyManagement() throws Exception {
        String version = xpath("/project/dependencyManagement/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter-data-jpa']/version");
        assertFalse(version.isEmpty());
    }

    @Test
    void securityStarterShouldHaveVersionInDependencyManagement() throws Exception {
        String version = xpath("/project/dependencyManagement/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter-security']/version");
        assertFalse(version.isEmpty());
    }

    @Test
    void validationStarterShouldHaveVersionInDependencyManagement() throws Exception {
        String version = xpath("/project/dependencyManagement/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter-validation']/version");
        assertFalse(version.isEmpty());
    }

    @Test
    void testStarterShouldHaveVersionInDependencyManagement() throws Exception {
        String version = xpath("/project/dependencyManagement/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter-test']/version");
        assertFalse(version.isEmpty());
    }
}
