import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ExpenseService } from '../../../core/services/expense.service';
import { AttachmentService } from '../../../core/services/attachment.service';
import { AuthService } from '../../../core/services/auth.service';
import { AdminDataService } from '../../../core/services/admin-data.service';
import { ExpenseCategory, ExpenseStatus, ExpenseAttachment, RoleType } from '../../../core/models/models';
import { ALLOWED_ATTACHMENT_EXTENSIONS } from '../../../core/constants/api-endpoints';

interface TempLine {
  localId: number;
  lineId?: number;
  expenseDate: string;
  categoryId: string;
  description: string;
  amount: number;
}

@Component({
  selector: 'app-expense-create',
  imports: [FormsModule, CommonModule],
  templateUrl: './expense-create.html',
  styleUrl: './expense-create.css',
})
export class ExpenseCreate implements OnInit {
  private expenseService = inject(ExpenseService);
  private attachmentService = inject(AttachmentService);
  public authService = inject(AuthService);
  private adminData = inject(AdminDataService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  public ExpenseStatus = ExpenseStatus;
  public allowedExtensions = ALLOWED_ATTACHMENT_EXTENSIONS;

  public title: string = '';
  public description: string = '';
  public dateFrom: string = new Date().toISOString().substring(0, 10);
  public dateTo: string = new Date().toISOString().substring(0, 10);

  public categories = this.adminData.categories;
  public lines: TempLine[] = [];
  public reportAttachments: ExpenseAttachment[] = [];
  public pendingAttachments: { file: File, localLineId?: number, localId: number }[] = [];
  private nextPendingId = 1;
  public isEditMode: boolean = false;
  public reportId: number | null = null;

  // Signals pour les modales de confirmation et succès
  public showConfirmModal = signal(false);
  public showSuccessModal = signal(false);
  public showDeleteModal = signal(false);
  
  public modalTitle = signal('');
  public modalMessage = signal('');
  public successTitle = signal('');
  public successMessage = signal('');
  public pendingStatus = signal<ExpenseStatus | null>(null);
  public deleteTarget = signal<{ type: 'line' | 'attachment'; id: any } | null>(null);
  private nextLineLocalId = 1;

  public attachmentError: string | null = null;

  constructor() {
  }

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.reportId = +idParam;
      this.isEditMode = true;
      this.loadExistingReport(this.reportId);
    } else {
      this.addLine();
    }
  }

  public get workflowSteps(): { name: string; role: RoleType }[] {
    const deptName = this.authService.currentUser()?.department?.name ?? '';
    const isTechnical = this.adminData.isTechnicalDepartment({ id: 0, name: deptName });
    if (isTechnical) {
      return [
        { name: 'Validation Manager', role: RoleType.MANAGER },
        { name: 'Validation Directeur Technique', role: RoleType.TECHNICAL_DIRECTOR },
        { name: 'Validation Directeur Général', role: RoleType.GENERAL_DIRECTOR },
        { name: 'Validation Comptabilité', role: RoleType.ACCOUNTANT },
      ];
    }
    return [
      { name: 'Validation Manager', role: RoleType.MANAGER },
      { name: 'Validation Directeur Général', role: RoleType.GENERAL_DIRECTOR },
      { name: 'Validation Comptabilité', role: RoleType.ACCOUNTANT },
    ];
  }

  private loadExistingReport(id: number) {
    const report = this.expenseService.getExpensesForUser().find(r => r.id === id);
    if (report) {
      this.title = report.title;
      this.description = report.description;
      this.dateFrom = report.dateFrom;
      this.dateTo = report.dateTo;
      this.lines = report.lines.map(line => ({
        localId: this.nextLineLocalId++,
        lineId: line.id,
        expenseDate: line.expenseDate,
        categoryId: line.category.id.toString(),
        description: line.description,
        amount: line.amount,
      }));
      // Charger les pièces jointes depuis le backend
      this.attachmentService.getByReportId(id).subscribe({
        next: atts => this.reportAttachments = atts,
        error: () => this.reportAttachments = []
      });
    } else {
      // Peut arriver si les données ne sont pas encore chargées : recharger
      this.expenseService.refreshExpenses().subscribe(() => {
        const r = this.expenseService.getExpensesForUser().find(r => r.id === id);
        if (r) { this.loadExistingReport(id); }
        else { alert('Note de frais introuvable'); this.router.navigate(['/dashboard']); }
      });
    }
  }

  public addLine() {
    this.lines.push({
      localId: this.nextLineLocalId++,
      expenseDate: new Date().toISOString().substring(0, 10),
      categoryId: this.categories()[0]?.id?.toString() || '1',
      description: '',
      amount: 50,
    });
  }

  public removeLine(index: number) {
    if (this.lines.length > 1) {
      this.lines.splice(index, 1);
    } else {
      this.lines[0] = {
        localId: this.nextLineLocalId++,
        expenseDate: new Date().toISOString().substring(0, 10),
        categoryId: this.categories()[0]?.id?.toString() || '1',
        description: '',
        amount: 50,
      };
    }
  }

  public getLineAttachments(lineIndex: number): any[] {
    const line = this.lines[lineIndex];
    const existing = this.reportAttachments.filter(a =>
      (line.lineId != null && a.lineId === line.lineId) || ((a as any).lineIndex === line.localId)
    );
    const pending = this.pendingAttachments.filter(a => a.localLineId === line.localId).map(p => ({
       id: 'pending_' + p.localId,
       originalFileName: p.file.name,
       fileSize: p.file.size,
       contentType: p.file.type,
       isPending: true
    }));
    return [...existing, ...pending];
  }

  public async onReportFilesSelected(event: Event) {
    await this.handleFiles(event, undefined);
  }

  public async onLineFilesSelected(event: Event, lineIndex: number) {
    await this.handleFiles(event, lineIndex);
  }

  private async handleFiles(event: Event, lineIndex?: number) {
    this.attachmentError = null;
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (!files?.length) return;

    const line = lineIndex != null ? this.lines[lineIndex] : undefined;

    if (!this.reportId || (line && line.lineId == null)) {
      for (const file of Array.from(files)) {
        const error = this.attachmentService.validateFile(file);
        if (error) {
          this.attachmentError = error;
          continue;
        }
        this.pendingAttachments.push({ file, localLineId: line?.localId, localId: this.nextPendingId++ });
      }
      input.value = '';
      return;
    }

    for (const file of Array.from(files)) {
      try {
        const att = await this.attachmentService.addAttachment(
          this.reportId,
          file,
          line?.lineId
        );
        this.reportAttachments.push(att);
      } catch (e: any) {
        this.attachmentError = e.message;
      }
    }
    input.value = '';
  }

  public removeAttachment(id: number | string) {
    if (typeof id === 'string' && id.startsWith('pending_')) {
      const localId = parseInt(id.replace('pending_', ''), 10);
      this.pendingAttachments = this.pendingAttachments.filter(p => p.localId !== localId);
      return;
    }
    
    this.attachmentService.deleteAttachment(id as number).subscribe({
      next: () => {
        this.reportAttachments = this.reportAttachments.filter(a => a.id !== id);
      },
      error: (err) => console.error('Erreur suppression pièce jointe', err)
    });
  }

  public get totalAmount(): number {
    return this.lines.reduce((sum, line) => sum + Number(line.amount || 0), 0);
  }

  public get totalAttachmentsCount(): number {
    return this.reportAttachments.length + this.pendingAttachments.length;
  }

  public get globalAttachments(): any[] {
    const existing = this.reportAttachments.filter(a => !a.lineId && a.lineIndex == null);
    const pending = this.pendingAttachments.filter(a => a.localLineId == null).map(p => ({
       id: 'pending_' + p.localId,
       originalFileName: p.file.name,
       fileSize: p.file.size,
       contentType: p.file.type,
       isPending: true
    }));
    return [...existing, ...pending];
  }

  public formatSize(bytes: number): string {
    return this.attachmentService.formatFileSize(bytes);
  }

  public prepareSubmit(status: ExpenseStatus) {
    this.onSubmit(status);
  }

  public onSubmit(status: ExpenseStatus) {
    this.attachmentError = null;

    if (!this.title.trim()) {
      alert('Veuillez donner un nom à votre note de frais.');
      return;
    }

    for (let i = 0; i < this.lines.length; i++) {
      const amt = this.lines[i].amount;
      if (amt == null || amt < 50) {
        alert(`La ligne ${i + 1} a un montant invalide. Le montant doit être d'au moins 50 FCFA.`);
        return;
      }
    }

    this.pendingStatus.set(status);
    if (status === ExpenseStatus.DRAFT) {
      this.modalTitle.set('Enregistrer comme brouillon');
      this.modalMessage.set('Voulez-vous vraiment enregistrer cette note de frais comme brouillon ?');
    } else {
      this.modalTitle.set('Soumettre la note de frais');
      this.modalMessage.set('Voulez-vous vraiment soumettre cette note de frais pour validation ?');
    }
    this.showConfirmModal.set(true);
  }

  public closeConfirmModal() {
    this.showConfirmModal.set(false);
  }

  public executeSubmit() {
    this.showConfirmModal.set(false);
    const status = this.pendingStatus();
    if (!status) return;

    const reportData = {
      title: this.title,
      description: this.description,
      dateFrom: this.dateFrom,
      dateTo: this.dateTo,
    };

    if (this.isEditMode && this.reportId !== null) {
      this.expenseService.updateExpenseReport(this.reportId, reportData, this.lines).subscribe({
        next: async (updated) => {
          await this.uploadPendingAttachments(updated);
          this.finalizeSubmit(updated.id, status);
        },
        error: (err) => alert('Erreur : ' + (err.error?.message ?? err.message))
      });
    } else {
      this.expenseService.saveExpenseReport(reportData, this.lines).subscribe({
        next: async (created) => {
          await this.uploadPendingAttachments(created);
          this.finalizeSubmit(created.id, status);
        },
        error: (err) => alert('Erreur : ' + (err.error?.message ?? err.message))
      });
    }
  }

  private async uploadPendingAttachments(report: any) {
    if (this.pendingAttachments.length === 0) return;
    const createdLines = report.lines || [];
    for (const pending of this.pendingAttachments) {
      let targetLineId: number | undefined = undefined;
      if (pending.localLineId != null) {
        const lineIndex = this.lines.findIndex(l => l.localId === pending.localLineId);
        if (lineIndex !== -1 && lineIndex < createdLines.length) {
          targetLineId = createdLines[lineIndex].id;
        }
      }
      try {
        await this.attachmentService.addAttachment(report.id, pending.file, targetLineId);
      } catch(e) {
        console.error("Erreur upload pièce jointe", e);
      }
    }
    this.pendingAttachments = [];
  }

  private finalizeSubmit(reportId: number, status: ExpenseStatus) {
    this.expenseService.refreshExpenses().subscribe({
      next: () => {
        if (status === ExpenseStatus.IN_PROGRESS) {
          this.expenseService.submitDraftReport(reportId).subscribe({
            next: () => {
              this.successTitle.set('Note de frais soumise !');
              this.successMessage.set('Votre note de frais a été transmise avec succès dans le circuit de validation.');
              this.showSuccessModal.set(true);
            },
            error: (err) => alert('Erreur lors de la soumission : ' + (err.error?.message ?? err.message))
          });
        } else {
          this.successTitle.set('Brouillon enregistré !');
          this.successMessage.set('Votre note de frais a été enregistrée comme brouillon avec succès.');
          this.showSuccessModal.set(true);
        }
      },
      error: (err) => {
        console.error('Erreur lors de l\'actualisation des notes', err);
        this.router.navigate(['/expenses']);
      }
    });
  }

  public goToExpenses() {
    this.showSuccessModal.set(false);
    this.router.navigate(['/expenses']);
  }

  public cancel() {
    this.router.navigate(['/dashboard']);
  }
}
