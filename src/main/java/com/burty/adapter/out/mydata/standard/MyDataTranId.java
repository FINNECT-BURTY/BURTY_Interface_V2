package com.burty.adapter.out.mydata.standard;

import java.security.SecureRandom;

/**
 * 표준 API 거래고유번호({@code x-api-tran-id}).
 *
 * <p>요청마다 새로 만든다. 정보제공자 로그와 우리 로그를 요청 단위로 맞춰볼 수 있는 유일한 키라, 재사용하면 장애 때 어느 호출이 어느 응답인지 가를 수 없다.
 *
 * <p>형식: 기관코드(10) + 생성주체구분(M: 마이데이터사업자) + 부여번호(영문 대문자·숫자).
 */
public final class MyDataTranId {

  public static final String HEADER = "x-api-tran-id";

  private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
  private static final int ORG_CODE_LENGTH = 10;
  private static final int SERIAL_LENGTH = 14;
  private static final SecureRandom RANDOM = new SecureRandom();

  private MyDataTranId() {}

  public static String next(String orgCode) {
    if (orgCode == null || orgCode.length() != ORG_CODE_LENGTH) {
      throw new IllegalArgumentException("기관코드는 10자리여야 합니다: " + orgCode);
    }
    StringBuilder id = new StringBuilder(ORG_CODE_LENGTH + 1 + SERIAL_LENGTH);
    id.append(orgCode).append('M');
    for (int i = 0; i < SERIAL_LENGTH; i++) {
      id.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
    }
    return id.toString();
  }
}
