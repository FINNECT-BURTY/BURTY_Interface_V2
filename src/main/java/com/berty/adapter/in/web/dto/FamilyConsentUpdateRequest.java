package com.berty.adapter.in.web.dto;

public class FamilyConsentUpdateRequest {
    private String parentUserId;
    private String childUserId;
    private boolean consented;

    public String getParentUserId() {
        return parentUserId;
    }

    public void setParentUserId(String parentUserId) {
        this.parentUserId = parentUserId;
    }

    public String getChildUserId() {
        return childUserId;
    }

    public void setChildUserId(String childUserId) {
        this.childUserId = childUserId;
    }

    public boolean isConsented() {
        return consented;
    }

    public void setConsented(boolean consented) {
        this.consented = consented;
    }
}
