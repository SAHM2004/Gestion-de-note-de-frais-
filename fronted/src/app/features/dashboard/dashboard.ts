import { Component, inject, computed, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ExpenseService } from '../../core/services/expense.service';
import { AuthService } from '../../core/services/auth.service';
import { AdminDataService } from '../../core/services/admin-data.service';
import { ExpenseStatus, ExpenseReport, RoleType } from '../../core/models/models';

import { AttachmentService } from '../../core/services/attachment.service';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  public expenseService = inject(ExpenseService);
  public authService = inject(AuthService);
  public adminData = inject(AdminDataService);
  private router = inject(Router);
  public attachmentService = inject(AttachmentService);

  public RoleType = RoleType;
  public analyticsSummary = signal<any>(null);

  ngOnInit() {
    this.expenseService.refreshExpenses().subscribe();
    this.loadAnalytics();
  }

  public loadAnalytics() {
    this.expenseService.getPersonalSummary().subscribe({
      next: (summary) => {
        this.analyticsSummary.set(summary);
      },
      error: (err) => console.error('Erreur chargement des stats personnelles', err)
    });
  }

  public getGlobalAttachments(report: ExpenseReport): import('../../core/models/models').ExpenseAttachment[] {
    return (report.attachments ?? []).filter(a => !a.lineId);
  }

  public getLineAttachments(report: ExpenseReport, lineId?: number): import('../../core/models/models').ExpenseAttachment[] {
    if (!lineId) return [];
    return (report.attachments ?? []).filter(a => a.lineId === lineId);
  }

  public downloadAttachment(att: import('../../core/models/models').ExpenseAttachment) {
    if (att.id) {
      this.attachmentService.downloadFile(att.id, att.originalFileName);
    }
  }

  public formatSize(bytes: number): string {
    return this.attachmentService.formatFileSize(bytes);
  }

  public getRoleCount(role: RoleType): number {
    return this.adminData.users().filter(u => u.role === role).length;
  }

  public adminStats = computed(() => {
    const users = this.adminData.users();
    const departments = this.adminData.departments();
    const managerIds = new Set<number>();
    users.filter(u => u.role === RoleType.MANAGER).forEach(u => managerIds.add(u.id));
    departments.forEach(d => {
      if (d.manager && d.manager.id) {
        managerIds.add(d.manager.id);
      }
    });

    return {
      users: users.length,
      departments: departments.length,
      categories: this.adminData.categories().length,
      managers: managerIds.size,
      employees: users.filter(u => u.role === RoleType.EMPLOYEE).length
    };
  });

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

  public totalSubmitted = computed(() => {
    return this.analyticsSummary()?.totalSubmitted ?? 0;
  });

  public totalPending = computed(() => {
    return this.analyticsSummary()?.totalPending ?? 0;
  });

  public totalApproved = computed(() => {
    return this.analyticsSummary()?.totalApproved ?? 0;
  });

  public totalPaid = computed(() => {
    return this.analyticsSummary()?.totalPaid ?? 0;
  });

  public totalRejected = computed(() => {
    return this.analyticsSummary()?.totalRejected ?? 0;
  });

  public categoryStats = computed(() => {
    const topCats = this.analyticsSummary()?.topCategories ?? [];
    let totalOverall = 0;
    const result = [];
    const palette = [
      { hex: '#3b82f6', twClass: 'bg-blue-500' }, 
      { hex: '#10b981', twClass: 'bg-emerald-500' }, 
      { hex: '#f59e0b', twClass: 'bg-amber-500' }, 
      { hex: '#8b5cf6', twClass: 'bg-violet-500' }, 
      { hex: '#ec4899', twClass: 'bg-pink-500' }, 
      { hex: '#64748b', twClass: 'bg-slate-500' }, 
    ];

    let i = 0;
    for (const cat of topCats) {
      const amount = Number(cat.amount || 0);
      totalOverall += amount;
      result.push({
         name: cat.categoryName,
         amount: amount,
         percentage: cat.percent,
         colorClass: palette[i % palette.length].twClass,
         hex: palette[i % palette.length].hex
      });
      i++;
    }

    let gradientString = '';
    if (result.length > 0 && totalOverall > 0) {
      let currentPercent = 0;
      const stops = result.map(stat => {
         const start = currentPercent;
         currentPercent += stat.percentage;
         return `${stat.hex} ${start}% ${currentPercent}%`;
      });
      gradientString = `conic-gradient(${stops.join(', ')})`;
    } else {
      gradientString = 'conic-gradient(#e2e8f0 0% 100%)';
    }
    
    return { data: result, totalOverall, gradientString };
  });

  // Selected report details modal
  public selectedReportForView = signal<ExpenseReport | null>(null);

  public openReportDetails(report: ExpenseReport) {
    this.selectedReportForView.set(report);
  }

  public closeReportDetails() {
    this.selectedReportForView.set(null);
  }

  public getWorkflowStepsForReport(report: ExpenseReport): { name: string, role: RoleType, status: 'completed' | 'current' | 'pending' | 'rejected' }[] {
    let stepsConfig: { name: string; role: RoleType }[] = [
      { name: 'Validation Manager', role: RoleType.MANAGER },
      { name: 'Validation Directeur Technique', role: RoleType.TECHNICAL_DIRECTOR },
      { name: 'Validation Directeur Général', role: RoleType.GENERAL_DIRECTOR },
      { name: 'Validation Comptabilité', role: RoleType.ACCOUNTANT }
    ];

    const deptName = report.employee.department?.name ?? '';
    const isTechnical = deptName.includes('ALVANET') || deptName.includes('SLF') || deptName.includes('SCR');
    if (!isTechnical) {
      stepsConfig = stepsConfig.filter(s => s.role !== RoleType.TECHNICAL_DIRECTOR);
    }

    const employeeRole = report.employee.role;

    if (employeeRole === RoleType.GENERAL_DIRECTOR) {
      stepsConfig = stepsConfig.filter(s => ![RoleType.MANAGER, RoleType.TECHNICAL_DIRECTOR, RoleType.GENERAL_DIRECTOR].includes(s.role));
    } else if (employeeRole === RoleType.TECHNICAL_DIRECTOR) {
      stepsConfig = stepsConfig.filter(s => ![RoleType.MANAGER, RoleType.TECHNICAL_DIRECTOR].includes(s.role));
    } else if (employeeRole === RoleType.MANAGER) {
      stepsConfig = stepsConfig.filter(s => s.role !== RoleType.MANAGER);
    }

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

    // IN_PROGRESS
    let foundCurrent = false;
    return stepsConfig.map(s => {
      if (report.currentStep && s.role === report.currentStep.requiredRole) {
        foundCurrent = true;
        return { ...s, status: 'current' };
      }
      return { ...s, status: (foundCurrent ? 'pending' : 'completed') };
    });
  }

  public getReportTotal(report: ExpenseReport): number {
    return this.expenseService.getReportTotal(report);
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

  public submitDraft(reportId: number) {
    this.expenseService.submitDraftReport(reportId).subscribe({
      next: () => {
        const updated = this.userExpenses().find(r => r.id === reportId);
        if (updated) {
          this.selectedReportForView.set(updated);
        } else {
          this.closeReportDetails();
        }
      },
      error: (err) => alert('Erreur : ' + (err.error?.message ?? err.message))
    });
  }

  public editReport(reportId: number) {
    this.closeReportDetails();
    this.router.navigate(['/expenses/edit', reportId]);
  }

  public showPayModal = signal(false);

  public openPayModal() {
    this.showPayModal.set(true);
  }

  public closePayModal() {
    this.showPayModal.set(false);
  }

  public confirmPay() {
    const reportId = this.selectedReportForView()?.id;
    if (reportId) {
      this.expenseService.markAsPaid(reportId).subscribe({
        next: (updated) => {
          this.selectedReportForView.set(updated);
          this.loadAnalytics();
          this.closePayModal();
        },
        error: (err) => alert('Erreur lors du remboursement : ' + (err.error?.message ?? err.message))
      });
    }
  }
}
