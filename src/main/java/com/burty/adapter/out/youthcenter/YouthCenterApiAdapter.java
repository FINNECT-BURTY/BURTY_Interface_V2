package com.burty.adapter.out.youthcenter;

import com.burty.adapter.out.youthcenter.dto.YouthCenterApiResponse;
import com.burty.adapter.out.youthcenter.dto.YouthCenterApiResponse.PolicyItem;
import com.burty.application.port.out.YouthPolicyPort;
import com.burty.config.YouthCenterProperties;
import com.burty.domain.entity.YouthPolicyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class YouthCenterApiAdapter implements YouthPolicyPort {

    private static final Logger log = LoggerFactory.getLogger(YouthCenterApiAdapter.class);

    private final RestTemplate restTemplate;
    private final YouthCenterProperties properties;

    public YouthCenterApiAdapter(RestTemplate restTemplate, YouthCenterProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public List<YouthPolicyEntity> fetchPolicies(int pageNum, int pageSize, String zipCd, String lclsfNm, String keyword) {
        StringBuilder url = new StringBuilder(properties.getBaseUrl())
                .append("?apiKeyNm=").append(encode(properties.getApiKey()))
                .append("&pageNum=").append(pageNum)
                .append("&pageSize=").append(pageSize)
                .append("&pageType=1")
                .append("&rtnType=json");

        if (zipCd != null && !zipCd.isBlank()) url.append("&zipCd=").append(encode(zipCd));
        if (lclsfNm != null && !lclsfNm.isBlank()) url.append("&lclsfNm=").append(encode(lclsfNm));
        if (keyword != null && !keyword.isBlank()) url.append("&plcyKywdNm=").append(encode(keyword));

        String urlStr = url.toString();
        log.info("[YouthCenter] 요청 URL: {}", urlStr);

        try {
            // 원시 응답 먼저 확인
            String raw = restTemplate.getForObject(urlStr, String.class);
            log.info("[YouthCenter] 원시 응답(앞 500자): {}", raw != null ? raw.substring(0, Math.min(500, raw.length())) : "null");

            if (raw == null || raw.isBlank()) {
                return Collections.emptyList();
            }

            YouthCenterApiResponse response = restTemplate.getForObject(urlStr, YouthCenterApiResponse.class);
            if (response == null || response.getYouthPolicyList() == null) {
                log.warn("[YouthCenter] youthPolicyList 파싱 결과 null — 응답 구조 확인 필요");
                return Collections.emptyList();
            }
            log.info("[YouthCenter] 파싱된 정책 수: {}", response.getYouthPolicyList().size());
            return response.getYouthPolicyList().stream()
                    .map(this::toEntity)
                    .toList();
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
