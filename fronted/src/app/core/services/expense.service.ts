import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap, switchMap } from 'rxjs';
import {
  ExpenseReport,
  ExpenseStatus,
  RoleType,
  ExpenseCategory,
  WorkflowStep,
  ExpenseReportApiResponse,
  ExpenseLineApiResponse,
  User
} from '../models/models';
import { AuthService } from './auth.service';
import { AdminDataService } from './admin-data.service';
import { API_ENDPOINTS } from '../constants/api-endpoints';

export const CURRENCY = 'FCFA';

@Injectable({
  providedIn: 'root'
})
export class ExpenseService {

  public get categories(): ExpenseCategory[] {
    return this.adminData.getCategoryList();
  }

  private expensesSignal = signal<ExpenseReport[]>([]);
  public expenses = this.expensesSignal.asReadonly();

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private adminData: AdminDataService
  ) {
    this.loadExpenses();
  }

  // ------------------------------------------------------------------
  // Mapping : réponse API backend → modèle frontend ExpenseReport
  // ------------------------------------------------------------------
  private mapApiResponse(api: ExpenseReportApiResponse): ExpenseReport {
    let currentStep: WorkflowStep | undefined = undefined;
    if (api.currentStepRole && api.currentStepName) {
      currentStep = {
        id: 0,
        stepOrder: 0,
        requiredRole: api.currentStepRole as RoleType,
        actionName: api.currentStepName
      };
    }

    const employee: User = {
      id: api.employeeId,
      name: api.employeeName,
      email: '',
      role: RoleType.EMPLOYEE,
      department: api.employeeDepartmentName
        ? { id: 0, name: api.employeeDepartmentName }
        : undefined
    };

    const lines = (api.lines ?? []).map((l: ExpenseLineApiResponse) => ({
      id: l.id,
      expenseDate: l.expenseDate,
      category: {
        id: l.categoryId,
        name: l.categoryName,
        code: l.categoryName?.substring(0, 3).toUpperCase() ?? 'AUT',
        maxAmount: l.categoryMaxAmount
      },
      description: l.description,
      amount: l.amount,
      categoryMaxAmount: l.categoryMaxAmount,
      isOverCeiling: l.isOverCeiling,
      ceilingWarningMessage: l.ceilingWarningMessage,
      itineraryFrom: l.itineraryFrom,
      itineraryTo: l.itineraryTo,
      attachments: l.attachments
    }));

    // Le backend retourne LocalDate qui peut être [YYYY, M, D] ou "YYYY-MM-DD"
    const formatDate = (d: any): string => {
      if (Array.isArray(d)) {
        return `${d[0]}-${String(d[1]).padStart(2, '0')}-${String(d[2]).padStart(2, '0')}`;
      }
      return String(d);
    };

    return {
      id: api.id,
      title: api.title,
      description: api.description,
      currency: api.currency,
      dateFrom: formatDate(api.dateFrom),
      dateTo: formatDate(api.dateTo),
      status: api.status,
      employee,
      currentStep,
      rejectionReason: api.rejectionReason,
      rejectedAtStepName: api.rejectedAtStepName,
      isAnyLineOverCeiling: api.isAnyLineOverCeiling,
      lines,
      attachments: api.attachments
    };
  }

  // ------------------------------------------------------------------
  // Chargement principal
  // ------------------------------------------------------------------
  public loadExpenses() {
    this.http.get<any>(API_ENDPOINTS.expenses.list, {
      params: new HttpParams().set('page', '0').set('size', '100')
    }).subscribe({
      next: response => {
        const list: ExpenseReportApiResponse[] = response.content ?? response;
        this.expensesSignal.set(list.map(e => this.mapApiResponse(e)));
      },
      error: err => console.error('Erreur chargement des notes de frais', err)
    });
  }

  public refreshExpenses(): Observable<ExpenseReport[]> {
    return new Observable(observer => {
      this.http.get<any>(API_ENDPOINTS.expenses.list, {
        params: new HttpParams().set('page', '0').set('size', '100')
      }).subscribe({
        next: response => {
          const list: ExpenseReportApiResponse[] = response.content ?? response;
          const mapped = list.map(e => this.mapApiResponse(e));
          this.expensesSignal.set(mapped);
          observer.next(mapped);
          observer.complete();
        },
        error: err => observer.error(err)
      });
    });
  }

  // ------------------------------------------------------------------
  // Lecture
  // ------------------------------------------------------------------
  public getReportTotal(report: ExpenseReport): number {
    return report.lines.reduce((sum, line) => sum + Number(line.amount), 0);
  }

  public getExpensesForUser(): ExpenseReport[] {
    const user = this.authService.currentUser();
    if (!user || user.role === RoleType.ADMIN) return [];
    return this.expenses().filter(r => r.employee.id === user.id);
  }

  public getPendingApprovals(): ExpenseReport[] {
    const user = this.authService.currentUser();
    if (!user) return [];
    return this.expenses().filter(r => 
      r.status === ExpenseStatus.IN_PROGRESS && 
      !!r.currentStep && 
      r.currentStep.requiredRole === user.role
    );
  }

  public getAnalyticsExpenses(departmentId?: number): ExpenseReport[] {
    const all = this.expenses().filter(e =>
      e.status !== ExpenseStatus.DRAFT && e.status !== ExpenseStatus.REJECTED
    );
    if (departmentId == null) return all;
    return all.filter(e => e.employee.department?.id === departmentId);
  }

  public loadPendingApprovals(): Observable<ExpenseReport[]> {
    return new Observable(observer => {
      this.http.get<ExpenseReportApiResponse[]>(API_ENDPOINTS.expenses.pendingApprovals).subscribe({
        next: list => {
          observer.next(list.map(e => this.mapApiResponse(e)));
          observer.complete();
        },
        error: err => observer.error(err)
      });
    });
  }

  // ------------------------------------------------------------------
  // Construction du payload
  // ------------------------------------------------------------------
  private buildPayload(reportData: Partial<ExpenseReport>, linesData: any[]) {
    return {
      title: reportData.title,
      description: reportData.description ?? '',
      currency: CURRENCY,
      dateFrom: reportData.dateFrom,
      dateTo: reportData.dateTo,
      lines: linesData.map(l => ({
        id: l.lineId ?? l.id ?? null,
        expenseDate: l.expenseDate,
        categoryId: +l.categoryId,
        description: l.description,
        amount: +l.amount,
        itineraryFrom: l.itineraryFrom ?? null,
        itineraryTo: l.itineraryTo ?? null
      }))
    };
  }

  // ------------------------------------------------------------------
  // Création / Modification
  // ------------------------------------------------------------------
  public saveExpenseReport(
    reportData: Partial<ExpenseReport>,
    linesData: any[]
  ): Observable<ExpenseReport> {
    const payload = this.buildPayload(reportData, linesData);
    return new Observable<ExpenseReport>(observer => {
      this.http.post<ExpenseReportApiResponse>(API_ENDPOINTS.expenses.draft, payload).subscribe({
        next: r => {
          const mapped = this.mapApiResponse(r);
          this.expensesSignal.update(list => [...list, mapped]);
          observer.next(mapped);
          observer.complete();
        },
        error: err => observer.error(err)
      });
    });
  }

  public createExpenseReport(
    reportData: Partial<ExpenseReport>,
    linesData: any[]
  ): Observable<ExpenseReport> {
    return this.saveExpenseReport(reportData, linesData);
  }

  public updateExpenseReport(
    reportId: number,
    reportData: Partial<ExpenseReport>,
    linesData: any[]
  ): Observable<ExpenseReport> {
    const payload = this.buildPayload(reportData, linesData);
    return new Observable<ExpenseReport>(observer => {
      this.http.put<ExpenseReportApiResponse>(
        API_ENDPOINTS.expenses.update(reportId), payload
      ).subscribe({
        next: r => {
          const mapped = this.mapApiResponse(r);
          this.expensesSignal.update(list => list.map(e => e.id === reportId ? mapped : e));
          observer.next(mapped);
          observer.complete();
        },
        error: err => observer.error(err)
      });
    });
  }

  // ------------------------------------------------------------------
  // Actions workflow
  // ------------------------------------------------------------------
  public submitDraftReport(reportId: number): Observable<any> {
    return this.http.post<void>(API_ENDPOINTS.expenses.submit(reportId), {}).pipe(
      switchMap(() => this.refreshExpenses())
    );
  }

  public approveReport(reportId: number, comment?: string): Observable<any> {
    let params = new HttpParams();
    if (comment) params = params.set('comment', comment);
    return this.http.post<void>(API_ENDPOINTS.expenses.approve(reportId), {}, { params }).pipe(
      switchMap(() => this.refreshExpenses())
    );
  }

  public rejectReport(reportId: number, reason: string): Observable<any> {
    return this.http.post<void>(
      API_ENDPOINTS.expenses.reject(reportId),
      {},
      { params: new HttpParams().set('comment', reason) }
    ).pipe(
      switchMap(() => this.refreshExpenses())
    );
  }

  public markAsPaid(reportId: number): Observable<any> {
    return this.http.post<void>(API_ENDPOINTS.expenses.markPaid(reportId), {}).pipe(
      switchMap(() => this.refreshExpenses())
    );
  }

  public getAnalyticsSummary(departmentId?: number): Observable<any> {
    let params = new HttpParams();
    if (departmentId != null) {
      params = params.set('departmentId', departmentId.toString());
    }
    return this.http.get<any>(API_ENDPOINTS.analytics.summary, { params });
  }

  // ------------------------------------------------------------------
  // Exportations PDF & CSV
  // ------------------------------------------------------------------
  public downloadPdfReport(reportId: number): void {
    const url = API_ENDPOINTS.reports.pdf(reportId);
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob: Blob) => {
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = `Note_de_frais_${reportId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(downloadUrl);
      },
      error: err => console.error('Erreur lors du téléchargement du PDF', err)
    });
  }

  public downloadCsvExport(status?: string, reportId?: number, ids?: number[]) {
    let url = API_ENDPOINTS.reports.csv;
    const params: string[] = [];
    if (ids && ids.length > 0) {
      params.push(`ids=${ids.join(',')}`);
    } else {
      if (status) params.push(`status=${encodeURIComponent(status)}`);
      if (reportId) params.push(`reportId=${reportId}`);
    }
    if (params.length > 0) {
      url += '?' + params.join('&');
    }

    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob: Blob) => {
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = ids && ids.length > 0 ? `Notes_de_frais_selection.csv` : (reportId ? `Note_de_frais_${reportId}_export.csv` : `Notes_de_frais_export.csv`);
        link.click();
        window.URL.revokeObjectURL(downloadUrl);
      },
      error: err => console.error('Erreur lors du téléchargement du CSV', err)
    });
  }

  // ------------------------------------------------------------------
  // OCR & Scan IA
  // ------------------------------------------------------------------
  public scanOcrReceipt(file: File): Observable<{
    isValidReceipt?: boolean;
    errorMessage?: string;
    extractedAmount?: number;
    extractedDate?: string;
    suggestedCategoryId?: number;
    suggestedCategoryName?: string;
    merchantName?: string;
    confidenceScore?: number;
  }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(API_ENDPOINTS.ocr.scan, formData);
  }
}
