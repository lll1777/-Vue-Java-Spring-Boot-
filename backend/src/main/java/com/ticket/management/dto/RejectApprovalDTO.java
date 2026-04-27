package com.ticket.management.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RejectApprovalDTO {

    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 500, message = "驳回原因不能超过500字符")
    private String comments;

    private String rejectTarget = "INITIATOR";

    private Long targetNodeId;

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getRejectTarget() {
        return rejectTarget;
    }

    public void setRejectTarget(String rejectTarget) {
        this.rejectTarget = rejectTarget;
    }

    public Long getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(Long targetNodeId) {
        this.targetNodeId = targetNodeId;
    }
}
