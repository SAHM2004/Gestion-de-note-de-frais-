import { Component, inject, computed, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ExpenseService } from '../../../core/services/expense.service';
import { AuthService } from '../../../core/services/auth.service';
import { AdminDataService } from '../../../core/services/admin-data.service';
import { ExpenseStatus, ExpenseReport } from '../../../core/models/models';

interface DeptStat {
  shortName: string;
  fullName: string;
  total: number;
  byCategory: Record<string, number>;
}

interface EmployeeStat {
  name: string;
  department: string;
  total: number;
}

@Component({
  selector: 'app-financial-dashboard',
  imports: [CommonModule],
  templateUrl: './financial-dashboard.html',
  styleUrl: './financial-dashboard.css',
})
export class FinancialDashboard implements OnInit {
  public expenseService = inject(ExpenseService);
  public authService = inject(AuthService);
  public adminData = inject(AdminDataService);
  public analyticsSummary = signal<any>(null);

  ngOnInit() {
    this.loadAnalytics();
  }

  public loadAnalytics(departmentId?: number) {
    this.expenseService.getAnalyticsSummary(departmentId).subscribe({
      next: (summary) => {
        this.analyticsSummary.set(summary);
      },
      error: (err) => console.error('Erreur chargement des stats de dashboard financier', err)
    });
  }

  public scopeLabel = computed(() => {
    return this.analyticsSummary()?.scope ?? 'Chargement...';
  });

  public totalReimbursed = computed(() =>
    this.analyticsSummary()?.totalReimbursed ?? 0
  );

  public totalPending = computed(() =>
    this.analyticsSummary()?.totalPending ?? 0
  );

  public rejectionRate = computed(() =>
    this.analyticsSummary()?.rejectionRatePercent ?? 0
  );

  public departmentStats = computed((): DeptStat[] => {
    const list = this.analyticsSummary()?.byDepartment ?? [];
    return list.map((d: any) => ({
      shortName: d.shortName,
      fullName: d.departmentName,
      total: Number(d.total || 0),
      byCategory: d.byCategory ?? {}
    }));
  });

  public topCategories = computed(() => {
    const list = this.analyticsSummary()?.topCategories ?? [];
    return list.map((c: any) => ({
      name: c.categoryName,
      amount: Number(c.amount || 0),
      percent: c.percent
    }));
  });

  public topEmployees = computed((): EmployeeStat[] => {
    const list = this.analyticsSummary()?.topEmployees ?? [];
    return list.map((e: any) => ({
      name: e.employeeName,
      department: e.departmentName,
      total: Number(e.total || 0)
    }));
  });

  public maxDeptTotal = computed(() =>
    Math.max(...this.departmentStats().map(d => d.total), 1)
  );

  private shortDeptName(name: string): string {
    if (name.includes('ALVANET')) return 'ALVANET';
    if (name.includes('SLF')) return 'SLF';
    if (name.includes('SCR')) return 'SCR';
    if (name.includes('Comptabilité')) return 'Comptabilité';
    if (name.includes('RH')) return 'RH';
    if (name.includes('Logistique')) return 'Logistique';
    if (name.includes('Commerciaux')) return 'Commerciaux';
    return name.length > 20 ? name.substring(0, 18) + '…' : name;
  }

  public barHeight(total: number): number {
    return Math.max(20, Math.round((total / this.maxDeptTotal()) * 180));
  }
}
