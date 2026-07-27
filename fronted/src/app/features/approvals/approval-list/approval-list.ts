import { Component, inject, computed, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExpenseService } from '../../../core/services/expense.service';
import { AuthService } from '../../../core/services/auth.service';
import { ExpenseReport, ExpenseLine, ExpenseAttachment } from '../../../core/models/models';
import { AttachmentService } from '../../../core/services/attachment.service';

@Component({
  selector: 'app-approval-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './approval-list.html',
  styleUrl: './approval-list.css',
})
export class ApprovalList {
  public expenseService = inject(ExpenseService);
  public authService = inject(AuthService);
  public attachmentService = inject(AttachmentService);

  public getGlobalAttachments(reportId: number): any[] {
    const report = this.expenseService.expenses().find(r => r.id === reportId);
    return (report?.attachments ?? []).filter((a: any) => !a.lineId);
  }

  public getLineAttachments(reportId: number, lineId?: number): any[] {
    if (!lineId) return [];
    const report = this.expenseService.expenses().find(r => r.id === reportId);
    return (report?.attachments ?? []).filter((a: any) => a.lineId === lineId);
  }

  public downloadAttachment(att: { id?: number; downloadUrl?: string; originalFileName: string }) {
    if (att.id) {
      this.attachmentService.downloadFile(att.id, att.originalFileName);
    }
  }

  public formatSize(bytes: number): string {
    return this.attachmentService.formatFileSize(bytes);
  }

  public selectedReportIds = signal<Set<number>>(new Set());

  public toggleReportSelection(id: number, event?: Event) {
    if (event) event.stopPropagation();
    const current = new Set(this.selectedReportIds());
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    this.selectedReportIds.set(current);
  }

  public isReportSelected(id: number): boolean {
    return this.selectedReportIds().has(id);
  }

  public toggleSelectAll(event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      const allIds = new Set(this.activeList().map(r => r.id));
      this.selectedReportIds.set(allIds);
    } else {
      this.selectedReportIds.set(new Set());
    }
  }

  public isAllSelected(): boolean {
    const list = this.activeList();
    return list.length > 0 && list.every(r => this.selectedReportIds().has(r.id));
  }

  public exportSelectedCsv() {
    const ids = Array.from(this.selectedReportIds());
    if (ids.length === 0) return;
    this.expenseService.downloadCsvExport(undefined, undefined, ids);
  }

  public downloadSelectedPdf() {
    const ids = Array.from(this.selectedReportIds());
    ids.forEach(id => this.expenseService.downloadPdfReport(id));
  }

  public downloadPdf(reportId: number) {
    this.expenseService.downloadPdfReport(reportId);
  }

  public exportCsv(reportId?: number) {
    const status = this.activeTab() === 'TO_REIMBURSE' ? 'APPROVED' : 'IN_PROGRESS';
    this.expenseService.downloadCsvExport(status, reportId);
  }

  public activeTab = signal<'TO_VALIDATE' | 'TO_REIMBURSE'>('TO_VALIDATE');

  public pendingApprovals = computed(() => {
    return this.expenseService.getPendingApprovals();
  });

  public approvedReports = computed(() => {
    return this.expenseService.expenses().filter(r => r.status === 'APPROVED');
  });

  public activeList = computed(() => {
    if (this.activeTab() === 'TO_REIMBURSE') {
      return this.approvedReports();
    }
    return this.pendingApprovals();
  });

  // Pagination
  public readonly PAGE_SIZE = 10;
  public currentPage = signal(0);

  public paginatedApprovals = computed(() => {
    const all = this.activeList();
    const start = this.currentPage() * this.PAGE_SIZE;
    return all.slice(start, start + this.PAGE_SIZE);
  });

  public totalPages = computed(() => Math.ceil(this.activeList().length / this.PAGE_SIZE));
  public pageNumbers = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i));

  public goToPage(page: number) {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  public selectedReportSignal = signal<ExpenseReport | null>(null);
  public selectedReport = this.selectedReportSignal.asReadonly();

  // Signals pour les modales de confirmation
  public showApproveModal = signal(false);
  public showRejectModal = signal(false);
  public showPayModal = signal(false);
  public rejectionReason = signal('');
  public approveComment = signal('');

  constructor() {
    // Automatically select the first item when the list changes
    effect(() => {
      const list = this.activeList();
      const current = this.selectedReportSignal();
      
      if (list.length > 0) {
        // If current selection is no longer in the list, or none selected, select first
        if (!current || !list.some(r => r.id === current.id)) {
          this.selectedReportSignal.set(list[0]);
        } else {
          // Update details of selected report in case values changed
          const updated = list.find(r => r.id === current.id);
          if (updated) {
            this.selectedReportSignal.set(updated);
          }
        }
      } else {
        this.selectedReportSignal.set(null);
      }
    }, { allowSignalWrites: true });
  }

  public selectReport(report: ExpenseReport) {
    this.selectedReportSignal.set(report);
  }

  public getReportTotal(report: ExpenseReport): number {
    return report.lines.reduce((sum, l) => sum + Number(l.amount || 0), 0);
  }

  public getEmployeeName(report: ExpenseReport | null): string {
    if (!report) return '';
    return report.employee?.name || (report as any).employeeName || '';
  }

  // Méthodes pour l'Approbation
  public openApproveModal() {
    this.approveComment.set('');
    this.showApproveModal.set(true);
  }

  public closeApproveModal() {
    this.showApproveModal.set(false);
  }

  public confirmApprove() {
    const current = this.selectedReport();
    if (current) {
      this.expenseService.approveReport(current.id, this.approveComment() || undefined).subscribe({
        next: () => this.closeApproveModal(),
        error: (err) => alert('Erreur approbation : ' + (err.error?.message ?? err.message))
      });
    }
  }

  // Méthodes pour le Rejet
  public openRejectModal() {
    this.rejectionReason.set('');
    this.showRejectModal.set(true);
  }

  public closeRejectModal() {
    this.showRejectModal.set(false);
  }

  public confirmReject() {
    const current = this.selectedReport();
    const reason = this.rejectionReason().trim();
    if (current && reason) {
      this.expenseService.rejectReport(current.id, reason).subscribe({
        next: () => this.closeRejectModal(),
        error: (err) => alert('Erreur rejet : ' + (err.error?.message ?? err.message))
      });
    }
  }

  // Méthodes pour le Remboursement
  public openPayModal() {
    this.showPayModal.set(true);
  }

  public closePayModal() {
    this.showPayModal.set(false);
  }

  public confirmPay() {
    const current = this.selectedReport();
    if (current) {
      this.expenseService.markAsPaid(current.id).subscribe({
        next: () => this.closePayModal(),
        error: (err) => alert('Erreur lors du remboursement : ' + (err.error?.message ?? err.message))
      });
    }
  }
}
