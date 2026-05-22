package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_youth_policy",
        indexes = {
                @Index(name = "idx_youth_policy_no", columnList = "plcy_no", unique = true),
                @Index(name = "idx_youth_policy_lclsf", columnList = "lclsf_nm"),
                @Index(name = "idx_youth_policy_zip", columnList = "zip_cd")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class YouthPolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "plcy_no", nullable = false, length = 50, unique = true)
    private String plcyNo;

    @Column(name = "plcy_nm", length = 500)
    private String plcyNm;

    @Column(name = "plcy_kywd_nm", length = 500)
    private String plcyKywdNm;

    @Column(name = "plcy_expln_cn", columnDefinition = "TEXT")
    private String plcyExplnCn;

    @Column(name = "lclsf_nm", length = 100)
    private String lclsfNm;

    @Column(name = "mclsf_nm", length = 100)
    private String mclsfNm;

    @Column(name = "plcy_sprt_cn", columnDefinition = "TEXT")
    private String plcySprtCn;

    @Column(name = "sprvsn_inst_cd_nm", length = 200)
    private String sprvsnInstCdNm;

    @Column(name = "oper_inst_cd_nm", length = 200)
    private String operInstCdNm;

    @Column(name = "aply_prd_se_cd", length = 10)
    private String aplyPrdSeCd;

    @Column(name = "biz_prd_bgng_ymd", length = 20)
    private String bizPrdBgngYmd;

    @Column(name = "biz_prd_end_ymd", length = 20)
    private String bizPrdEndYmd;

    @Column(name = "plcy_aply_mthd_cn", columnDefinition = "TEXT")
    private String plcyAplyMthdCn;

    @Column(name = "aply_url_addr", length = 500)
    private String aplyUrlAddr;

    @Column(name = "ref_url_addr1", length = 500)
    private String refUrlAddr1;

    @Column(name = "sprt_trgt_min_age", length = 10)
    private String sprtTrgtMinAge;

    @Column(name = "sprt_trgt_max_age", length = 10)
    private String sprtTrgtMaxAge;

    @Column(name = "sprt_trgt_age_lmt_yn", length = 1)
    private String sprtTrgtAgeLmtYn;

    @Column(name = "zip_cd", length = 200)
    private String zipCd;

    @Column(name = "earn_cnd_se_cd", length = 10)
    private String earnCndSeCd;

    @Column(name = "earn_min_amt", length = 30)
    private String earnMinAmt;

    @Column(name = "earn_max_amt", length = 30)
    private String earnMaxAmt;

    @Column(name = "earn_etc_cn", columnDefinition = "TEXT")
    private String earnEtcCn;

    @Column(name = "mrg_stts_cd", length = 10)
    private String mrgSttsCd;

    @Column(name = "job_cd", length = 100)
    private String jobCd;

    @Column(name = "school_cd", length = 100)
    private String schoolCd;

    @Column(name = "s_biz_cd", length = 100)
    private String sBizCd;

    @Column(name = "aply_ymd", length = 200)
    private String aplyYmd;

    @Column(name = "frst_reg_dt", length = 30)
    private String frstRegDt;

    @Column(name = "last_mdfcn_dt", length = 30)
    private String lastMdfcnDt;

    @Column(name = "inq_cnt", length = 20)
    private String inqCnt;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @PrePersist
    @PreUpdate
    void onSync() {
        syncedAt = LocalDateTime.now();
    }
}
