export const ADMIN_ROLES = ['CONTENT_ADMIN', 'RISK_REVIEWER', 'SECURITY_REVIEWER', 'SUPER_ADMIN']

const ROLE_LABELS = {
  CONTENT_ADMIN: '内容管理员',
  RISK_REVIEWER: '风控审核员',
  SECURITY_REVIEWER: '安全审核员',
  SUPER_ADMIN: '超级管理员',
}

export function adminRoleLabel(role) {
  return ROLE_LABELS[role] ?? role
}

export function canAccessRoles(currentRoles, requiredRoles = []) {
  if (!requiredRoles.length) return currentRoles.some((role) => ADMIN_ROLES.includes(role))
  return currentRoles.includes('SUPER_ADMIN') || requiredRoles.some((role) => currentRoles.includes(role))
}
