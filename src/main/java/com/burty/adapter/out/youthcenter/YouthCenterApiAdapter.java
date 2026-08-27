/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (YouthCenterApiAdapter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.youthcenter
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.adapter.out.youthcenter;

import com.burty.adapter.out.youthcenter.dto.YouthCenterApiResponse;
import com.burty.adapter.out.youthcenter.dto.YouthCenterApiResponse.PolicyItem;
import com.burty.application.port.out.policy.YouthPolicyPort;
import com.burty.config.YouthCenterProperties;
import com.burty.core.config.HttpClientFactory;
import com.burty.domain.policy.entity.YouthPolicyEntity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class YouthCenterApiAdapter implements YouthPolicyPort {

  private final RestTemplate restTemplate;
  private final YouthCenterProperties properties;

  public YouthCenterApiAdapter(
      HttpClientFactory httpClientFactory, YouthCenterProperties properties) {
    this.restTemplate = httpClientFactory.restTemplate("youth-center", properties.getTimeoutMs());
    this.properties = properties;
  }

  @Override
  public List<YouthPolicyEntity> fetchPolicies(
      int pageNum, int pageSize, String zipCd, String lclsfNm, String keyword) {
    StringBuilder url =
        new StringBuilder(properties.getBaseUrl())
            .append("?apiKeyNm=")
            .append(encode(properties.getApiKey()))
            .append("&pageNum=")
            .append(pageNum)
            .append("&pageSize=")
            .append(pageSize)
            .append("&pageType=1")
            .append("&rtnType=json");

    if (zipCd != null && !zipCd.isBlank()) url.append("&zipCd=").append(encode(zipCd));
    if (lclsfNm != null && !lclsfNm.isBlank()) url.append("&lclsfNm=").append(encode(lclsfNm));
    if (keyword != null && !keyword.isBlank()) url.append("&plcyKywdNm=").append(encode(keyword));

    try {
      YouthCenterApiResponse response =
          restTemplate.getForObject(url.toString(), YouthCenterApiResponse.class);
      if (response == null || response.getYouthPolicyList() == null) {
        return Collections.emptyList();
      }
      return response.getYouthPolicyList().stream().map(this::toEntity).toList();
    } catch (RestClientException e) {
      throw new IllegalStateException("온통청년 API 호출 실패: " + e.getMessage(), e);
    }
  }

  private YouthPolicyEntity toEntity(PolicyItem item) {
    YouthPolicyEntity e = new YouthPolicyEntity();
    e.setPlcyNo(item.getPlcyNo());
    e.setPlcyNm(item.getPlcyNm());
    e.setPlcyKywdNm(item.getPlcyKywdNm());
    e.setPlcyExplnCn(item.getPlcyExplnCn());
    e.setLclsfNm(item.getLclsfNm());
    e.setMclsfNm(item.getMclsfNm());
    e.setPlcySprtCn(item.getPlcySprtCn());
    e.setSprvsnInstCdNm(item.getSprvsnInstCdNm());
    e.setOperInstCdNm(item.getOperInstCdNm());
    e.setAplyPrdSeCd(item.getAplyPrdSeCd());
    e.setBizPrdBgngYmd(item.getBizPrdBgngYmd());
    e.setBizPrdEndYmd(item.getBizPrdEndYmd());
    e.setPlcyAplyMthdCn(item.getPlcyAplyMthdCn());
    e.setAplyUrlAddr(item.getAplyUrlAddr());
    e.setRefUrlAddr1(item.getRefUrlAddr1());
    e.setSprtTrgtMinAge(item.getSprtTrgtMinAge());
    e.setSprtTrgtMaxAge(item.getSprtTrgtMaxAge());
    e.setSprtTrgtAgeLmtYn(item.getSprtTrgtAgeLmtYn());
    e.setZipCd(item.getZipCd());
    e.setEarnCndSeCd(item.getEarnCndSeCd());
    e.setEarnMinAmt(item.getEarnMinAmt());
    e.setEarnMaxAmt(item.getEarnMaxAmt());
    e.setEarnEtcCn(item.getEarnEtcCn());
    e.setMrgSttsCd(item.getMrgSttsCd());
    e.setJobCd(item.getJobCd());
    e.setSchoolCd(item.getSchoolCd());
    e.setSBizCd(item.getSBizCd());
    e.setAplyYmd(item.getAplyYmd());
    e.setFrstRegDt(item.getFrstRegDt());
    e.setLastMdfcnDt(item.getLastMdfcnDt());
    e.setInqCnt(item.getInqCnt());
    return e;
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
