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

class ParentPomModuleRegistrationTest {

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

    private boolean exists(String expr) throws Exception {
        return (Boolean) xpath.evaluate(expr, doc, XPathConstants.BOOLEAN);
    }

    @Test
    void parentPomShouldRegisterConsultationModule() throws Exception {
        assertTrue(exists("/project/modules/module[.='modules/consultation']"));
    }

    @Test
    void parentPomShouldRegisterPrescriptionModule() throws Exception {
        assertTrue(exists("/project/modules/module[.='modules/prescription']"));
    }

    @Test
    void parentPomShouldRegisterMedicalRecordModule() throws Exception {
        assertTrue(exists("/project/modules/module[.='modules/medical-record']"));
    }

    @Test
    void newModulesShouldAppearAfterAdmin() throws Exception {
        String adminPosition = xpath.evaluate("count(/project/modules/module[.='modules/admin']/preceding-sibling::*)", doc);
        String consultationPosition = xpath.evaluate("count(/project/modules/module[.='modules/consultation']/preceding-sibling::*)", doc);
        int adminIdx = Integer.parseInt(adminPosition);
        int consultationIdx = Integer.parseInt(consultationPosition);
        assertTrue(consultationIdx > adminIdx, "consultation module should appear after admin module");
    }

    @Test
    void newModulesShouldAppearBeforeApplication() throws Exception {
        String applicationPosition = xpath.evaluate("count(/project/modules/module[.='application']/preceding-sibling::*)", doc);
        String consultationPosition = xpath.evaluate("count(/project/modules/module[.='modules/consultation']/preceding-sibling::*)", doc);
        int applicationIdx = Integer.parseInt(applicationPosition);
        int consultationIdx = Integer.parseInt(consultationPosition);
        assertTrue(consultationIdx < applicationIdx, "consultation module should appear before application module");
    }
}
