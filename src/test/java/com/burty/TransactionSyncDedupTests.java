package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.application.port.in.transaction.TransactionSyncUseCase;
import com.burty.application.port.out.bank.OpenBankingPort;
import com.burty.domain.transaction.repository.TransactionRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.support.IntegrationTestBase;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 거래 동기화의 중복 판정.
 *
 * <p>기관이 거래 ID 를 주지 않으면 대체 키를 만들어 중복을 거른다. 예전 대체 키는 {@code 계좌-날짜-금액} 이었다. <b>같은 날 같은 금액의 서로 다른 거래가
 * 같은 키가 되어 두 번째가 조용히 사라졌다.</b>
 *
 * <p>4,500원짜리 커피를 하루에 두 번 마시면 한 건만 남는다. 자산 관리 앱에서 거래 누락은 예산·지출 집계를 그대로 틀리게 만든다.
 */
@SpringBootTest
class TransactionSyncDedupTests extends IntegrationTestBase {

  private static final String FINTECH_NUM = "199200001234567890123456";

  @Autowired private TransactionSyncUseCase transactionSync;
  @Autowired private TransactionRepository transactionRepository;
  @Autowired private UserRepository userRepository;

  @MockitoBean private OpenBankingPort openBankingPort;

  private String userId;

  @BeforeEach
  void setUp() {
    UserEntity user = new UserEntity();
    String unique = UUID.randomUUID().toString().replace("-", "");
    user.setCiHash(("c" + unique + unique).substring(0, 64));
    user.setCi("ci-" + unique);
    user.setPhoneHash(("p" + unique + unique).substring(0, 64));
    user.setPhone("010-0000-0000");
    user.setStatus(UserEntity.UserStatus.ACTIVE);
    user.setFailedLoginCount(0);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userId = String.valueOf(userRepository.save(user).getUserId());
  }

  @Test
  @DisplayName("거래 ID 가 없어도 같은 날 같은 금액의 다른 거래를 모두 저장한다")
  void keepsDistinctTransactionsWithoutBankId() {
    stub(
        tx("2026-08-31", 4500, "스타벅스"),
        tx("2026-08-31", 4500, "투썸플레이스"),
        tx("2026-08-31", 4500, "이디야"));

    int saved = transactionSync.syncFromOpenBanking(userId, FINTECH_NUM);

    assertEquals(3, saved, "같은 날 같은 금액이라는 이유로 거래가 사라졌다");
    assertEquals(3, transactionRepository.countByUserId(Long.parseLong(userId)));
  }

  @Test
  @DisplayName("가맹점까지 같아도 건수만큼 저장한다")
  void keepsRepeatedIdenticalTransactions() {
    // 같은 가게에서 같은 금액을 두 번 결제하는 것은 흔하다.
    stub(tx("2026-08-31", 4500, "스타벅스"), tx("2026-08-31", 4500, "스타벅스"));

    assertEquals(2, transactionSync.syncFromOpenBanking(userId, FINTECH_NUM));
  }

  @Test
  @DisplayName("같은 응답을 다시 동기화해도 중복 저장하지 않는다")
  void reSyncDoesNotDuplicate() {
    stub(
        tx("2026-08-31", 4500, "스타벅스"),
        tx("2026-08-31", 4500, "스타벅스"),
        tx("2026-08-30", 12000, "이마트"));

    assertEquals(3, transactionSync.syncFromOpenBanking(userId, FINTECH_NUM));
    // 대체 키가 배치마다 달라지면 재동기화가 거래를 복제한다.
    assertEquals(0, transactionSync.syncFromOpenBanking(userId, FINTECH_NUM), "재동기화가 거래를 복제했다");
    assertEquals(3, transactionRepository.countByUserId(Long.parseLong(userId)));
  }

  @Test
  @DisplayName("기관이 준 거래 ID 가 있으면 그것을 그대로 쓴다")
  void prefersBankProvidedId() {
    Map<String, Object> withId = new java.util.HashMap<>(tx("2026-08-31", 4500, "스타벅스"));
    withId.put("id", "BANK-TX-1");
    stub(withId);

    assertEquals(1, transactionSync.syncFromOpenBanking(userId, FINTECH_NUM));
    assertTrue(
        transactionRepository
            .findByUserIdAndExternalTxId(Long.parseLong(userId), "BANK-TX-1")
            .isPresent(),
        "기관이 준 거래 ID 를 쓰지 않았다");
  }

  @SafeVarargs
  private void stub(Map<String, Object>... transactions) {
    org.mockito.Mockito.when(openBankingPort.getTransactions(userId, FINTECH_NUM))
        .thenReturn(Map.of("transactions", new ArrayList<>(List.of(transactions))));
  }

  private static Map<String, Object> tx(String date, long amount, String merchant) {
    return Map.of(
        "date", date,
        "amount", amount,
        "type", "WITHDRAWAL",
        "merchant", merchant,
        "memo", "");
  }
}
