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

class ApplicationPomTest {

    private static Document doc;
    private static XPath xpath;

    @BeforeAll
    static void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.parse(new File("../application/pom.xml"));
        xpath = XPathFactory.newInstance().newXPath();
    }

    private boolean exists(String expr) throws Exception {
        return (Boolean) xpath.evaluate(expr, doc, XPathConstants.BOOLEAN);
    }

    @Test
    void shouldContainMavenDependencyPlugin() throws Exception {
        assertTrue(exists("/project/build/plugins/plugin[artifactId='maven-dependency-plugin']"));
    }

    @Test
    void shouldHaveAllSevenIgnoredUnusedDeclaredDependencies() throws Exception {
        String base = "/project/build/plugins/plugin[artifactId='maven-dependency-plugin']/configuration/ignoredUnusedDeclaredDependencies/ignoredUnusedDeclaredDependency";
        assertTrue(exists(base + "[.='com.aimedical:ai-api']"));
        assertTrue(exists(base + "[.='com.aimedical:common-module-api']"));
        assertTrue(exists(base + "[.='com.aimedical:patient']"));
        assertTrue(exists(base + "[.='com.aimedical:doctor']"));
        assertTrue(exists(base + "[.='com.aimedical:admin']"));
        assertTrue(exists(base + "[.='com.aimedical:medical-order']"));
        assertTrue(exists(base + "[.='com.aimedical:registration']"));
    }

    @Test
    void shouldNotDefineVersionOnMavenDependencyPlugin() throws Exception {
        String version = xpath.evaluate("/project/build/plugins/plugin[artifactId='maven-dependency-plugin']/version", doc);
        assertTrue(version == null || version.isEmpty());
    }

    @Test
    void patientDependencyShouldHaveVersion() throws Exception {
        String version = xpath.evaluate("/project/dependencies/dependency[artifactId='patient']/version", doc);
        assertFalse(version.isEmpty());
    }

    @Test
    void doctorDependencyShouldHaveVersion() throws Exception {
        String version = xpath.evaluate("/project/dependencies/dependency[artifactId='doctor']/version", doc);
        assertFalse(version.isEmpty());
    }

    @Test
    void adminDependencyShouldHaveVersion() throws Exception {
        String version = xpath.evaluate("/project/dependencies/dependency[artifactId='admin']/version", doc);
        assertFalse(version.isEmpty());
    }
}
