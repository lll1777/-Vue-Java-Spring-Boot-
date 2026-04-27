import request from './request'

export function exportTicketReport(data) {
  return request({
    url: '/reports/export',
    method: 'post',
    data,
    responseType: 'blob'
  })
}

export function getDashboardStatistics(params) {
  return request({
    url: '/reports/dashboard',
    method: 'get',
    params
  })
}

export function getSLAStatistics(params) {
  return request({
    url: '/reports/sla',
    method: 'get',
    params
  })
}

export function exportByConfig(configId, data) {
  return request({
    url: `/reports/export/${configId}`,
    method: 'post',
    data,
    responseType: 'blob'
  })
}

export function getReportConfigList(params) {
  return request({
    url: '/report-configs',
    method: 'get',
    params
  })
}

export function getReportConfig(id) {
  return request({
    url: `/report-configs/${id}`,
    method: 'get'
  })
}

export function createReportConfig(data) {
  return request({
    url: '/report-configs',
    method: 'post',
    data
  })
}

export function updateReportConfig(id, data) {
  return request({
    url: `/report-configs/${id}`,
    method: 'put',
    data
  })
}

export function deleteReportConfig(id) {
  return request({
    url: `/report-configs/${id}`,
    method: 'delete'
  })
}
