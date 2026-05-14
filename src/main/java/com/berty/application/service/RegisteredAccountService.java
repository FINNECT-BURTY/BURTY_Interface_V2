package com.berty.application.service;

import com.berty.application.port.in.RegisteredAccountUseCase;
import com.berty.domain.entity.RegisteredAccountEntity;
import com.berty.domain.repository.RegisteredAccountRepository;
import com.berty.util.AccountNumberCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegisteredAccountService implements RegisteredAccountUseCase {

    private final RegisteredAccountRepository repository;
    private final AccountNumberCipher accountNumberCipher;

    public RegisteredAccountService(RegisteredAccountRepository repository,
                                    AccountNumberCipher accountNumberCipher) {
        this.repository = repository;
        this.accountNumberCipher = accountNumberCipher;
    }

    @Override
    @Transactional
    public RegisteredAccountEntity register(String userId, String accountNo, String alias) {
        AccountNumberCipher.Encoded enc = accountNumberCipher.encode(accountNo);
        RegisteredAccountEntity entity = repository.findById(userId + "|" + enc.hash())
                .orElseGet(RegisteredAccountEntity::new);
        entity.setUserId(userId);
        entity.setAccountNoHash(enc.hash());
        entity.setAccountNoEncrypted(enc.encrypted());
        entity.setAccountNoMasked(enc.masked());
        if (alias != null && !alias.isBlank()) entity.setAlias(alias);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public boolean unregister(String userId, String accountNo) {
        String hash = accountNumberCipher.hash(accountNo);
        String pk = userId + "|" + hash;
        if (!repository.existsById(pk)) return false;
        repository.deleteById(pk);
        return true;
    }

    @Override
    public List<View> list(String userId) {
        return repository.findByUserId(userId).stream()
                .map(e -> new View(
                        e.getAccountNoHash(),
                        e.getAccountNoMasked(),
                        accountNumberCipher.decrypt(e.getAccountNoEncrypted()),
                        e.getAlias()
                ))
                .toList();
    }
}
