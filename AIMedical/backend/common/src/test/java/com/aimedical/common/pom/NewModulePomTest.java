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

class NewModulePomTest {

    private static Document consultationDoc;
    private static Document prescriptionDoc;
    private static Document medicalRecordDoc;
    private static XPath xpath;

    @BeforeAll
    static void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        consultationDoc = builder.parse(new File("../modules/consultation/pom.xml"));
        prescriptionDoc = builder.parse(new File("../modules/prescription/pom.xml"));
        medicalRecordDoc = builder.parse(new File("../modules/medical-record/pom.xml"));
        xpath = XPathFactory.newInstance().newXPath();
    }

    private String xpath(Document doc, String expr) throws Exception {
        return xpath.evaluate(expr, doc);
    }

    private boolean exists(Document doc, String expr) throws Exception {
        return (Boolean) xpath.evaluate(expr, doc, XPathConstants.BOOLEAN);
    }

    @Test
    void consultationParentShouldBeAimedicalSys() throws Exception {
        assertEquals("com.aimedical", xpath(consultationDoc, "/project/parent/groupId"));
        assertEquals("aimedical-sys", xpath(consultationDoc, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(consultationDoc, "/project/parent/version"));
        assertEquals("../../pom.xml", xpath(consultationDoc, "/project/parent/relativePath"));
    }

    @Test
    void consultationShouldHaveCorrectArtifactId() throws Exception {
        assertEquals("consultation", xpath(consultationDoc, "/project/artifactId"));
    }

    @Test
    void consultationShouldBeJarPackaging() throws Exception {
        assertEquals("jar", xpath(consultationDoc, "/project/packaging"));
    }

    @Test
    void consultationShouldNotDeclareVersionInDependencies() throws Exception {
        String base = "/project/dependencies/dependency";
        String count = xpath(consultationDoc, "count(" + base + "/version)");
        assertEquals("0", count, "All dependency versions must be inherited from parent pom");
    }

    @Test
    void consultationShouldContainRequiredDependencies() throws Exception {
        String base = "/project/dependencies/dependency";
        assertTrue(exists(consultationDoc, base + "[artifactId='lombok' and optional='true']"));
        assertTrue(exists(consultationDoc, base + "[artifactId='common' and groupId='com.aimedical']"));
        assertTrue(exists(consultationDoc, base + "[artifactId='common-module-api' and groupId='com.aimedical']"));
        assertTrue(exists(consultationDoc, base + "[artifactId='ai-api' and groupId='com.aimedical']"));
        assertTrue(exists(consultationDoc, base + "[artifactId='spring-boot-starter-web' and groupId='org.springframework.boot']"));
        assertTrue(exists(consultationDoc, base + "[artifactId='spring-boot-starter-data-jpa' and groupId='org.springframework.boot']"));
        assertTrue(exists(consultationDoc, base + "[artifactId='spring-boot-starter-validation' and groupId='org.springframework.boot']"));
        assertTrue(exists(consultationDoc, base + "[artifactId='spring-boot-starter-test' and groupId='org.springframework.boot' and scope='test']"));
    }

    @Test
    void consultationShouldHaveJacocoEnabled() throws Exception {
        assertEquals("false", xpath(consultationDoc, "/project/properties/jacoco.skip"));
        assertEquals("false", xpath(consultationDoc, "/project/properties/jacoco.skip.check"));
    }

    @Test
    void prescriptionParentShouldBeAimedicalSys() throws Exception {
        assertEquals("com.aimedical", xpath(prescriptionDoc, "/project/parent/groupId"));
        assertEquals("aimedical-sys", xpath(prescriptionDoc, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(prescriptionDoc, "/project/parent/version"));
        assertEquals("../../pom.xml", xpath(prescriptionDoc, "/project/parent/relativePath"));
    }

    @Test
    void prescriptionShouldHaveCorrectArtifactId() throws Exception {
        assertEquals("prescription", xpath(prescriptionDoc, "/project/artifactId"));
    }

    @Test
    void prescriptionShouldBeJarPackaging() throws Exception {
        assertEquals("jar", xpath(prescriptionDoc, "/project/packaging"));
    }

    @Test
    void prescriptionShouldNotDeclareVersionInDependencies() throws Exception {
        String base = "/project/dependencies/dependency";
        String count = xpath(prescriptionDoc, "count(" + base + "/version)");
        assertEquals("0", count, "All dependency versions must be inherited from parent pom");
    }

    @Test
    void prescriptionShouldContainRequiredDependencies() throws Exception {
        String base = "/project/dependencies/dependency";
        assertTrue(exists(prescriptionDoc, base + "[artifactId='lombok' and optional='true']"));
        assertTrue(exists(prescriptionDoc, base + "[artifactId='common' and groupId='com.aimedical']"));
        assertTrue(exists(prescriptionDoc, base + "[artifactId='common-module-api' and groupId='com.aimedical']"));
        assertTrue(exists(prescriptionDoc, base + "[artifactId='ai-api' and groupId='com.aimedical']"));
        assertTrue(exists(prescriptionDoc, base + "[artifactId='spring-boot-starter-web' and groupId='org.springframework.boot']"));
        assertTrue(exists(prescriptionDoc, base + "[artifactId='spring-boot-starter-data-jpa' and groupId='org.springframework.boot']"));
        assertTrue(exists(prescriptionDoc, base + "[artifactId='spring-boot-starter-validation' and groupId='org.springframework.boot']"));
        assertTrue(exists(prescriptionDoc, base + "[artifactId='spring-boot-starter-test' and groupId='org.springframework.boot' and scope='test']"));
    }

    @Test
    void prescriptionShouldHaveJacocoEnabled() throws Exception {
        assertEquals("false", xpath(prescriptionDoc, "/project/properties/jacoco.skip"));
        assertEquals("false", xpath(prescriptionDoc, "/project/properties/jacoco.skip.check"));
    }

    @Test
    void medicalRecordParentShouldBeAimedicalSys() throws Exception {
        assertEquals("com.aimedical", xpath(medicalRecordDoc, "/project/parent/groupId"));
        assertEquals("aimedical-sys", xpath(medicalRecordDoc, "/project/parent/artifactId"));
        assertEquals("0.0.1-SNAPSHOT", xpath(medicalRecordDoc, "/project/parent/version"));
        assertEquals("../../pom.xml", xpath(medicalRecordDoc, "/project/parent/relativePath"));
    }

    @Test
    void medicalRecordShouldHaveCorrectArtifactId() throws Exception {
        assertEquals("medical-record", xpath(medicalRecordDoc, "/project/artifactId"));
    }

    @Test
    void medicalRecordShouldBeJarPackaging() throws Exception {
        assertEquals("jar", xpath(medicalRecordDoc, "/project/packaging"));
    }

    @Test
    void medicalRecordShouldNotDeclareVersionInDependencies() throws Exception {
        String base = "/project/dependencies/dependency";
        String count = xpath(medicalRecordDoc, "count(" + base + "/version)");
        assertEquals("0", count, "All dependency versions must be inherited from parent pom");
    }

    @Test
    void medicalRecordShouldContainRequiredDependencies() throws Exception {
        String base = "/project/dependencies/dependency";
        assertTrue(exists(medicalRecordDoc, base + "[artifactId='lombok' and optional='true']"));
        assertTrue(exists(medicalRecordDoc, base + "[artifactId='common' and groupId='com.aimedical']"));
        assertTrue(exists(medicalRecordDoc, base + "[artifactId='common-module-api' and groupId='com.aimedical']"));
        assertTrue(exists(medicalRecordDoc, base + "[artifactId='ai-api' and groupId='com.aimedical']"));
        assertTrue(exists(medicalRecordDoc, base + "[artifactId='spring-boot-starter-web' and groupId='org.springframework.boot']"));
        assertTrue(exists(medicalRecordDoc, base + "[artifactId='spring-boot-starter-data-jpa' and groupId='org.springframework.boot']"));
        assertTrue(exists(medicalRecordDoc, base + "[artifactId='spring-boot-starter-validation' and groupId='org.springframework.boot']"));
        assertTrue(exists(medicalRecordDoc, base + "[artifactId='spring-boot-starter-test' and groupId='org.springframework.boot' and scope='test']"));
    }

    @Test
    void medicalRecordShouldHaveJacocoEnabled() throws Exception {
        assertEquals("false", xpath(medicalRecordDoc, "/project/properties/jacoco.skip"));
        assertEquals("false", xpath(medicalRecordDoc, "/project/properties/jacoco.skip.check"));
    }

    @Test
    void allModulesShouldHaveUniqueArtifactIds() throws Exception {
        String consultationId = xpath(consultationDoc, "/project/artifactId");
        String prescriptionId = xpath(prescriptionDoc, "/project/artifactId");
        String medicalRecordId = xpath(medicalRecordDoc, "/project/artifactId");
        assertNotEquals(consultationId, prescriptionId);
        assertNotEquals(consultationId, medicalRecordId);
        assertNotEquals(prescriptionId, medicalRecordId);
    }
}
