package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burty.adapter.out.http.ResilientHttpExecutor;
import com.burty.adapter.out.mydata.MyDataBankApiAdapter;
import com.burty.adapter.out.mydata.standard.StandardBankFixtures;
import com.burty.application.port.out.mydata.MyDataApiException;
import com.burty.application.port.out.mydata.MyDataBankPort;
import com.burty.application.port.out.mydata.MyDataOAuthPort;
import com.burty.application.port.out.mydata.MyDataUnauthorizedException;
import com.burty.application.service.mydata.LinkedInstitutionPersistenceService;
import com.burty.application.service.mydata.MyDataAssetAggregationService;
import com.burty.application.service.mydata.MyDataTokenHydrationService;
import com.burty.config.MyDataProperties;
import com.burty.domain.asset.model.AssetSnapshot;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity.LinkStatus;
import com.burty.domain.mydata.model.BankAccount;
import com.burty.domain.mydata.model.BankTransaction;
import com.burty.domain.mydata.model.BankTransaction.Direction;
import com.burty.domain.mydata.repository.LinkedInstitutionRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

/**
 * 연결된 기관 합산의 규칙을 못 박는다.
 *
 * <p>예전에는 기관 하나로 고정해 조회했고, 모의 모드에서는 연결 여부와 무관하게 모든 사용자에게 같은 값을 돌려줬다.
 */
class MyDataAssetAggregationTests {

  private static final String USER = "7";
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
  private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);
  private static final BankAccount CONSENTED = new BankAccount("1", "1", "입출금", "1001", "01", true);

  private LinkedInstitutionRepository repository;
  private LinkedInstitutionPersistenceService persistence;
  private MyDataTokenHydrationService hydration;
  private MyDataOAuthPort oauthPort;
  private MyDataBankPort bankPort;

  @BeforeEach
  void setUp() {
    repository = mock(LinkedInstitutionRepository.class);
    persistence = mock(LinkedInstitutionPersistenceService.class);
    hydration = mock(MyDataTokenHydrationService.class);
    oauthPort = mock(MyDataOAuthPort.class);
    bankPort = mock(MyDataBankPort.class);
    when(oauthPort.findAccessToken(anyString())).thenAnswer(inv -> "token-" + inv.getArgument(0));
  }

  private MyDataAssetAggregationService service(MyDataBankPort port) {
    Clock clock = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);
    return new MyDataAssetAggregationService(
        repository, persistence, hydration, oauthPort, port, clock);
  }

  private void links(LinkedInstitutionEntity... entities) {
    when(repository.findByUser_UserId(anyLong())).thenReturn(List.of(entities));
  }

  private static LinkedInstitutionEntity link(String code, LinkStatus status) {
    LinkedInstitutionEntity entity = new LinkedInstitutionEntity();
    entity.setInstitutionCode(code);
    entity.setStatus(status);
    return entity;
  }

  private void institution(String code, long balance, BankTransaction... transactions) {
    when(bankPort.listAccounts(eq(code), anyString())).thenReturn(List.of(CONSENTED));
    when(bankPort.depositBalance(eq(code), anyString(), any()))
        .thenReturn(BigDecimal.valueOf(balance));
    when(bankPort.depositTransactions(eq(code), anyString(), any(), any(), any()))
        .thenReturn(List.of(transactions));
  }

  private static BankTransaction tx(Direction direction, LocalDate day, long amount) {
    return new BankTransaction(
        day.atTime(12, 0), "n", direction, "c", BigDecimal.valueOf(amount), null);
  }

  @Test
  @DisplayName("연결된 ACTIVE 기관 전체의 잔액과 이번 달 출금을 합산한다")
  void sumsAllActiveInstitutions() {
    links(link("KB", LinkStatus.ACTIVE), link("SHINHAN", LinkStatus.ACTIVE));
    institution(
        "KB",
        1_000_000,
        tx(Direction.WITHDRAWAL, TODAY.withDayOfMonth(3), 50_000),
        tx(Direction.WITHDRAWAL, TODAY.minusMonths(1), 70_000)); // 지난달 출금은 이번 달 지출이 아니다
    institution("SHINHAN", 2_500_000, tx(Direction.WITHDRAWAL, TODAY.withDayOfMonth(10), 30_000));

    AssetSnapshot snapshot = service(bankPort).fetchAssetSnapshot(USER);

    assertEquals(3_500_000, snapshot.totalAsset());
    assertEquals(80_000, snapshot.monthlySpend());
    assertEquals(2, snapshot.linkedInstitutionCount());
    assertFalse(snapshot.partial());
  }

  @Test
  @DisplayName("철회된 연동과 오픈뱅킹은 합산하지 않는다")
  void excludesRevokedAndOpenBanking() {
    links(
        link("KB", LinkStatus.ACTIVE),
        link("SHINHAN", LinkStatus.REVOKED),
        link(LinkedInstitutionPersistenceService.OPEN_BANKING_CODE, LinkStatus.ACTIVE));
    institution("KB", 1_000_000);

    AssetSnapshot snapshot = service(bankPort).fetchAssetSnapshot(USER);

    assertEquals(1, snapshot.linkedInstitutionCount());
    verify(bankPort, never()).listAccounts(eq("SHINHAN"), anyString());
    verify(bankPort, never())
        .listAccounts(eq(LinkedInstitutionPersistenceService.OPEN_BANKING_CODE), anyString());
  }

  @Test
  @DisplayName("연결이 없으면 0원이 아니라 미연동이다")
  void noLinksIsNotLinked() {
    links();

    AssetSnapshot snapshot = service(bankPort).fetchAssetSnapshot(USER);

    assertFalse(snapshot.linked(), "연결이 없는데 연동된 것으로 보인다");
    verify(bankPort, never()).listAccounts(anyString(), anyString());
  }

  @Test
  @DisplayName("사용자 행이 없는 세션은 미연동이다")
  void nonNumericUserIsNotLinked() {
    // 데모 세션(demo-user)이 여기 걸린다. 숫자로 바꾸다 실패해 화면 전체가 오류가 나면 안 된다.
    assertFalse(service(bankPort).fetchAssetSnapshot("demo-user").linked());
    verify(repository, never()).findByUser_UserId(anyLong());
  }

  @Test
  @DisplayName("한 기관이 실패해도 나머지는 합산하고 실패 수를 싣는다")
  void oneFailureIsPartial() {
    links(link("KB", LinkStatus.ACTIVE), link("SHINHAN", LinkStatus.ACTIVE));
    when(bankPort.listAccounts(eq("KB"), anyString()))
        .thenThrow(new MyDataApiException("50001", "장애"));
    institution("SHINHAN", 2_500_000);

    AssetSnapshot snapshot = service(bankPort).fetchAssetSnapshot(USER);

    assertEquals(2_500_000, snapshot.totalAsset());
    assertEquals(1, snapshot.failedInstitutionCount());
    assertTrue(snapshot.partial(), "일부만 더한 합계를 전체처럼 보여준다");
  }

  @Test
  @DisplayName("정보주체가 동의하지 않은 계좌는 조회하지 않는다")
  void skipsNonConsentedAccounts() {
    links(link("KB", LinkStatus.ACTIVE));
    when(bankPort.listAccounts(eq("KB"), anyString()))
        .thenReturn(List.of(new BankAccount("9", "1", "비동의", "1001", "01", false)));

    service(bankPort).fetchAssetSnapshot(USER);

    verify(bankPort, never()).depositBalance(anyString(), anyString(), any());
    verify(bankPort, never()).depositTransactions(anyString(), anyString(), any(), any(), any());
  }

  @Test
  @DisplayName("토큰이 거절되면 한 번 재발급해 다시 부르고, 재발급한 토큰을 원본에도 남긴다")
  void refreshesOnceAndPersists() {
    // 런타임 저장소에만 두면 다음 조회의 hydrate() 가 원본의 옛 토큰으로 되돌려 놓는다.
    links(link("KB", LinkStatus.ACTIVE));
    String scopeKey = MyDataOAuthPort.scopeKey(USER, "KB");
    when(oauthPort.findAccessToken(scopeKey)).thenReturn("old");
    when(oauthPort.refreshAccessToken(scopeKey)).thenReturn("new");
    when(bankPort.listAccounts("KB", "old")).thenThrow(new MyDataUnauthorizedException("만료"));
    when(bankPort.listAccounts("KB", "new")).thenReturn(List.of(CONSENTED));
    when(bankPort.depositBalance(eq("KB"), eq("new"), any())).thenReturn(BigDecimal.valueOf(100));
    when(bankPort.depositTransactions(eq("KB"), eq("new"), any(), any(), any()))
        .thenReturn(List.of());

    AssetSnapshot snapshot = service(bankPort).fetchAssetSnapshot(USER);

    assertEquals(100, snapshot.totalAsset());
    verify(oauthPort, times(1)).refreshAccessToken(scopeKey);
    verify(persistence)
        .updateRefreshedTokens(
            eq(USER), eq("KB"), argThat(bundle -> "new".equals(bundle.accessToken())));
  }

  @Test
  @DisplayName("재발급 뒤에도 거절되면 그 기관만 실패로 센다")
  void refreshStillRejectedCountsAsFailure() {
    links(link("KB", LinkStatus.ACTIVE));
    String scopeKey = MyDataOAuthPort.scopeKey(USER, "KB");
    when(oauthPort.refreshAccessToken(scopeKey)).thenReturn("new");
    when(bankPort.listAccounts(eq("KB"), anyString()))
        .thenThrow(new MyDataUnauthorizedException("거절"));

    AssetSnapshot snapshot = service(bankPort).fetchAssetSnapshot(USER);

    assertEquals(1, snapshot.failedInstitutionCount());
    verify(oauthPort, times(1)).refreshAccessToken(scopeKey); // 무한히 재발급하지 않는다
  }

  @Test
  @DisplayName("소득 변동성은 직전 3개월 월별 입금의 변동계수다 — 끊긴 달도 0 으로 넣는다")
  void incomeVariability() {
    YearMonth m1 = YearMonth.of(2026, 6);
    YearMonth m2 = YearMonth.of(2026, 7);
    YearMonth m3 = YearMonth.of(2026, 8);
    List<YearMonth> months = List.of(m1, m2, m3);
    BigDecimal salary = BigDecimal.valueOf(3_000_000);

    assertEquals(
        0.0,
        MyDataAssetAggregationService.variabilityPercent(
            Map.of(m1, salary, m2, salary, m3, salary), months));
    // 한 달이 끊기면 변동이 커야 한다. 빈 달을 빼고 계산하면 0 으로 나온다.
    assertEquals(
        70.7,
        MyDataAssetAggregationService.variabilityPercent(Map.of(m1, salary, m2, salary), months));
    assertEquals(0.0, MyDataAssetAggregationService.variabilityPercent(Map.of(), months));
  }

  @Test
  @DisplayName("모의 정보제공자로 두 기관을 연결하면 기관별로 다른 값이 합산된다")
  void stubProviderSumsPerInstitution() {
    // 고정값 하나를 돌려주던 예전 모의로는 합산이 실제로 일어나는지 볼 수 없었다.
    MyDataProperties stub = new MyDataProperties();
    stub.setStubMode(true);
    MyDataBankPort stubPort =
        new MyDataBankApiAdapter(
            new RestTemplate(),
            stub,
            new ResilientHttpExecutor(CircuitBreakerRegistry.ofDefaults()));
    links(link("KB", LinkStatus.ACTIVE), link("SHINHAN", LinkStatus.ACTIVE));

    AssetSnapshot snapshot = service(stubPort).fetchAssetSnapshot(USER);

    long expected = 0;
    for (String code : List.of("KB", "SHINHAN")) {
      expected += StandardBankFixtures.balanceOf(code, StandardBankFixtures.checkingAccount(code));
      expected += StandardBankFixtures.balanceOf(code, StandardBankFixtures.savingsAccount(code));
    }
    assertEquals(expected, (long) snapshot.totalAsset());
    assertTrue(snapshot.monthlySpend() > 0, "모의 거래내역에서 이번 달 지출이 잡히지 않았다");
    assertEquals(2, snapshot.linkedInstitutionCount());
  }

  @Test
  @DisplayName("연결한 내 계좌 사이의 이체는 지출에도 소득에도 넣지 않는다")
  void transferBetweenLinkedAccountsIsNeitherSpendNorIncome() {
    // KB 입출금에서 신한 적금으로 매달 30만 원을 옮긴다. 한쪽에서는 출금, 다른 쪽에서는 입금으로
    // 보이지만 쓴 돈도 번 돈도 아니다.
    links(link("KB", LinkStatus.ACTIVE), link("SHINHAN", LinkStatus.ACTIVE));
    LocalDate thisMonth = TODAY.withDayOfMonth(10);
    LocalDate lastMonth = TODAY.minusMonths(1).withDayOfMonth(10);
    institution(
        "KB",
        1_000_000,
        tx(Direction.WITHDRAWAL, thisMonth, 300_000),
        tx(Direction.WITHDRAWAL, lastMonth, 300_000),
        tx(Direction.WITHDRAWAL, TODAY.withDayOfMonth(3), 50_000));
    institution(
        "SHINHAN",
        2_000_000,
        tx(Direction.DEPOSIT, thisMonth, 300_000),
        tx(Direction.DEPOSIT, lastMonth, 300_000));

    AssetSnapshot snapshot = service(bankPort).fetchAssetSnapshot(USER);

    assertEquals(50_000, snapshot.monthlySpend());
    // 옮긴 돈을 소득으로 세면 8월에만 30만 원 소득이 있는 것처럼 보여 변동성이 141.4% 로 나온다.
    assertEquals(0.0, snapshot.volatilityPercent());
  }

  @Test
  @DisplayName("같은 기관 안의 계좌 사이 이체도 뺀다 — 입출금에서 적금으로")
  void transferBetweenAccountsOfOneInstitution() {
    links(link("KB", LinkStatus.ACTIVE));
    BankAccount checking = new BankAccount("100", "1", "입출금", "1001", "01", true);
    BankAccount savings = new BankAccount("200", "1", "적금", "1003", "01", true);
    LocalDate day = TODAY.withDayOfMonth(10);
    when(bankPort.listAccounts(eq("KB"), anyString())).thenReturn(List.of(checking, savings));
    when(bankPort.depositBalance(eq("KB"), anyString(), any())).thenReturn(BigDecimal.ZERO);
    when(bankPort.depositTransactions(eq("KB"), anyString(), eq(checking), any(), any()))
        .thenReturn(
            List.of(tx(Direction.WITHDRAWAL, day, 300_000), tx(Direction.WITHDRAWAL, day, 20_000)));
    when(bankPort.depositTransactions(eq("KB"), anyString(), eq(savings), any(), any()))
        .thenReturn(List.of(tx(Direction.DEPOSIT, day, 300_000)));

    assertEquals(20_000, service(bankPort).fetchAssetSnapshot(USER).monthlySpend());
  }

  @Test
  @DisplayName("짝은 일대일로 맞춘다 — 같은 금액 출금 두 건에 입금 한 건이면 한 건만 이체다")
  void pairsOneToOne() {
    links(link("KB", LinkStatus.ACTIVE), link("SHINHAN", LinkStatus.ACTIVE));
    LocalDate day = TODAY.withDayOfMonth(10);
    institution(
        "KB", 0, tx(Direction.WITHDRAWAL, day, 100_000), tx(Direction.WITHDRAWAL, day, 100_000));
    institution("SHINHAN", 0, tx(Direction.DEPOSIT, day, 100_000));

    assertEquals(100_000, service(bankPort).fetchAssetSnapshot(USER).monthlySpend());
  }

  @Test
  @DisplayName("같은 계좌 안의 입출금, 다른 날, 다른 금액은 이체로 보지 않는다")
  void onlySameDaySameAmountAcrossAccountsIsTransfer() {
    // 같은 계좌에서 같은 금액이 나갔다 들어온 것은 취소·환불일 수 있다.
    links(link("KB", LinkStatus.ACTIVE), link("SHINHAN", LinkStatus.ACTIVE));
    LocalDate day = TODAY.withDayOfMonth(10);
    institution(
        "KB",
        0,
        tx(Direction.WITHDRAWAL, day, 100_000),
        tx(Direction.DEPOSIT, day, 100_000), // 같은 계좌
        tx(Direction.WITHDRAWAL, day, 70_000));
    institution(
        "SHINHAN",
        0,
        tx(Direction.DEPOSIT, day.plusDays(1), 70_000), // 다음 날
        tx(Direction.DEPOSIT, day, 69_000)); // 다른 금액

    assertEquals(170_000, service(bankPort).fetchAssetSnapshot(USER).monthlySpend());
  }

  @Test
  @DisplayName("모의 거래내역의 적금 납입은 이체로 짝지어져 지출에서 빠진다")
  void stubSavingsContributionIsTransfer() {
    // 예전 모의에는 적금 입금에 짝이 되는 출금이 없어서 이 결함이 보이지 않았다.
    MyDataProperties stub = new MyDataProperties();
    stub.setStubMode(true);
    MyDataBankPort stubPort =
        new MyDataBankApiAdapter(
            new RestTemplate(),
            stub,
            new ResilientHttpExecutor(CircuitBreakerRegistry.ofDefaults()));
    links(link("KB", LinkStatus.ACTIVE));

    AssetSnapshot snapshot = service(stubPort).fetchAssetSnapshot(USER);

    String checking = StandardBankFixtures.checkingAccount("KB");
    long withdrawals = 0;
    boolean contribution = false;
    String next = null;
    do {
      var page =
          StandardBankFixtures.transactions("KB", checking, TODAY.withDayOfMonth(1), TODAY, next);
      for (var transaction : page.transList()) {
        if ("02".equals(transaction.transType())) {
          withdrawals += transaction.transAmt().longValue();
          contribution |=
              transaction.transDtime().startsWith("20260910")
                  && transaction.transAmt().longValue() == 300_000;
        }
      }
      next = page.nextPage();
    } while (next != null);

    assertTrue(contribution, "모의 입출금 통장에 적금 납입 출금이 없다");
    assertEquals(withdrawals - 300_000, (long) snapshot.monthlySpend());
  }
}
