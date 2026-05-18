package com.burty.application.port.in;

import com.burty.domain.entity.RegisteredAccountEntity;

import java.util.List;

public interface RegisteredAccountUseCase {

    RegisteredAccountEntity register(String userId, String accountNo, String alias);

    boolean unregister(String userId, String accountNo);

    List<View> list(String userId);

    record View(String accountNo, String alias) {}
}
