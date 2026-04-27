package com.ticket.management.dto;

import javax.validation.constraints.NotNull;

public class AddSignDTO {

    @NotNull(message = "加签用户不能为空")
    private Long signUserId;

    private String signType = "AFTER_SIGN";

    private String reason;

    public Long getSignUserId() {
        return signUserId;
    }

    public void setSignUserId(Long signUserId) {
        this.signUserId = signUserId;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
