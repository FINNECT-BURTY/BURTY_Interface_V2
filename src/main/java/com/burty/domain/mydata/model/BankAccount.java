package com.burty.domain.mydata.model;

/**
 * 정보제공자(은행)가 알려준 계좌 한 건.
 *
 * <p>{@code seqno} 는 같은 계좌번호 아래 회차를 구분한다. 계좌번호만으로 식별하면 예·적금 회차가 섞인다.
 */
public record BankAccount(
    String accountNum,
    String seqno,
    String productName,
    String accountType,
    String accountStatus,
    boolean consented) {}
