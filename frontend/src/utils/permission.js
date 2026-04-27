import store from '@/store'

const ADMIN_ROLES = ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SYSTEM_ADMIN']

const ADMIN_ONLY_ROUTES = [
  '/batch',
  '/system',
  '/report/config',
  '/approval/config'
]

const BATCH_OPERATION_PATHS = [
  '/api/batch/assign',
  '/api/batch/status',
  '/api/batch/close',
  '/api/batch/resolve',
  '/api/batch/assign/async'
]

export function hasPermission(permission) {
  const permissions = store.getters.permissions || []
  const roles = store.getters.roles || []
  
  if (isAdmin()) {
    return true
  }
  
  if (typeof permission === 'string') {
    return permissions.includes(permission)
  }
  
  if (Array.isArray(permission)) {
    return permission.some(p => permissions.includes(p))
  }
  
  return false
}

export function hasAllPermissions(permissions) {
  const userPermissions = store.getters.permissions || []
  return permissions.every(p => userPermissions.includes(p))
}

export function hasRole(role) {
  const roles = store.getters.roles || []
  
  if (isAdmin()) {
    return true
  }
  
  if (typeof role === 'string') {
    return roles.includes(role)
  }
  
  if (Array.isArray(role)) {
    return role.some(r => roles.includes(r))
  }
  
  return false
}

export function hasLevel(requiredLevel) {
  const userInfo = store.getters.userInfo
  if (!userInfo) return false
  
  const userLevel = userInfo.level || 1
  
  if (isAdmin()) {
    return true
  }
  
  return userLevel >= requiredLevel
}

export function isAdmin() {
  const roles = store.getters.roles || []
  return ADMIN_ROLES.some(role => roles.includes(role))
}

export function isSameDepartment(departmentId) {
  const userInfo = store.getters.userInfo
  if (!userInfo) return false
  
  if (isAdmin()) {
    return true
  }
  
  const userDepartmentId = userInfo.departmentId
  return userDepartmentId === departmentId
}

export function getCurrentUserDepartmentId() {
  const userInfo = store.getters.userInfo
  return userInfo?.departmentId
}

export function getCurrentUserId() {
  const userInfo = store.getters.userInfo
  return userInfo?.id
}

export function canViewTicket(ticket) {
  if (!ticket) return false
  
  if (isAdmin()) {
    return true
  }
  
  const userInfo = store.getters.userInfo
  if (!userInfo) return false
  
  const userId = userInfo.id
  
  if (ticket.creator?.id === userId) {
    return true
  }
  
  if (ticket.assignee?.id === userId) {
    return true
  }
  
  if (isSameDepartment(ticket.department?.id)) {
    return true
  }
  
  return false
}

export function canEditTicket(ticket) {
  if (!ticket) return false
  
  if (isAdmin()) {
    return true
  }
  
  const userInfo = store.getters.userInfo
  if (!userInfo) return false
  
  const userId = userInfo.id
  
  if (ticket.creator?.id === userId) {
    return true
  }
  
  if (ticket.assignee?.id === userId) {
    return true
  }
  
  if (isSameDepartment(ticket.department?.id)) {
    const userLevel = userInfo.level || 1
    return userLevel >= 2
  }
  
  return false
}

export function canDeleteTicket(ticket) {
  if (!ticket) return false
  
  if (isAdmin()) {
    return true
  }
  
  const userInfo = store.getters.userInfo
  if (!userInfo) return false
  
  if (ticket.creator?.id === userInfo.id) {
    return true
  }
  
  return false
}

export function canAssignTicket(ticket) {
  if (!ticket) return false
  
  if (isAdmin()) {
    return true
  }
  
  const userInfo = store.getters.userInfo
  if (!userInfo) return false
  
  if (isSameDepartment(ticket.department?.id)) {
    const userLevel = userInfo.level || 1
    return userLevel >= 2
  }
  
  return false
}

export function canAccessBatchOperations() {
  return isAdmin()
}

export function canAccessAdminRoutes() {
  return isAdmin()
}

export function filterTicketList(tickets) {
  if (!tickets || tickets.length === 0) {
    return []
  }
  
  if (isAdmin()) {
    return tickets
  }
  
  return tickets.filter(ticket => canViewTicket(ticket))
}

export function checkApiAccess(path) {
  if (!path) return { allowed: true }
  
  for (const batchPath of BATCH_OPERATION_PATHS) {
    if (path.includes(batchPath)) {
      if (!isAdmin()) {
        return {
          allowed: false,
          message: '权限不足：批量处理功能仅管理员可使用'
        }
      }
    }
  }
  
  for (const adminRoute of ADMIN_ONLY_ROUTES) {
    if (path.includes('/api' + adminRoute)) {
      if (!isAdmin()) {
        return {
          allowed: false,
          message: '权限不足：该功能仅管理员可访问'
        }
      }
    }
  }
  
  return { allowed: true }
}

export function checkRouteAccess(route) {
  if (!route) return { allowed: true }
  
  const path = route.path || route.fullPath || ''
  
  for (const adminRoute of ADMIN_ONLY_ROUTES) {
    if (path.startsWith(adminRoute)) {
      if (!isAdmin()) {
        return {
          allowed: false,
          message: '权限不足：该页面仅管理员可访问'
        }
      }
    }
  }
  
  if (route.meta?.adminOnly) {
    if (!isAdmin()) {
      return {
        allowed: false,
        message: '权限不足：该页面仅管理员可访问'
      }
    }
  }
  
  if (route.meta?.permission) {
    if (!hasPermission(route.meta.permission)) {
      return {
        allowed: false,
        message: '权限不足：您没有访问该页面的权限'
      }
    }
  }
  
  return { allowed: true }
}

export const PermissionDirective = {
  inserted(el, binding) {
    const { value } = binding
    
    if (!value) {
      return
    }
    
    if (!hasPermission(value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}

export const RoleDirective = {
  inserted(el, binding) {
    const { value } = binding
    
    if (!value) {
      return
    }
    
    if (!hasRole(value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}

export const LevelDirective = {
  inserted(el, binding) {
    const { value } = binding
    
    if (!value) {
      return
    }
    
    if (!hasLevel(value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}

export const AdminOnlyDirective = {
  inserted(el) {
    if (!isAdmin()) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}

export const TicketViewDirective = {
  inserted(el, binding) {
    const { value } = binding
    
    if (!value) {
      return
    }
    
    if (!canViewTicket(value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}

export const TicketEditDirective = {
  inserted(el, binding) {
    const { value } = binding
    
    if (!value) {
      return
    }
    
    if (!canEditTicket(value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}

export default {
  hasPermission,
  hasAllPermissions,
  hasRole,
  hasLevel,
  isAdmin,
  isSameDepartment,
  getCurrentUserDepartmentId,
  getCurrentUserId,
  canViewTicket,
  canEditTicket,
  canDeleteTicket,
  canAssignTicket,
  canAccessBatchOperations,
  canAccessAdminRoutes,
  filterTicketList,
  checkApiAccess,
  checkRouteAccess
}
