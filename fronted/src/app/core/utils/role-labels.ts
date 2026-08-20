import { RoleType } from '../models/models';

export function getRoleLabel(role: RoleType): string {
  switch (role) {
    case RoleType.EMPLOYEE: return 'Employé';
    case RoleType.MANAGER: return 'Manager';
    case RoleType.TECHNICAL_DIRECTOR: return 'Directeur Technique';
    case RoleType.GENERAL_DIRECTOR: return 'Directeur Général';
    case RoleType.ACCOUNTANT: return 'Comptable';
    case RoleType.ADMIN: return 'Administrateur';
    default: return role;
  }
}

export function isTechnicalDepartment(departmentName?: string): boolean {
  if (!departmentName) return false;
  const nameLower = departmentName.toLowerCase();
  return nameLower.includes('direction technique') || nameLower.includes('alvanet') || nameLower.includes('slf') || nameLower.includes('scr');
}

export function canViewGlobalAnalytics(role: RoleType): boolean {
  return [RoleType.TECHNICAL_DIRECTOR, RoleType.GENERAL_DIRECTOR, RoleType.ACCOUNTANT].includes(role);
}
