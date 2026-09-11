package com.burty.adapter.out.mydata.standard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 모의 모드의 정보제공자 응답. 표준 응답 형태 그대로 만든다.
 *
 * <p>예전 모의는 어떤 사용자에게나 고정값 하나(총자산 3억 2천만 원)를 돌려줬다. 이제는 모의 모드에서도 어댑터의 응답 해석·페이지 순회·응답코드 판정이 그대로 돈다.
 * 기관코드마다 값이 달라서 여러 기관을 연결하면 합산이 실제로 일어나는지 화면에서 보인다.
 *
 * <p>같은 입력에는 늘 같은 값을 준다. 새로고침할 때마다 잔액이 바뀌면 모의가 아니라 잡음이다.
 */
public final class StandardBankFixtures {

  /** 계좌 두 개를 두 쪽으로 나눠, 모의에서도 {@code next_page} 순회를 태운다. */
  static final int ACCOUNTS_PER_PAGE = 1;

  /** 거래내역도 여러 쪽으로 나눈다. 실제 기관은 요청한 limit 을 따르지만 순회 코드는 같다. */
  static final int TRANSACTIONS_PER_PAGE = 20;

  static final String DEPOSIT = "01";
  static final String WITHDRAWAL = "02";

  private static final String OK_MSG = "정상처리";
  private static final DateTimeFormatter DTIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private StandardBankFixtures() {}

  public static BankAccountListResponse accounts(String orgCode, String nextPage) {
    List<BankAccountListResponse.Account> all =
        List.of(
            new BankAccountListResponse.Account(
                checkingAccount(orgCode), true, "1", false, "모의 입출금통장", false, "1001", "01"),
            new BankAccountListResponse.Account(
                savingsAccount(orgCode), true, "1", false, "모의 자유적금", false, "1003", "01"));
    int start = pageStart(nextPage, all.size());
    int end = Math.min(start + ACCOUNTS_PER_PAGE, all.size());
    return new BankAccountListResponse(
        StandardResponse.SUCCESS,
        OK_MSG,
        0L,
        "20250101",
        end < all.size() ? String.valueOf(end) : null,
        end - start,
        List.copyOf(all.subList(start, end)));
  }

  public static BankDepositDetailResponse detail(String orgCode, String accountNum) {
    BigDecimal balance = BigDecimal.valueOf(balanceOf(orgCode, accountNum));
    return new BankDepositDetailResponse(
        StandardResponse.SUCCESS,
        OK_MSG,
        0L,
        1,
        List.of(
            new BankDepositDetailResponse.Detail(
                "KRW", balance, balance, new BigDecimal("2.50"), 0)));
  }

  public static BankTransactionListResponse transactions(
      String orgCode, String accountNum, LocalDate from, LocalDate to, String nextPage) {
    List<BankTransactionListResponse.Transaction> all =
        buildTransactions(orgCode, accountNum, from, to);
    int start = pageStart(nextPage, all.size());
    int end = Math.min(start + TRANSACTIONS_PER_PAGE, all.size());
    return new BankTransactionListResponse(
        StandardResponse.SUCCESS,
        OK_MSG,
        end < all.size() ? String.valueOf(end) : null,
        end - start,
        List.copyOf(all.subList(start, end)));
  }

  /** 입출금 통장의 잔액. 합산 결과를 테스트에서 계산해 볼 수 있게 공개한다. */
  public static long balanceOf(String orgCode, String accountNum) {
    int seed = seed(orgCode);
    return accountNum.equals(savingsAccount(orgCode))
        ? 3_000_000L + (seed % 13) * 1_000_000L
        : 1_200_000L + (seed % 28) * 100_000L;
  }

  public static String checkingAccount(String orgCode) {
    return "100" + String.format("%07d", seed(orgCode));
  }

  public static String savingsAccount(String orgCode) {
    return "200" + String.format("%07d", seed(orgCode));
  }

  private static List<BankTransactionListResponse.Transaction> buildTransactions(
      String orgCode, String accountNum, LocalDate from, LocalDate to) {
    record Entry(LocalDate date, String type, String transClass, long amount) {}

    int seed = seed(orgCode);
    boolean savings = accountNum.equals(savingsAccount(orgCode));
    // 매달 10일 입출금 통장에서 적금으로 옮기는 금액. 양쪽에 같은 날 같은 금액으로 보여야 한다 — 짝 없는 입금만
    // 있으면 내 계좌 간 이체를 가르는 규칙이 모의에서 한 번도 돌지 않는다.
    long savingsContribution = 300_000L;
    List<Entry> entries = new ArrayList<>();
    for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
      int dom = day.getDayOfMonth();
      if (savings) {
        if (dom == 10) {
          entries.add(new Entry(day, DEPOSIT, "자동이체", savingsContribution));
        }
        continue;
      }
      if (dom == 10) {
        entries.add(new Entry(day, WITHDRAWAL, "자동이체", savingsContribution));
      }
      if (dom == 25) {
        entries.add(new Entry(day, DEPOSIT, "급여", 2_800_000L + (seed % 9) * 100_000L));
      }
      if (dom == 5) {
        entries.add(new Entry(day, WITHDRAWAL, "자동이체", 450_000L + (seed % 6) * 50_000L));
      }
      if (dom == 15) {
        entries.add(new Entry(day, WITHDRAWAL, "카드대금", 300_000L + (seed % 8) * 50_000L));
      }
      if ((day.getDayOfYear() + seed) % 3 == 0) {
        entries.add(
            new Entry(
                day,
                WITHDRAWAL,
                "체크카드",
                8_000L + ((day.getDayOfYear() * 7L + seed) % 50) * 1_000L));
      }
    }

    // 거래 후 잔액이 마지막 거래에서 현재 잔액과 맞도록 거꾸로 시작점을 잡는다.
    long net =
        entries.stream().mapToLong(e -> DEPOSIT.equals(e.type()) ? e.amount() : -e.amount()).sum();
    long running = balanceOf(orgCode, accountNum) - net;
    List<BankTransactionListResponse.Transaction> out = new ArrayList<>(entries.size());
    int no = 1;
    for (Entry e : entries) {
      running += DEPOSIT.equals(e.type()) ? e.amount() : -e.amount();
      out.add(
          new BankTransactionListResponse.Transaction(
              e.date().atTime(12, 0).format(DTIME),
              String.format("%08d", no++),
              e.type(),
              e.transClass(),
              "KRW",
              BigDecimal.valueOf(e.amount()),
              BigDecimal.valueOf(running),
              savings ? 1 : null));
    }
    return out;
  }

  private static int pageStart(String nextPage, int size) {
    if (nextPage == null || nextPage.isBlank()) {
      return 0;
    }
    try {
      return Math.min(Math.max(Integer.parseInt(nextPage), 0), size);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static int seed(String orgCode) {
    return Math.floorMod(orgCode == null ? 0 : orgCode.hashCode(), 1000);
  }
}
