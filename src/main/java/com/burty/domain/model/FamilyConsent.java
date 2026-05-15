package com.burty.domain.model;

public class FamilyConsent {
    private String parentUserId;
    private String childUserId;
    private boolean consented;

    public FamilyConsent(String parentUserId, String childUserId, boolean consented) {
        this.parentUserId = parentUserId;
        this.childUserId = childUserId;
        this.consented = consented;
    }

    public String getParentUserId() { return parentUserId; }
    public String getChildUserId() { return childUserId; }
    public boolean isConsented() { return consented; }
}
