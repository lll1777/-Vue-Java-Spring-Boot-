package com.ticket.management.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RecallApprovalDTO {

    @NotBlank(message = "撤回原因不能为空")
    @Size(max = 500, message = "撤回原因不能超过500字符")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
