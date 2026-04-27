import request from './request'

export function batchAssign(data) {
  return request({
    url: '/batch/assign',
    method: 'post',
    data
  })
}

export function batchUpdateStatus(data) {
  return request({
    url: '/batch/status',
    method: 'post',
    data
  })
}

export function batchClose(data) {
  return request({
    url: '/batch/close',
    method: 'post',
    data
  })
}

export function batchResolve(data) {
  return request({
    url: '/batch/resolve',
    method: 'post',
    data
  })
}

export function batchAssignAsync(data) {
  return request({
    url: '/batch/assign/async',
    method: 'post',
    data
  })
}
