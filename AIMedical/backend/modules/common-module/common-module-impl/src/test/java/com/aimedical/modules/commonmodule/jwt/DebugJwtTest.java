package com.aimedical.modules.commonmodule.jwt;

public class DebugJwtTest {
    public static void main(String[] args) {
        JwtConfig config = new JwtConfig();
        config.setSecret("dGVzdA==");
        String expected = "至少32字节";
        System.out.println("Expected bytes: ");
        for (byte b : expected.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            System.out.printf("%02x ", b);
        }
        System.out.println();
        
        try {
            config.validate();
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            System.out.println("Actual message: " + msg);
            System.out.println("Actual bytes: ");
            for (byte b : msg.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
                System.out.printf("%02x ", b);
            }
            System.out.println();
            System.out.println("Contains check: " + msg.contains(expected));
            
            // Also check raw
            System.out.println("Default charset: " + java.nio.charset.Charset.defaultCharset());
            System.out.println("file.encoding: " + System.getProperty("file.encoding"));
        }
    }
}
