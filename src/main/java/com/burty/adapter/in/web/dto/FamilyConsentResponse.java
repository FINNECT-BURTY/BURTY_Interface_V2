package com.burty.adapter.in.web.dto;

public class FamilyConsentResponse {
    private String parentUserId;
    private String childUserId;
    private boolean consented;

    public FamilyConsentResponse() {}
    public FamilyConsentResponse(String parentUserId, String childUserId, boolean consented) {
        this.parentUserId = parentUserId;
        this.childUserId = childUserId;
        this.consented = consented;
    }

    public String getParentUserId() { return parentUserId; }
    public void setParentUserId(String parentUserId) { this.parentUserId = parentUserId; }
    public String getChildUserId() { return childUserId; }
    public void setChildUserId(String childUserId) { this.childUserId = childUserId; }
    public boolean isConsented() { return consented; }
    public void setConsented(boolean consented) { this.consented = consented; }
}
