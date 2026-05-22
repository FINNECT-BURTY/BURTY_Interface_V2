package com.burty.application.service;

import com.burty.application.port.in.YouthPolicyUseCase;
import com.burty.application.port.out.YouthPolicyPort;
import com.burty.config.YouthCenterProperties;
import com.burty.domain.entity.YouthPolicyEntity;
import com.burty.domain.repository.YouthPolicyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class YouthPolicyService implements YouthPolicyUseCase {

    private final YouthPolicyPort youthPolicyPort;
    private final YouthPolicyRepository youthPolicyRepository;
    private final YouthCenterProperties properties;

    public YouthPolicyService(YouthPolicyPort youthPolicyPort,
                              YouthPolicyRepository youthPolicyRepository,
                              YouthCenterProperties properties) {
        this.youthPolicyPort = youthPolicyPort;
        this.youthPolicyRepository = youthPolicyRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public int syncPolicies(String zipCd, String lclsfNm, String keyword) {
        int pageNum = 1;
        int totalSaved = 0;

        while (true) {
            List<YouthPolicyEntity> fetched = youthPolicyPort.fetchPolicies(
                    pageNum, properties.getPageSize(), zipCd, lclsfNm, keyword
            );
            if (fetched.isEmpty()) break;

            for (YouthPolicyEntity incoming : fetched) {
                if (incoming.getPlcyNo() == null) continue;
                youthPolicyRepository.findByPlcyNo(incoming.getPlcyNo())
                        .ifPresentOrElse(
                                existing -> copyFields(incoming, existing),
                                () -> youthPolicyRepository.save(incoming)
                        );
                totalSaved++;
            }

            if (fetched.size() < properties.getPageSize()) break;
            pageNum++;
        }
        return totalSaved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<YouthPolicyEntity> searchPolicies(String lclsfNm, String mclsfNm, String zipCd,
                                                   String keyword, Integer minAge, Integer maxAge,
                                                   Pageable pageable) {
        return youthPolicyRepository.search(
                blank(lclsfNm) ? null : lclsfNm,
                blank(mclsfNm) ? null : mclsfNm,
                blank(zipCd) ? null : zipCd,
                blank(keyword) ? null : keyword,
                minAge,
                maxAge,
                pageable
        );
    }

    private void copyFields(YouthPolicyEntity src, YouthPolicyEntity dest) {
        dest.setPlcyNm(src.getPlcyNm());
        dest.setPlcyKywdNm(src.getPlcyKywdNm());
        dest.setPlcyExplnCn(src.getPlcyExplnCn());
        dest.setLclsfNm(src.getLclsfNm());
        dest.setMclsfNm(src.getMclsfNm());
        dest.setPlcySprtCn(src.getPlcySprtCn());
        dest.setSprvsnInstCdNm(src.getSprvsnInstCdNm());
        dest.setOperInstCdNm(src.getOperInstCdNm());
        dest.setAplyPrdSeCd(src.getAplyPrdSeCd());
        dest.setBizPrdBgngYmd(src.getBizPrdBgngYmd());
        dest.setBizPrdEndYmd(src.getBizPrdEndYmd());
        dest.setPlcyAplyMthdCn(src.getPlcyAplyMthdCn());
        dest.setAplyUrlAddr(src.getAplyUrlAddr());
        dest.setRefUrlAddr1(src.getRefUrlAddr1());
        dest.setSprtTrgtMinAge(src.getSprtTrgtMinAge());
        dest.setSprtTrgtMaxAge(src.getSprtTrgtMaxAge());
        dest.setSprtTrgtAgeLmtYn(src.getSprtTrgtAgeLmtYn());
        dest.setZipCd(src.getZipCd());
        dest.setEarnCndSeCd(src.getEarnCndSeCd());
        dest.setEarnMinAmt(src.getEarnMinAmt());
        dest.setEarnMaxAmt(src.getEarnMaxAmt());
        dest.setEarnEtcCn(src.getEarnEtcCn());
        dest.setMrgSttsCd(src.getMrgSttsCd());
        dest.setJobCd(src.getJobCd());
        dest.setSchoolCd(src.getSchoolCd());
        dest.setSBizCd(src.getSBizCd());
        dest.setAplyYmd(src.getAplyYmd());
        dest.setFrstRegDt(src.getFrstRegDt());
        dest.setLastMdfcnDt(src.getLastMdfcnDt());
        dest.setInqCnt(src.getInqCnt());
    }

    private boolean blank(String v) {
        return v == null || v.isBlank();
    }
}
