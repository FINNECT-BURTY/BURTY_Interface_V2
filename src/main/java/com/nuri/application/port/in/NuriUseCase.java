package com.nuri.application.port.in;

import com.nuri.domain.model.*;

import java.util.List;
import java.util.Map;

public interface NuriUseCase {
    ConsultationResult consult(String userId, String question);
    MonthlyReport createMonthlyReport(String userId);
    TransferResult transfer(String userId, String fromAccount, String toAccount, long amount, String description, String assertionToken);
    List<FamilyAlert> getFamilyAlerts(String userId);
    void updateLimit(String userId, long newLimit);
    long getLimit(String userId);
    void registerFamilyConsent(String parentUserId, String childUserId);
    boolean updateFamilyConsent(String parentUserId, String childUserId, boolean consented);
    boolean revokeFamilyConsent(String parentUserId, String childUserId);
    List<FamilyConsent> getFamilyConsents(String parentUserId);
    FamilyDashboardSummary getFamilyDashboardSummary(String userId);
    TransferResult getTransfer(String transferId);
    List<TransferResult> getTransfers(String userId);
    Map<String, Object> getAssetSummary(String userId);
    List<Map<String, Object>> getAssetTrend(String userId);
    boolean unlinkMyDataInstitution(String userId, String institutionCode);
}
