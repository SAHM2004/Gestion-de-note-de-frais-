import { Component, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ExpenseService } from '../../../core/services/expense.service';
import { AuthService } from '../../../core/services/auth.service';
import { ExpenseStatus, ExpenseReport, ExpenseAttachment } from '../../../core/models/models';
import { AttachmentService } from '../../../core/services/attachment.service';

@Component({
  selector: 'app-expense-list',
  imports: [CommonModule],
  templateUrl: './expense-list.html'
})
export class ExpenseListComponent {
  public expenseService = inject(ExpenseService);
  public authService = inject(AuthService);
  private router = inject(Router);
  public attachmentService = inject(AttachmentService);

  public getGlobalAttachments(report: ExpenseReport): ExpenseAttachment[] {
    return (report.attachments ?? []).filter(a => !a.lineId);
  }

  public getLineAttachments(report: ExpenseReport, lineId?: number): ExpenseAttachment[] {
    if (!lineId) return [];
    return (report.attachments ?? []).filter(a => a.lineId === lineId);
  }

  public downloadAttachment(att: ExpenseAttachment) {
    if (att.id) {
      this.attachmentService.downloadFile(att.id, att.originalFileName);
    }
  }

  public formatSize(bytes: number): string {
    return this.attachmentService.formatFileSize(bytes);
  }

  // Selected report details modal
  public selectedReportForView = signal<ExpenseReport | null>(null);

  public openReportDetails(report: ExpenseReport) {
    this.selectedReportForView.set(report);
  }

  public closeReportDetails() {
    this.selectedReportForView.set(null);
  }

  public getWorkflowStepsForReport(report: ExpenseReport) {
    const isTechnical = report.employee.department?.name.toLowerCase().includes('info') || report.employee.department?.name.toLowerCase().includes('tech');
    
    const stepsConfig = isTechnical 
      ? [
          { name: 'Validation Manager', role: 'MANAGER' },
          { name: 'Validation Directeur Technique', role: 'TECHNICAL_DIRECTOR' },
          { name: 'Validation Directeur Général', role: 'GENERAL_DIRECTOR' },
          { name: 'Validation Comptabilité', role: 'ACCOUNTANT' }
        ]
      : [
          { name: 'Validation Manager', role: 'MANAGER' },
          { name: 'Validation Directeur Général', role: 'GENERAL_DIRECTOR' },
          { name: 'Validation Comptabilité', role: 'ACCOUNTANT' }
        ];

    if (report.status === ExpenseStatus.DRAFT) {
      return stepsConfig.map(s => ({ ...s, status: 'pending' }));
    }

    if (report.status === ExpenseStatus.REJECTED) {
      const rejectedIndex = stepsConfig.findIndex(s => s.name === report.rejectedAtStepName);
      if (rejectedIndex !== -1) {
        return stepsConfig.map((s, idx) => {
          if (idx < rejectedIndex) return { ...s, status: 'completed' };
          if (idx === rejectedIndex) return { ...s, status: 'rejected' };
          return { ...s, status: 'pending' };
        });
      } else {
        // Fallback if rejectedAtStepName is missing
        return stepsConfig.map((s, idx) => {
          if (idx === 0) return { ...s, status: 'rejected' };
          return { ...s, status: 'pending' };
        });
      }
    }

    if (report.status === ExpenseStatus.APPROVED || report.status === ExpenseStatus.PAID) {
      return stepsConfig.map(s => ({ ...s, status: 'completed' }));
    }

    let foundCurrent = false;
    return stepsConfig.map(s => {
      if (report.currentStep && s.role === report.currentStep.requiredRole) {
        foundCurrent = true;
        return { ...s, status: 'current' };
      }
      return { ...s, status: (foundCurrent ? 'pending' : 'completed') };
    });
  }

  public userExpenses = computed(() => {
    return this.expenseService.getExpensesForUser();
  });

  // Pagination
  public readonly PAGE_SIZE = 10;
  public currentPage = signal(0);

  public paginatedExpenses = computed(() => {
    const all = this.userExpenses();
    const start = this.currentPage() * this.PAGE_SIZE;
    return all.slice(start, start + this.PAGE_SIZE);
  });

  public totalPages = computed(() => Math.ceil(this.userExpenses().length / this.PAGE_SIZE));
  public pageNumbers = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i));

  public goToPage(page: number) {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  public getReportTotal(report: ExpenseReport): number {
    return report.lines.reduce((sum, line) => sum + Number(line.amount), 0);
  }

  public getStatusClass(status: ExpenseStatus): string {
    switch (status) {
      case ExpenseStatus.DRAFT: return 'bg-gray-100 text-gray-700';
      case ExpenseStatus.IN_PROGRESS: return 'bg-orange-100 text-orange-700';
      case ExpenseStatus.APPROVED: return 'bg-blue-100 text-blue-700';
      case ExpenseStatus.PAID: return 'bg-green-100 text-green-700';
      case ExpenseStatus.REJECTED: return 'bg-red-100 text-red-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  }

  public getStatusLabel(status: ExpenseStatus): string {
    switch (status) {
      case ExpenseStatus.DRAFT: return 'Brouillon';
      case ExpenseStatus.IN_PROGRESS: return 'Soumis';
      case ExpenseStatus.APPROVED: return 'Approuvé';
      case ExpenseStatus.PAID: return 'Remboursé';
      case ExpenseStatus.REJECTED: return 'Rejeté';
      default: return status;
    }
  }

  public showSubmitModal = signal(false);
  public showPayModal = signal(false);
  public pendingReportId = signal<number | null>(null);

  public submitDraft(reportId: number) {
    this.openSubmitModal(reportId);
  }

  public openSubmitModal(reportId: number) {
    this.pendingReportId.set(reportId);
    this.showSubmitModal.set(true);
  }

  public closeSubmitModal() {
    this.showSubmitModal.set(false);
    this.pendingReportId.set(null);
  }

  public confirmSubmitDraft() {
    const reportId = this.pendingReportId();
    if (reportId) {
      this.expenseService.submitDraftReport(reportId).subscribe({
        next: () => {
          this.closeSubmitModal();
          const updated = this.userExpenses().find(r => r.id === reportId);
          if (updated) {
            this.selectedReportForView.set(updated);
          } else {
            this.closeReportDetails();
          }
        },
        error: (err) => alert('Erreur soumission : ' + (err.error?.message ?? err.message))
      });
    }
  }

  public editReport(reportId: number) {
    this.closeReportDetails();
    this.router.navigate(['/expenses/edit', reportId]);
  }

  public markAsPaid(reportId: number) {
    this.openPayModal(reportId);
  }

  public openPayModal(reportId: number) {
    this.pendingReportId.set(reportId);
    this.showPayModal.set(true);
  }

  public closePayModal() {
    this.showPayModal.set(false);
    this.pendingReportId.set(null);
  }

  public confirmPay() {
    const reportId = this.pendingReportId();
    if (reportId) {
      this.expenseService.markAsPaid(reportId).subscribe({
        next: (updated) => {
          this.closePayModal();
          this.selectedReportForView.set(updated);
        },
        error: (err) => alert('Erreur lors du remboursement : ' + (err.error?.message ?? err.message))
      });
    }
  }
}
