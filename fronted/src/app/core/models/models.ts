export enum RoleType {
  EMPLOYEE = 'EMPLOYEE',
  MANAGER = 'MANAGER',
  TECHNICAL_DIRECTOR = 'TECHNICAL_DIRECTOR',
  GENERAL_DIRECTOR = 'GENERAL_DIRECTOR',
  ACCOUNTANT = 'ACCOUNTANT',
  ADMIN = 'ADMIN'
}

export enum ExpenseStatus {
  DRAFT = 'DRAFT',
  IN_PROGRESS = 'IN_PROGRESS',
  APPROVED = 'APPROVED',
  PAID = 'PAID',
  REJECTED = 'REJECTED'
}

export interface Department {
  id: number;
  name: string;
  manager?: User;
  defaultWorkflowTemplate?: WorkflowTemplate;
}

export interface User {
  id: number;
  name: string;
  email: string;
  role: RoleType;
  department?: Department;
  active?: boolean;
  requirePasswordChange?: boolean;
}

export interface WorkflowTemplate {
  id: number;
  name: string;
  steps?: WorkflowStep[];
}

export interface WorkflowStep {
  id: number;
  templateId?: number;
  stepOrder: number;
  requiredRole: RoleType;
  actionName: string;
}

export interface ExpenseLine {
  id?: number;
  expenseDate: string; // ISO string format (YYYY-MM-DD)
  category: ExpenseCategory;
  description: string;
  amount: number;
  categoryMaxAmount?: number;
  isOverCeiling?: boolean;
  ceilingWarningMessage?: string;
  itineraryFrom?: string;
  itineraryTo?: string;
  attachments?: ExpenseAttachment[];
}

export interface ExpenseCategory {
  id: number;
  name: string;
  code: string;
  description?: string;
  maxAmount?: number;
}

export interface ExpenseAttachment {
  id: number;
  reportId: number;
  lineId?: number;
  /** Index de ligne temporaire avant persistance (frontend mock) */
  lineIndex?: number;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  uploadedAt?: string;
  downloadUrl?: string;
  dataUrl?: string;
}

export interface ExpenseReport {
  id: number;
  title: string;
  description: string;
  currency: string;
  dateFrom: string;
  dateTo: string;
  status: ExpenseStatus;
  employee: User;
  currentStep?: WorkflowStep;
  rejectionReason?: string;
  rejectedAtStepName?: string;
  isAnyLineOverCeiling?: boolean;
  lines: ExpenseLine[];
  attachments?: ExpenseAttachment[];
}

/** Réponse API backend (ExpenseReportResponse) */
export interface ExpenseReportApiResponse {
  id: number;
  title: string;
  description: string;
  currency: string;
  dateFrom: string;
  dateTo: string;
  status: ExpenseStatus;
  employeeId: number;
  employeeName: string;
  employeeDepartmentName?: string;
  employeeRole?: string;
  currentStepRole?: string;
  currentStepName?: string;
  rejectionReason?: string;
  rejectedAtStepName?: string;
  isAnyLineOverCeiling?: boolean;
  lines: ExpenseLineApiResponse[];
  attachments?: ExpenseAttachment[];
}

export interface ExpenseLineApiResponse {
  id: number;
  expenseDate: string;
  categoryId: number;
  categoryName: string;
  description: string;
  amount: number;
  categoryMaxAmount?: number;
  isOverCeiling?: boolean;
  ceilingWarningMessage?: string;
  itineraryFrom?: string;
  itineraryTo?: string;
  attachments?: ExpenseAttachment[];
}

// Generic pagination model matching Spring Data Page<T> response
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;       // current page (0-indexed)
  first: boolean;
  last: boolean;
  empty: boolean;
}
