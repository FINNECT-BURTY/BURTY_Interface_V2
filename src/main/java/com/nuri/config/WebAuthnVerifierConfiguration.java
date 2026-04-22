package com.nuri.config;

import com.nuri.security.StandardLikeWebAuthnVerifier;
import com.nuri.security.WebAuthn4jCompositeAssertionVerifier;
import com.nuri.security.WebAuthnAssertionVerifier;
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
