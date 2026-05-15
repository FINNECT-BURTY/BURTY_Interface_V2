package com.burty.config;

import com.burty.security.StandardLikeWebAuthnVerifier;
import com.burty.security.WebAuthn4jCompositeAssertionVerifier;
import com.burty.security.WebAuthnAssertionVerifier;
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
