package com.burty.application.port.in;

import com.burty.domain.entity.RegisteredAccountEntity;

import java.util.List;

public interface RegisteredAccountUseCase {

    RegisteredAccountEntity register(String userId, String accountNo, String alias);

    boolean unregister(String userId, String accountNo);

    List<View> list(String userId);

    /** Decrypted view for sensitive read; controller maps to masked-only DTO unless caller is privileged. */
    record View(String accountNoHash, String accountNoMasked, String accountNoPlain, String alias) {}
}
