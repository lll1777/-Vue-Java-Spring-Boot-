import request from './request'

export function startApproval(data) {
  return request({
    url: '/approvals/start',
    method: 'post',
    data
  })
}

export function approveInstance(instanceId, data) {
  return request({
    url: `/approvals/${instanceId}/approve`,
    method: 'post',
    data
  })
}

export function rejectInstance(instanceId, data) {
  return request({
    url: `/approvals/${instanceId}/reject`,
    method: 'post',
    data
  })
}

export function forwardInstance(instanceId, data) {
  return request({
    url: `/approvals/${instanceId}/forward`,
    method: 'post',
    data
  })
}

export function getApprovalInstance(instanceId) {
  return request({
    url: `/approvals/${instanceId}`,
    method: 'get'
  })
}

export function getMyPendingApprovals(params) {
  return request({
    url: '/approvals/pending',
    method: 'get',
    params
  })
}

export function getMyApprovalHistory(params) {
  return request({
    url: '/approvals/history',
    method: 'get',
    params
  })
}

export function getApprovalFlowList(params) {
  return request({
    url: '/approval-flows',
    method: 'get',
    params
  })
}

export function getApprovalFlow(id) {
  return request({
    url: `/approval-flows/${id}`,
    method: 'get'
  })
}

export function createApprovalFlow(data) {
  return request({
    url: '/approval-flows',
    method: 'post',
    data
  })
}

export function updateApprovalFlow(id, data) {
  return request({
    url: `/approval-flows/${id}`,
    method: 'put',
    data
  })
}

export function deleteApprovalFlow(id) {
  return request({
    url: `/approval-flows/${id}`,
    method: 'delete'
  })
}
