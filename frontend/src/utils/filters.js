import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export function formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return ''
  return dayjs(date).format(format)
}

export function formatRelativeTime(date) {
  if (!date) return ''
  return dayjs(date).fromNow()
}

export function ticketStatusFilter(status) {
  const statusMap = {
    'DRAFT': { label: '草稿', type: 'info' },
    'PENDING_APPROVAL': { label: '待审批', type: 'warning' },
    'APPROVED': { label: '已审批', type: 'success' },
    'REJECTED': { label: '已拒绝', type: 'danger' },
    'ASSIGNED': { label: '已分配', type: 'primary' },
    'IN_PROGRESS': { label: '处理中', type: 'warning' },
    'PENDING_REVIEW': { label: '待审核', type: 'warning' },
    'RESOLVED': { label: '已解决', type: 'success' },
    'CLOSED': { label: '已关闭', type: 'info' },
    'CANCELLED': { label: '已取消', type: 'info' },
    'ESCALATED': { label: '已升级', type: 'danger' }
  }
  return statusMap[status] || { label: status, type: 'info' }
}

export function ticketPriorityFilter(priority) {
  const priorityMap = {
    'LOW': { label: '低', type: 'info' },
    'MEDIUM': { label: '中', type: 'warning' },
    'HIGH': { label: '高', type: 'danger' },
    'CRITICAL': { label: '紧急', type: 'danger' }
  }
  return priorityMap[priority] || { label: priority, type: 'info' }
}

export function approvalStatusFilter(status) {
  const statusMap = {
    'PENDING': { label: '待处理', type: 'warning' },
    'APPROVED': { label: '已通过', type: 'success' },
    'REJECTED': { label: '已拒绝', type: 'danger' },
    'FORWARDED': { label: '已转发', type: 'info' },
    'RECALLED': { label: '已撤回', type: 'info' }
  }
  return statusMap[status] || { label: status, type: 'info' }
}

export function slaStatusFilter(status) {
  const statusMap = {
    'NORMAL': { label: '正常', type: 'success' },
    'WARNING': { label: '预警', type: 'warning' },
    'OVERDUE': { label: '超时', type: 'danger' }
  }
  return statusMap[status] || { label: status, type: 'info' }
}

export function collaborationStatusFilter(status) {
  const statusMap = {
    'PENDING': { label: '待处理', type: 'warning' },
    'ACCEPTED': { label: '已接受', type: 'primary' },
    'REJECTED': { label: '已拒绝', type: 'danger' },
    'COMPLETED': { label: '已完成', type: 'success' },
    'CANCELLED': { label: '已取消', type: 'info' }
  }
  return statusMap[status] || { label: status, type: 'info' }
}

export function durationFilter(minutes) {
  if (!minutes) return '0分钟'
  
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  
  if (hours > 0) {
    return `${hours}小时${mins}分钟`
  }
  return `${mins}分钟`
}
