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

class CommonPomTest {

    private static Document doc;
    private static XPath xpath;

    @BeforeAll
    static void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.parse(new File("pom.xml"));
        xpath = XPathFactory.newInstance().newXPath();
    }

    private boolean exists(String expr) throws Exception {
        return (Boolean) xpath.evaluate(expr, doc, XPathConstants.BOOLEAN);
    }

    private String xpath(String expr) throws Exception {
        return xpath.evaluate(expr, doc);
    }

    @Test
    void shouldContainWebStarterAsOptional() throws Exception {
        assertTrue(exists("/project/dependencies/dependency[artifactId='spring-boot-starter-web' and optional='true']"));
    }

    @Test
    void shouldContainDataJpaStarterAsOptional() throws Exception {
        assertTrue(exists("/project/dependencies/dependency[artifactId='spring-boot-starter-data-jpa' and optional='true']"));
    }

    @Test
    void shouldContainTestStarterWithTestScope() throws Exception {
        assertEquals("test", xpath("/project/dependencies/dependency[artifactId='spring-boot-starter-test']/scope"));
    }

    @Test
    void dependencyCountShouldBeExactlySix() throws Exception {
        assertEquals(6, doc.getDocumentElement()
            .getElementsByTagName("dependency").getLength());
    }
}
