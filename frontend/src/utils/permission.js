import store from '@/store'

export function hasPermission(permission) {
  const permissions = store.getters.permissions || []
  const roles = store.getters.roles || []
  
  if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_SUPER_ADMIN')) {
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
  
  if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_SUPER_ADMIN')) {
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
  
  if (hasRole(['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'])) {
    return true
  }
  
  return userLevel >= requiredLevel
}

export function isSameDepartment(departmentId) {
  const userInfo = store.getters.userInfo
  if (!userInfo) return false
  
  if (hasRole(['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'])) {
    return true
  }
  
  const userDepartmentId = userInfo.departmentId
  return userDepartmentId === departmentId
}

export function canViewTicket(ticket) {
  if (!ticket) return false
  
  if (hasRole(['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'])) {
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
  
  if (hasRole(['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'])) {
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
  
  if (hasPermission(['TICKET_EDIT', 'TICKET_ASSIGN', 'TICKET_EDIT_STATUS'])) {
    if (isSameDepartment(ticket.department?.id)) {
      return true
    }
  }
  
  return false
}

export function canAssignTicket(ticket) {
  if (!ticket) return false
  
  if (hasRole(['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'])) {
    return true
  }
  
  if (!hasPermission('TICKET_ASSIGN')) {
    return false
  }
  
  if (isSameDepartment(ticket.department?.id)) {
    return true
  }
  
  return false
}

export function canDeleteTicket(ticket) {
  if (!ticket) return false
  
  if (hasRole(['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'])) {
    return true
  }
  
  const userInfo = store.getters.userInfo
  if (!userInfo) return false
  
  if (ticket.creator?.id === userInfo.id) {
    return true
  }
  
  return false
}

export function filterTicketList(tickets) {
  if (!tickets || tickets.length === 0) {
    return []
  }
  
  if (hasRole(['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'])) {
    return tickets
  }
  
  return tickets.filter(ticket => canViewTicket(ticket))
}

export function getCurrentUserDepartment() {
  const userInfo = store.getters.userInfo
  return userInfo?.departmentId
}

export function getCurrentUserId() {
  const userInfo = store.getters.userInfo
  return userInfo?.id
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

export default {
  hasPermission,
  hasAllPermissions,
  hasRole,
  hasLevel,
  isSameDepartment,
  canViewTicket,
  canEditTicket,
  canAssignTicket,
  canDeleteTicket,
  filterTicketList,
  getCurrentUserDepartment,
  getCurrentUserId
}
