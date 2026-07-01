package com.hybrid.framework.api;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SensitiveDataRedactorTest {

    @Test
    public void redactsTopLevelPasswordAndToken() {
        String input = """
                {
                  "email": "user@test.com",
                  "password": "S3cret!",
                  "token": "eyJhbGciOiJIUzI1NiJ9"
                }
                """;

        String redacted = SensitiveDataRedactor.redact(input);

        Assert.assertTrue(redacted.contains("\"password\": \"***\""));
        Assert.assertTrue(redacted.contains("user@test.com"));
        Assert.assertFalse(redacted.contains("S3cret!"));
        Assert.assertFalse(redacted.contains("eyJhbGciOiJIUzI1NiJ9"));
        System.out.println(redacted);
    }

    @Test
    public void redactsNestedPassword() {
        String input = """
                {
                  "user": {
                    "email": "user@test.com",
                    "password": "fixscal"
                  }
                }
                """;

        String redacted = SensitiveDataRedactor.redact(input);

        Assert.assertTrue(redacted.contains("\"password\": \"***\""));
        Assert.assertFalse(redacted.contains("fixscal"));
        System.out.println(redacted);
    }

    @Test
    public void redactsAuthorizationHeaderLine() {
        String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.payload.signature";

        String redacted = SensitiveDataRedactor.redact(input);

        Assert.assertEquals(redacted, "Authorization: Bearer ***");
        System.out.println(redacted);
    }
}
