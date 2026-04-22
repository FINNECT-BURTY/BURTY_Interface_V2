package com.nuri.adapter.in.web.dto;

public class FamilyConsentRequest {
    private String parentUserId;
    private String childUserId;

    public String getParentUserId() { return parentUserId; }
    public void setParentUserId(String parentUserId) { this.parentUserId = parentUserId; }
    public String getChildUserId() { return childUserId; }
    public void setChildUserId(String childUserId) { this.childUserId = childUserId; }
}
