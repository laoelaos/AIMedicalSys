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

class MovedModulePomVerificationTest {

    private static Document rootPom;
    private static XPath xpath;

    @BeforeAll
    static void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        rootPom = builder.parse(new File("../pom.xml"));
        xpath = XPathFactory.newInstance().newXPath();
    }

    private boolean exists(String expr) throws Exception {
        return (Boolean) xpath.evaluate(expr, rootPom, XPathConstants.BOOLEAN);
    }

    @Test
    void rootPomShouldHaveExactlyThirteenModules() {
        assertEquals(13, rootPom.getDocumentElement()
            .getElementsByTagName("module").getLength());
    }

    @Test
    void rootPomModulesShouldContainAllExpectedEntries() throws Exception {
        assertTrue(exists("/project/modules/module[.='common']"));
        assertTrue(exists("/project/modules/module[.='modules/common-module']"));
        assertTrue(exists("/project/modules/module[.='modules/ai']"));
        assertTrue(exists("/project/modules/module[.='modules/patient']"));
        assertTrue(exists("/project/modules/module[.='modules/doctor']"));
        assertTrue(exists("/project/modules/module[.='modules/admin']"));
        assertTrue(exists("/project/modules/module[.='modules/registration']"));
        assertTrue(exists("/project/modules/module[.='modules/medical-order']"));
        assertTrue(exists("/project/modules/module[.='modules/consultation']"));
        assertTrue(exists("/project/modules/module[.='modules/prescription']"));
        assertTrue(exists("/project/modules/module[.='modules/medical-record']"));
        assertTrue(exists("/project/modules/module[.='application']"));
        assertTrue(exists("/project/modules/module[.='integration']"));
    }
}
