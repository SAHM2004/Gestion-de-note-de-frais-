/** Contrat API backend — à utiliser lors de la connexion frontend ↔ backend */
export const API_BASE_URL = 'http://localhost:8080/api';

export const API_ENDPOINTS = {
  auth: {
    login: `${API_BASE_URL}/auth/login`,
    me: `${API_BASE_URL}/auth/me`,
    profile: `${API_BASE_URL}/auth/profile`,
    changePassword: `${API_BASE_URL}/auth/change-password`,
  },
  expenses: {
    list: `${API_BASE_URL}/expenses`,
    pendingApprovals: `${API_BASE_URL}/expenses/pending-approvals`,
    byId: (id: number) => `${API_BASE_URL}/expenses/${id}`,
    draft: `${API_BASE_URL}/expenses/draft`,
    update: (id: number) => `${API_BASE_URL}/expenses/${id}`,
    submit: (id: number) => `${API_BASE_URL}/expenses/${id}/submit`,
    approve: (id: number) => `${API_BASE_URL}/expenses/${id}/approve`,
    reject: (id: number) => `${API_BASE_URL}/expenses/${id}/reject`,
    markPaid: (id: number) => `${API_BASE_URL}/expenses/${id}/mark-paid`,
    addLine: (id: number) => `${API_BASE_URL}/expenses/${id}/lines`,
    updateLine: (lineId: number) => `${API_BASE_URL}/expenses/lines/${lineId}`,
    deleteLine: (lineId: number) => `${API_BASE_URL}/expenses/lines/${lineId}`,
    attachments: (id: number) => `${API_BASE_URL}/expenses/${id}/attachments`,
    uploadAttachment: (id: number) => `${API_BASE_URL}/expenses/${id}/attachments`,
    getAttachment: (id: number) => `${API_BASE_URL}/expenses/attachments/${id}`,
    downloadAttachment: (id: number) => `${API_BASE_URL}/expenses/attachments/${id}/download`,
    deleteAttachment: (id: number) => `${API_BASE_URL}/expenses/attachments/${id}`,
  },
  categories: `${API_BASE_URL}/categories`,
  departments: `${API_BASE_URL}/departments`,
  users: `${API_BASE_URL}/users`,
  references: {
    categories: `${API_BASE_URL}/references/categories`,
    workflowTemplates: `${API_BASE_URL}/references/workflow-templates`,
  },
  analytics: {
    summary: `${API_BASE_URL}/analytics/summary`,
  },
  reports: {
    pdf: (id: number) => `${API_BASE_URL}/reports/expenses/${id}/pdf`,
    csv: `${API_BASE_URL}/reports/expenses/export/csv`,
  },
  ocr: {
    scan: `${API_BASE_URL}/ocr/scan`,
  },
} as const;

export const ALLOWED_ATTACHMENT_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/jpg',
  'image/png',
];

export const ALLOWED_ATTACHMENT_EXTENSIONS = '.pdf,.jpg,.jpeg,.png';

export const MAX_ATTACHMENT_SIZE_BYTES = 10 * 1024 * 1024;
