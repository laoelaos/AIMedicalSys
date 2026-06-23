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

class ParentPomTest {

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

    private boolean exists(String expr) throws Exception {
        return (Boolean) xpath.evaluate(expr, doc, XPathConstants.BOOLEAN);
    }

    @Test
    void dependencyManagementShouldNotContainSpringBootStarters() throws Exception {
        String base = "/project/dependencyManagement/dependencies/dependency";
    }

    @Test
    void dependencyManagementShouldNotContainBusinessModules() throws Exception {
        String base = "/project/dependencyManagement/dependencies/dependency";
        assertFalse(exists(base + "[groupId='com.aimedical' and artifactId='patient']"));
        assertFalse(exists(base + "[groupId='com.aimedical' and artifactId='doctor']"));
        assertFalse(exists(base + "[groupId='com.aimedical' and artifactId='admin']"));
    }

    @Test
    void dependencyManagementShouldContainCoreInternalModules() throws Exception {
        String base = "/project/dependencyManagement/dependencies/dependency";
        assertTrue(exists(base + "[groupId='com.aimedical' and artifactId='common']"));
        assertTrue(exists(base + "[groupId='com.aimedical' and artifactId='common-module-api']"));
        assertTrue(exists(base + "[groupId='com.aimedical' and artifactId='common-module-impl']"));
        assertTrue(exists(base + "[groupId='com.aimedical' and artifactId='ai-api']"));
        assertTrue(exists(base + "[groupId='com.aimedical' and artifactId='ai-impl']"));
        assertTrue(exists(base + "[groupId='com.aimedical' and artifactId='application']"));
    }

    @Test
    void ignoredUnusedDeclaredDependenciesShouldContainOnlySpiModules() throws Exception {
        String base = "/project/build/plugins/plugin[artifactId='maven-dependency-plugin']/configuration/ignoredUnusedDeclaredDependencies/ignoredUnusedDeclaredDependency";
        assertTrue(exists(base + "[.='com.aimedical:ai-api']"));
        assertTrue(exists(base + "[.='com.aimedical:common-module-api']"));
        assertFalse(exists(base + "[.='com.aimedical:patient']"));
        assertFalse(exists(base + "[.='com.aimedical:doctor']"));
        assertFalse(exists(base + "[.='com.aimedical:admin']"));
    }
}
