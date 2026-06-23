package com.aimedical.common.pom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class ParentPomDependencyManagementCleanupTest {

    private static Document doc;
    private static XPath xpath;

    @BeforeAll
    static void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        String pomPath = System.getProperty("parent.pom.path", "../pom.xml");
        doc = builder.parse(new File(pomPath));
        xpath = XPathFactory.newInstance().newXPath();
    }

    private boolean exists(String expr) throws Exception {
        return (Boolean) xpath.evaluate(expr, doc, XPathConstants.BOOLEAN);
    }

    @Test
    void webStarterShouldNotBeInDependencyManagement() throws Exception {
        assertFalse(exists("/project/dependencyManagement/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter-web']"));
    }

    @Test
    void dataJpaStarterShouldNotBeInDependencyManagement() throws Exception {
        assertFalse(exists("/project/dependencyManagement/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter-data-jpa']"));
    }

    @Test
    void securityStarterShouldNotBeInDependencyManagement() throws Exception {
        assertFalse(exists("/project/dependencyManagement/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter-security']"));
    }

    @Test
    void validationStarterShouldNotBeInDependencyManagement() throws Exception {
        assertFalse(exists("/project/dependencyManagement/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter-validation']"));
    }

    @Test
    void testStarterShouldNotBeInDependencyManagement() throws Exception {
        assertFalse(exists("/project/dependencyManagement/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter-test']"));
    }

    @Test
    void h2ShouldNotHaveScopeInDependencyManagement() throws Exception {
        assertFalse(exists("/project/dependencyManagement/dependencies/dependency[groupId='com.h2database' and artifactId='h2']/scope"));
    }
}
