package com.burty.domain.asset.repository;

import com.burty.domain.asset.entity.AccountSnapshotEntity;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSnapshotRepository extends JpaRepository<AccountSnapshotEntity, Long> {

  /** 계좌 파기 시 함께 지운다. 스냅샷은 계좌에 FK 로 매달려 있어 먼저 지우지 않으면 제약 위반이 난다. */
  long deleteByAccount_AccountIdIn(Collection<Long> accountIds);

  long countByAccount_AccountIdIn(Collection<Long> accountIds);
}
