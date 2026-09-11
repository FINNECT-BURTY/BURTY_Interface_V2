package com.burty.application.service.mydata;

import com.burty.application.port.in.asset.AssetSnapshotQuery;
import com.burty.application.port.out.mydata.MyDataBankPort;
import com.burty.application.port.out.mydata.MyDataOAuthPort;
import com.burty.application.port.out.mydata.MyDataUnauthorizedException;
import com.burty.domain.asset.model.AssetSnapshot;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity.LinkStatus;
import com.burty.domain.mydata.model.BankAccount;
import com.burty.domain.mydata.model.BankTransaction;
import com.burty.domain.mydata.model.BankTransaction.Direction;
import com.burty.domain.mydata.model.MyDataTokenBundle;
import com.burty.domain.mydata.repository.LinkedInstitutionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 연결된 ACTIVE 기관 전체를 합산해 자산 스냅샷을 만든다.
 *
 * <p>예전에는 기관 하나({@code MYDATA})로 고정해 조회했다. 기관별 연결은 토큰을 {@code userId::기관코드} 로 저장하는데 조회는 {@code
 * userId} 키만 읽어서, 기관별로 연결해도 자산에 반영되지 않았다. 모의 모드에서는 연결 여부와 무관하게 고정값을 돌려줬다.
 *
 * <p>합산 규칙
 *
 * <ul>
 *   <li>총자산 — 정보주체가 전송에 동의한 계좌의 원화 잔액 합
 *   <li>월지출 — 이번 달 1일부터 오늘까지 출금 합
 *   <li>소득 변동성 — 직전 3개 완결 월의 월별 입금 합의 변동계수(%). 페르소나가 이 값을 소득 변동성으로 쓴다. 일별 지출의 변동을 넣으면 지출이 특정 날에 몰리는
 *       탓에 모두가 "변동이 크다" 로 분류된다.
 * </ul>
 *
 * <p>한 기관이 실패해도 나머지는 합산하고 실패 수를 싣는다. 일부만 더한 합계를 전체처럼 보여주면 안 되기 때문이다.
 *
 * <p>자기 계좌 간 이체(입출금 → 적금)는 가르지 않는다. 입출금에서는 출금, 적금에서는 입금으로 보인다.
 */
@Service
public class MyDataAssetAggregationService implements AssetSnapshotQuery {

  private static final Logger log = LoggerFactory.getLogger(MyDataAssetAggregationService.class);
  private static final int INCOME_MONTHS = 3;

  private final LinkedInstitutionRepository linkedInstitutionRepository;
  private final LinkedInstitutionPersistenceService linkedInstitutionPersistence;
  private final MyDataTokenHydrationService tokenHydrationService;
  private final MyDataOAuthPort myDataOAuthPort;
  private final MyDataBankPort bankPort;
  private final Clock clock;

  public MyDataAssetAggregationService(
      LinkedInstitutionRepository linkedInstitutionRepository,
      LinkedInstitutionPersistenceService linkedInstitutionPersistence,
      MyDataTokenHydrationService tokenHydrationService,
      MyDataOAuthPort myDataOAuthPort,
      MyDataBankPort bankPort,
      Clock clock) {
    this.linkedInstitutionRepository = linkedInstitutionRepository;
    this.linkedInstitutionPersistence = linkedInstitutionPersistence;
    this.tokenHydrationService = tokenHydrationService;
    this.myDataOAuthPort = myDataOAuthPort;
    this.bankPort = bankPort;
    this.clock = clock;
  }

  @Override
  public AssetSnapshot fetchAssetSnapshot(String userId) {
    Long numericUserId = parseUserId(userId);
    if (numericUserId == null) {
      // 데모 세션처럼 사용자 행이 없는 세션. 연동이 있을 수 없다.
      return AssetSnapshot.notLinked();
    }
    List<LinkedInstitutionEntity> links =
        linkedInstitutionRepository.findByUser_UserId(numericUserId).stream()
            .filter(link -> link.getStatus() == LinkStatus.ACTIVE)
            // 오픈뱅킹은 마이데이터 정보제공자가 아니다. 이 합산에 넣으면 엉뚱한 기관에 토큰을 보낸다.
            .filter(
                link ->
                    !LinkedInstitutionPersistenceService.OPEN_BANKING_CODE.equals(
                        link.getInstitutionCode()))
            .toList();
    if (links.isEmpty()) {
      return AssetSnapshot.notLinked();
    }

    LocalDate today = LocalDate.now(clock);
    LocalDate monthStart = today.withDayOfMonth(1);
    YearMonth thisMonth = YearMonth.from(today);
    LocalDate from = monthStart.minusMonths(INCOME_MONTHS);

    BigDecimal total = BigDecimal.ZERO;
    BigDecimal monthSpend = BigDecimal.ZERO;
    Map<YearMonth, BigDecimal> incomeByMonth = new HashMap<>();
    int failed = 0;
    for (LinkedInstitutionEntity link : links) {
      String code = link.getInstitutionCode();
      try {
        InstitutionData data = collect(userId, code, from, today);
        total = total.add(data.balance());
        for (BankTransaction transaction : data.transactions()) {
          YearMonth month = YearMonth.from(transaction.occurredAt());
          if (transaction.direction() == Direction.WITHDRAWAL && month.equals(thisMonth)) {
            monthSpend = monthSpend.add(transaction.amount());
          } else if (transaction.direction() == Direction.DEPOSIT && month.isBefore(thisMonth)) {
            incomeByMonth.merge(month, transaction.amount(), BigDecimal::add);
          }
        }
      } catch (RuntimeException e) {
        failed++;
        log.warn(
            "마이데이터 기관 조회 실패 — 합산에서 뺀다 userId={} institution={} err={}", userId, code, e.toString());
      }
    }

    List<YearMonth> incomeMonths = new ArrayList<>();
    for (int i = INCOME_MONTHS; i >= 1; i--) {
      incomeMonths.add(thisMonth.minusMonths(i));
    }
    return new AssetSnapshot(
        total.doubleValue(),
        monthSpend.doubleValue(),
        variabilityPercent(incomeByMonth, incomeMonths),
        links.size(),
        failed);
  }

  private InstitutionData collect(String userId, String code, LocalDate from, LocalDate to) {
    tokenHydrationService.hydrate(userId, code);
    InstitutionToken token = new InstitutionToken(userId, code);
    BigDecimal balance = BigDecimal.ZERO;
    List<BankTransaction> transactions = new ArrayList<>();
    for (BankAccount account : token.call(t -> bankPort.listAccounts(code, t))) {
      if (!account.consented()) {
        // 정보주체가 전송에 동의하지 않은 계좌는 조회하지 않는다.
        continue;
      }
      balance = balance.add(token.call(t -> bankPort.depositBalance(code, t, account)));
      transactions.addAll(
          token.call(t -> bankPort.depositTransactions(code, t, account, from, to)));
    }
    return new InstitutionData(balance, transactions);
  }

  /** 월별 합의 변동계수(%). 입금이 없는 달도 0 으로 넣는다 — 빼면 끊긴 소득이 안정적으로 보인다. */
  public static double variabilityPercent(
      Map<YearMonth, BigDecimal> byMonth, List<YearMonth> months) {
    double[] values =
        months.stream()
            .mapToDouble(month -> byMonth.getOrDefault(month, BigDecimal.ZERO).doubleValue())
            .toArray();
    double mean = 0;
    for (double value : values) {
      mean += value;
    }
    mean /= values.length;
    if (mean <= 0) {
      return 0;
    }
    double variance = 0;
    for (double value : values) {
      variance += (value - mean) * (value - mean);
    }
    variance /= values.length;
    return Math.round(Math.sqrt(variance) / mean * 1000) / 10.0;
  }

  private static Long parseUserId(String userId) {
    try {
      return userId == null ? null : Long.parseLong(userId);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private record InstitutionData(BigDecimal balance, List<BankTransaction> transactions) {}

  /**
   * 기관 한 곳의 접근토큰.
   *
   * <p>거절되면 한 번만 재발급해 다시 시도한다. 재발급한 토큰은 원본(DB)에도 남긴다 — 런타임 저장소에만 두면 다음 {@code hydrate()} 가 원본의 옛
   * 토큰으로 되돌려 놓는다. 예전 어댑터가 그렇게 해서 재발급할 때마다 옛 토큰으로 돌아갔다.
   */
  private final class InstitutionToken {

    private final String userId;
    private final String code;
    private final String scopeKey;
    private String token;
    private boolean refreshed;

    private InstitutionToken(String userId, String code) {
      this.userId = userId;
      this.code = code;
      this.scopeKey = MyDataOAuthPort.scopeKey(userId, code);
      this.token = myDataOAuthPort.findAccessToken(scopeKey);
      if (token == null || token.isBlank()) {
        throw new IllegalStateException("연동 토큰이 없다 institution=" + code);
      }
    }

    private <T> T call(Function<String, T> request) {
      try {
        return request.apply(token);
      } catch (MyDataUnauthorizedException e) {
        if (refreshed) {
          throw e;
        }
        refreshed = true;
        String next = myDataOAuthPort.refreshAccessToken(scopeKey);
        if (next == null || next.isBlank()) {
          throw e;
        }
        linkedInstitutionPersistence.updateRefreshedTokens(
            userId,
            code,
            new MyDataTokenBundle(
                next,
                myDataOAuthPort.findRefreshToken(scopeKey),
                myDataOAuthPort.findTokenExpiresAt(scopeKey)));
        token = next;
        return request.apply(token);
      }
    }
  }
}
