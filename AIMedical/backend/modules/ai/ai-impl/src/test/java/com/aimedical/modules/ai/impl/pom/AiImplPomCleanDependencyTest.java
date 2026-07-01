package com.aimedical.modules.ai.impl.pom;

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

class AiImplPomCleanDependencyTest {

    private static Document doc;
    private static XPath xpath;

    @BeforeAll
    static void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        String pomPath = System.getProperty("ai.impl.pom.path", "pom.xml");
        doc = builder.parse(new File(pomPath));
        xpath = XPathFactory.newInstance().newXPath();
    }

    private boolean exists(String expr) throws Exception {
        return (Boolean) xpath.evaluate(expr, doc, XPathConstants.BOOLEAN);
    }

    @Test
    void shouldNotContainRedundantCommonDependency() throws Exception {
        assertFalse(exists("/project/dependencies/dependency[groupId='com.aimedical' and artifactId='common']"));
    }

    @Test
    void shouldContainAiApiDependency() throws Exception {
        assertTrue(exists("/project/dependencies/dependency[groupId='com.aimedical' and artifactId='ai-api']"));
    }

    @Test
    void shouldContainSpringBootStarter() throws Exception {
        assertTrue(exists("/project/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter']"));
    }

    @Test
    void shouldContainSpringBootStarterWeb() throws Exception {
        assertTrue(exists("/project/dependencies/dependency[groupId='org.springframework.boot' and artifactId='spring-boot-starter-web']"));
    }

    @Test
    void shouldContainTestStarterWithTestScope() throws Exception {
        assertTrue(exists("/project/dependencies/dependency[artifactId='spring-boot-starter-test' and scope='test']"));
    }

    @Test
    void totalDependenciesCountShouldBeFour() throws Exception {
        Double count = (Double) xpath.evaluate("count(/project/dependencies/dependency)", doc, XPathConstants.NUMBER);
        assertEquals(4, count.intValue());
    }
}
