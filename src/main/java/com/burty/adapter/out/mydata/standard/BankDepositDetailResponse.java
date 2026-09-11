package com.burty.adapter.out.mydata.standard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/** 수신계좌 추가정보 조회 응답 — {@code POST /v1/bank/accounts/deposit/detail}. 잔액이 여기 있다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BankDepositDetailResponse(
    @JsonProperty("rsp_code") String rspCode,
    @JsonProperty("rsp_msg") String rspMsg,
    @JsonProperty("search_timestamp") Long searchTimestamp,
    @JsonProperty("detail_cnt") Integer detailCount,
    @JsonProperty("detail_list") List<Detail> detailList)
    implements StandardResponse {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Detail(
      @JsonProperty("currency_code") String currencyCode,
      @JsonProperty("balance_amt") BigDecimal balanceAmt,
      @JsonProperty("withdrawable_amt") BigDecimal withdrawableAmt,
      @JsonProperty("offered_rate") BigDecimal offeredRate,
      @JsonProperty("last_paid_in_cnt") Integer lastPaidInCnt) {}
}
