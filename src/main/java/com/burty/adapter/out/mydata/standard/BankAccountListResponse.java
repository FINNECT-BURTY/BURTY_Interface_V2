package com.burty.adapter.out.mydata.standard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** 은행 계좌목록 조회 응답 — {@code GET /v1/bank/accounts}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BankAccountListResponse(
    @JsonProperty("rsp_code") String rspCode,
    @JsonProperty("rsp_msg") String rspMsg,
    @JsonProperty("search_timestamp") Long searchTimestamp,
    @JsonProperty("reg_date") String regDate,
    @JsonProperty("next_page") String nextPage,
    @JsonProperty("account_cnt") Integer accountCount,
    @JsonProperty("account_list") List<Account> accountList)
    implements StandardResponse {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Account(
      @JsonProperty("account_num") String accountNum,
      @JsonProperty("is_consent") Boolean isConsent,
      @JsonProperty("seqno") String seqno,
      @JsonProperty("is_foreign_deposit") Boolean isForeignDeposit,
      @JsonProperty("prod_name") String prodName,
      @JsonProperty("is_minus") Boolean isMinus,
      @JsonProperty("account_type") String accountType,
      @JsonProperty("account_status") String accountStatus) {}
}
