package com.berty;

import com.berty.config.JwtProperties;
import com.berty.security.RiskLevel;
import com.berty.security.RiskProofService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RiskProofServiceTests {

    @Test
    void issuedProof_isVerifiableForSameUserAndLevel() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-1234567890");
        RiskProofService service = new RiskProofService(properties);

        String token = service.issue("user-a", RiskLevel.LEVEL_2);

        Assertions.assertTrue(service.verify(token, "user-a", RiskLevel.LEVEL_2));
        Assertions.assertFalse(service.verify(token, "user-b", RiskLevel.LEVEL_2));
        Assertions.assertFalse(service.verify(token, "user-a", RiskLevel.LEVEL_3));
    }
}
