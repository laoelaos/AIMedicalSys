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

class MovedModulePomTest {

    private static Document rootPom;
    private static Document commonModuleApiPom;
    private static Document commonModuleImplPom;
    private static Document aiApiPom;
    private static Document aiImplPom;
    private static Document patientPom;
    private static Document doctorPom;
    private static Document adminPom;
    private static Document applicationPom;
    private static Document commonPom;
    private static Document integrationPom;
    private static XPath xpath;

    @BeforeAll
    static void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        rootPom = builder.parse(new File("../pom.xml"));
        commonModuleApiPom = builder.parse(new File("../modules/common-module/common-module-api/pom.xml"));
        commonModuleImplPom = builder.parse(new File("../modules/common-module/common-module-impl/pom.xml"));
        aiApiPom = builder.parse(new File("../modules/ai/ai-api/pom.xml"));
        aiImplPom = builder.parse(new File("../modules/ai/ai-impl/pom.xml"));
        patientPom = builder.parse(new File("../modules/patient/pom.xml"));
        doctorPom = builder.parse(new File("../modules/doctor/pom.xml"));
        adminPom = builder.parse(new File("../modules/admin/pom.xml"));
        applicationPom = builder.parse(new File("../application/pom.xml"));
        commonPom = builder.parse(new File("pom.xml"));
        integrationPom = builder.parse(new File("../integration/pom.xml"));
        xpath = XPathFactory.newInstance().newXPath();
    }

    private boolean exists(Document doc, String expr) throws Exception {
        return (Boolean) xpath.evaluate(expr, doc, XPathConstants.BOOLEAN);
    }

    private String xpath(Document doc, String expr) throws Exception {
        return xpath.evaluate(expr, doc);
    }

    @Test
    void commonModuleApiParentShouldBeCommonModule() throws Exception {
        assertEquals("com.aimedical", xpath(commonModuleApiPom, "/project/parent/groupId"));
        assertEquals("common-module", xpath(commonModuleApiPom, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(commonModuleApiPom, "/project/parent/version"));
        assertEquals("../pom.xml", xpath(commonModuleApiPom, "/project/parent/relativePath"));
    }

    @Test
    void commonModuleImplParentShouldBeCommonModule() throws Exception {
        assertEquals("com.aimedical", xpath(commonModuleImplPom, "/project/parent/groupId"));
        assertEquals("common-module", xpath(commonModuleImplPom, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(commonModuleImplPom, "/project/parent/version"));
        assertEquals("../pom.xml", xpath(commonModuleImplPom, "/project/parent/relativePath"));
    }

    @Test
    void aiApiParentShouldBeAi() throws Exception {
        assertEquals("com.aimedical", xpath(aiApiPom, "/project/parent/groupId"));
        assertEquals("ai", xpath(aiApiPom, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(aiApiPom, "/project/parent/version"));
        assertEquals("../pom.xml", xpath(aiApiPom, "/project/parent/relativePath"));
    }

    @Test
    void aiImplParentShouldBeAi() throws Exception {
        assertEquals("com.aimedical", xpath(aiImplPom, "/project/parent/groupId"));
        assertEquals("ai", xpath(aiImplPom, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(aiImplPom, "/project/parent/version"));
        assertEquals("../pom.xml", xpath(aiImplPom, "/project/parent/relativePath"));
    }

    @Test
    void patientParentRelativePathShouldBeCorrect() throws Exception {
        assertEquals("com.aimedical", xpath(patientPom, "/project/parent/groupId"));
        assertEquals("aimedical-sys", xpath(patientPom, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(patientPom, "/project/parent/version"));
        assertEquals("../../pom.xml", xpath(patientPom, "/project/parent/relativePath"));
    }

    @Test
    void doctorParentRelativePathShouldBeCorrect() throws Exception {
        assertEquals("com.aimedical", xpath(doctorPom, "/project/parent/groupId"));
        assertEquals("aimedical-sys", xpath(doctorPom, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(doctorPom, "/project/parent/version"));
        assertEquals("../../pom.xml", xpath(doctorPom, "/project/parent/relativePath"));
    }

    @Test
    void adminParentRelativePathShouldBeCorrect() throws Exception {
        assertEquals("com.aimedical", xpath(adminPom, "/project/parent/groupId"));
        assertEquals("aimedical-sys", xpath(adminPom, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(adminPom, "/project/parent/version"));
        assertEquals("../../pom.xml", xpath(adminPom, "/project/parent/relativePath"));
    }

    @Test
    void unmovedModulesRelativePathShouldRemainUnchanged() throws Exception {
        assertEquals("com.aimedical", xpath(applicationPom, "/project/parent/groupId"));
        assertEquals("aimedical-sys", xpath(applicationPom, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(applicationPom, "/project/parent/version"));
        assertEquals("../pom.xml", xpath(applicationPom, "/project/parent/relativePath"));
        assertEquals("com.aimedical", xpath(commonPom, "/project/parent/groupId"));
        assertEquals("aimedical-sys", xpath(commonPom, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(commonPom, "/project/parent/version"));
        assertEquals("../pom.xml", xpath(commonPom, "/project/parent/relativePath"));
        assertEquals("com.aimedical", xpath(integrationPom, "/project/parent/groupId"));
        assertEquals("aimedical-sys", xpath(integrationPom, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(integrationPom, "/project/parent/version"));
        assertEquals("../pom.xml", xpath(integrationPom, "/project/parent/relativePath"));
    }

    @Test
    void unmovedModulesParentGroupIdShouldBeComAimedical() throws Exception {
        assertEquals("com.aimedical", xpath(applicationPom, "/project/parent/groupId"));
        assertEquals("com.aimedical", xpath(commonPom, "/project/parent/groupId"));
        assertEquals("com.aimedical", xpath(integrationPom, "/project/parent/groupId"));
    }

    @Test
    void rootPomModulesShouldUseLayeredPaths() throws Exception {
        assertTrue(exists(rootPom, "/project/modules/module[.='common']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='modules/common-module']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='modules/ai']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='modules/patient']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='modules/doctor']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='modules/admin']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='modules/registration']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='modules/medical-order']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='modules/consultation']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='modules/prescription']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='modules/medical-record']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='application']"));
        assertTrue(exists(rootPom, "/project/modules/module[.='integration']"));
    }

    @Test
    void rootPomShouldHaveExactlyThirteenModules() throws Exception {
        assertEquals(13, rootPom.getDocumentElement()
            .getElementsByTagName("module").getLength());
    }
}
