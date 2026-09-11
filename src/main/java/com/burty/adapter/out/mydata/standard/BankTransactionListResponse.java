package com.burty.adapter.out.mydata.standard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/** 수신계좌 거래내역 조회 응답 — {@code POST /v1/bank/accounts/deposit/transactions}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BankTransactionListResponse(
    @JsonProperty("rsp_code") String rspCode,
    @JsonProperty("rsp_msg") String rspMsg,
    @JsonProperty("next_page") String nextPage,
    @JsonProperty("trans_cnt") Integer transCount,
    @JsonProperty("trans_list") List<Transaction> transList)
    implements StandardResponse {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Transaction(
      @JsonProperty("trans_dtime") String transDtime,
      @JsonProperty("trans_no") String transNo,
      @JsonProperty("trans_type") String transType,
      @JsonProperty("trans_class") String transClass,
      @JsonProperty("currency_code") String currencyCode,
      @JsonProperty("trans_amt") BigDecimal transAmt,
      @JsonProperty("balance_amt") BigDecimal balanceAmt,
      @JsonProperty("paid_in_cnt") Integer paidInCnt) {}
}
