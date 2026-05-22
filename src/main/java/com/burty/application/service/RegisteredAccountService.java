package com.burty.application.service;

import com.burty.application.port.in.RegisteredAccountUseCase;
import com.burty.domain.entity.RegisteredAccountEntity;
import com.burty.domain.repository.RegisteredAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegisteredAccountService implements RegisteredAccountUseCase {

    private final RegisteredAccountRepository repository;

    public RegisteredAccountService(RegisteredAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public RegisteredAccountEntity register(String userId, String accountNo, String alias) {
        String pk = userId + "|" + accountNo;
        RegisteredAccountEntity entity = repository.findById(pk)
                .orElseGet(RegisteredAccountEntity::new);
        entity.setRegisteredId(pk);
        entity.setUserId(userId);
        entity.setAccountNo(accountNo);
        if (alias != null && !alias.isBlank()) {
            entity.setAlias(alias);
        }
        return repository.save(entity);
    }

    @Override
    @Transactional
    public boolean unregister(String userId, String accountNo) {
        String pk = userId + "|" + accountNo;
        if (!repository.existsById(pk)) {
            return false;
        }
        repository.deleteById(pk);
        return true;
    }

    @Override
    public List<View> list(String userId) {
        return repository.findByUserId(userId).stream()
                .map(e -> new View(e.getAccountNo(), e.getAlias()))
                .toList();
    }
}
