package com.burty.application.port.out.mydata;

import com.burty.domain.mydata.model.BankAccount;
import com.burty.domain.mydata.model.BankTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 은행 정보제공자의 표준 API — 기관 한 곳, 접근토큰 하나 단위.
 *
 * <p>여러 기관을 합치는 일은 이 포트 밖(애플리케이션)에서 한다. 여기서는 규격대로 부르고 응답을 해석하는 것만 한다.
 *
 * @throws MyDataUnauthorizedException 접근토큰이 거절됐을 때. 호출부가 재발급 후 한 번 재시도한다.
 * @throws MyDataApiException 그 밖에 정보제공자가 정상 응답을 주지 않았을 때
 */
public interface MyDataBankPort {

  /** 계좌목록. {@code next_page} 를 끝까지 따라가 전부 돌려준다. */
  List<BankAccount> listAccounts(String orgCode, String accessToken);

  /** 수신계좌 잔액. */
  BigDecimal depositBalance(String orgCode, String accessToken, BankAccount account);

  /** 수신계좌 거래내역. 기간 양끝을 포함한다. */
  List<BankTransaction> depositTransactions(
      String orgCode, String accessToken, BankAccount account, LocalDate from, LocalDate to);
}
