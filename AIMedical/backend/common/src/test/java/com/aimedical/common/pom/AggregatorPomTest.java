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

class AggregatorPomTest {

    private static Document commonModuleDoc;
    private static Document aiDoc;
    private static XPath xpath;

    @BeforeAll
    static void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        commonModuleDoc = builder.parse(new File("../modules/common-module/pom.xml"));
        aiDoc = builder.parse(new File("../modules/ai/pom.xml"));
        xpath = XPathFactory.newInstance().newXPath();
    }

    private boolean exists(Document doc, String expr) throws Exception {
        return (Boolean) xpath.evaluate(expr, doc, XPathConstants.BOOLEAN);
    }

    private String xpath(Document doc, String expr) throws Exception {
        return xpath.evaluate(expr, doc);
    }

    @Test
    void commonModuleParentShouldBeAimedicalSys() throws Exception {
        assertEquals("com.aimedical", xpath(commonModuleDoc, "/project/parent/groupId"));
        assertEquals("aimedical-sys", xpath(commonModuleDoc, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(commonModuleDoc, "/project/parent/version"));
        assertEquals("../../pom.xml", xpath(commonModuleDoc, "/project/parent/relativePath"));
    }

    @Test
    void commonModuleShouldBePomPackaging() throws Exception {
        assertEquals("pom", xpath(commonModuleDoc, "/project/packaging"));
    }

    @Test
    void commonModuleShouldHaveCorrectArtifactId() throws Exception {
        assertEquals("common-module", xpath(commonModuleDoc, "/project/artifactId"));
    }

    @Test
    void commonModuleShouldContainBothSubmodules() throws Exception {
        assertTrue(exists(commonModuleDoc, "/project/modules/module[.='common-module-api']"));
        assertTrue(exists(commonModuleDoc, "/project/modules/module[.='common-module-impl']"));
    }

    @Test
    void aiParentShouldBeAimedicalSys() throws Exception {
        assertEquals("com.aimedical", xpath(aiDoc, "/project/parent/groupId"));
        assertEquals("aimedical-sys", xpath(aiDoc, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(aiDoc, "/project/parent/version"));
        assertEquals("../../pom.xml", xpath(aiDoc, "/project/parent/relativePath"));
    }

    @Test
    void aiShouldBePomPackaging() throws Exception {
        assertEquals("pom", xpath(aiDoc, "/project/packaging"));
    }

    @Test
    void aiShouldHaveCorrectArtifactId() throws Exception {
        assertEquals("ai", xpath(aiDoc, "/project/artifactId"));
    }

    @Test
    void aiShouldContainBothSubmodules() throws Exception {
        assertTrue(exists(aiDoc, "/project/modules/module[.='ai-api']"));
        assertTrue(exists(aiDoc, "/project/modules/module[.='ai-impl']"));
    }

    @Test
    void commonModuleShouldHaveNoDependencyManagement() throws Exception {
        assertFalse(exists(commonModuleDoc, "/project/dependencyManagement"));
    }

    @Test
    void commonModuleShouldHaveNoDependencies() throws Exception {
        assertFalse(exists(commonModuleDoc, "/project/dependencies"));
    }

    @Test
    void aiShouldHaveNoDependencyManagement() throws Exception {
        assertFalse(exists(aiDoc, "/project/dependencyManagement"));
    }

    @Test
    void aiShouldHaveNoDependencies() throws Exception {
        assertFalse(exists(aiDoc, "/project/dependencies"));
    }
}
