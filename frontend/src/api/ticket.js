import request from './request'

export function createTicket(data) {
  return request({
    url: '/tickets',
    method: 'post',
    data
  })
}

export function getTicket(id) {
  return request({
    url: `/tickets/${id}`,
    method: 'get'
  })
}

export function getTicketByNo(ticketNo) {
  return request({
    url: `/tickets/no/${ticketNo}`,
    method: 'get'
  })
}

export function updateTicket(id, data) {
  return request({
    url: `/tickets/${id}`,
    method: 'put',
    data
  })
}

export function getMyTickets(params) {
  return request({
    url: '/tickets/my',
    method: 'get',
    params
  })
}

export function getCreatedTickets(params) {
  return request({
    url: '/tickets/created',
    method: 'get',
    params
  })
}

export function assignTicket(id, data) {
  return request({
    url: `/tickets/${id}/assign`,
    method: 'post',
    data
  })
}

export function updateTicketStatus(id, data) {
  return request({
    url: `/tickets/${id}/status`,
    method: 'post',
    data
  })
}

export function getTicketHistory(id) {
  return request({
    url: `/tickets/${id}/history`,
    method: 'get'
  })
}

export function getTicketList(params) {
  return request({
    url: '/tickets',
    method: 'get',
    params
  })
}
