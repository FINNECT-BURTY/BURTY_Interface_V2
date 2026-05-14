package com.berty.config;

import com.berty.security.StandardLikeWebAuthnVerifier;
import com.berty.security.WebAuthn4jCompositeAssertionVerifier;
import com.berty.security.WebAuthnAssertionVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WebAuthnVerifierConfiguration {

    @Bean
    public StandardLikeWebAuthnVerifier standardLikeWebAuthnVerifier() {
        return new StandardLikeWebAuthnVerifier();
    }

    @Bean
    @Primary
    public WebAuthnAssertionVerifier webAuthnAssertionVerifier(StandardLikeWebAuthnVerifier standardLikeWebAuthnVerifier) {
        return new WebAuthn4jCompositeAssertionVerifier(standardLikeWebAuthnVerifier);
    }
}
