import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AdminDataService } from '../../../core/services/admin-data.service';
import { RoleType, User, Department, ExpenseCategory } from '../../../core/models/models';

type SettingsTab = 'categories' | 'departments' | 'users';

@Component({
  selector: 'app-settings',
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.html',
  styleUrl: './settings.css',
})
export class Settings implements OnInit {
  public authService = inject(AuthService);
  public adminData = inject(AdminDataService);
  private route = inject(ActivatedRoute);

  public activeTab: SettingsTab = 'users';

  public categories = computed(() => this.adminData.categories());
  public departments = computed(() => this.adminData.departments());
  public users = computed(() => this.adminData.users());

  public showModal = false;
  public modalType: 'category' | 'department' | 'user' = 'user';
  public editMode = false;
  public selectedItemId: number | null = null;

  public catName = '';
  public catCode = '';
  public depName = '';
  public depManagerId = '';
  public userName = '';
  public userEmail = '';
  public userInitialPassword = 'password';
  public userRole = RoleType.EMPLOYEE;
  public userDepId = '';

  public RoleType = RoleType;
  public assignableRoles = this.adminData.getAssignableRoles();

  public readonly PAGE_SIZE = 10;
  public catPage = signal(0);
  public depPage = signal(0);
  public userPage = signal(0);
  public userFilterDepartment = signal<string>('');

  public paginatedCategories = computed(() => {
    const start = this.catPage() * this.PAGE_SIZE;
    return this.categories().slice(start, start + this.PAGE_SIZE);
  });
  public catTotalPages = computed(() => Math.ceil(this.categories().length / this.PAGE_SIZE));
  public catPageNumbers = computed(() => Array.from({ length: this.catTotalPages() }, (_, i) => i));

  public paginatedDepartments = computed(() => {
    const start = this.depPage() * this.PAGE_SIZE;
    return this.departments().slice(start, start + this.PAGE_SIZE);
  });
  public depTotalPages = computed(() => Math.ceil(this.departments().length / this.PAGE_SIZE));
  public depPageNumbers = computed(() => Array.from({ length: this.depTotalPages() }, (_, i) => i));

  public filteredUsers = computed(() => {
    const filter = this.userFilterDepartment();
    let list = this.users();
    if (filter) {
      list = list.filter(u => u.department?.id === +filter);
    }
    return list;
  });

  public paginatedUsers = computed(() => {
    const start = this.userPage() * this.PAGE_SIZE;
    return this.filteredUsers().slice(start, start + this.PAGE_SIZE);
  });
  public userTotalPages = computed(() => Math.ceil(this.filteredUsers().length / this.PAGE_SIZE));
  public userPageNumbers = computed(() => Array.from({ length: this.userTotalPages() }, (_, i) => i));

  ngOnInit() {
    const tab = this.route.snapshot.data['tab'] as SettingsTab;
    if (tab) this.activeTab = tab;
    // Charger les utilisateurs au démarrage
    this.adminData.loadUsers().subscribe();
  }

  public goToPage(tab: SettingsTab, page: number) {
    if (tab === 'categories' && page >= 0 && page < this.catTotalPages()) this.catPage.set(page);
    if (tab === 'departments' && page >= 0 && page < this.depTotalPages()) this.depPage.set(page);
    if (tab === 'users' && page >= 0 && page < this.userTotalPages()) this.userPage.set(page);
  }

  public onUserFilterChange(event: Event) {
    const target = event.target as HTMLSelectElement;
    this.userFilterDepartment.set(target.value);
    this.userPage.set(0);
  }

  public openCreateModal() {
    this.modalType = this.activeTab === 'categories' ? 'category' : this.activeTab === 'departments' ? 'department' : 'user';
    this.editMode = false;
    this.selectedItemId = null;
    this.catName = '';
    this.catCode = '';
    this.depName = '';
    this.depManagerId = this.users()[0]?.id?.toString() || '';
    this.userName = '';
    this.userEmail = '';
    this.userRole = RoleType.EMPLOYEE;
    this.userDepId = '';
    this.userInitialPassword = 'password';
    this.showModal = true;
  }

  public openEditModal(item: ExpenseCategory | Department | User, type: 'category' | 'department' | 'user') {
    this.modalType = type;
    this.editMode = true;
    this.selectedItemId = item.id;

    if (type === 'category') {
      const cat = item as ExpenseCategory;
      this.catName = cat.name;
      this.catCode = cat.code;
    } else if (type === 'department') {
      const dep = item as Department;
      this.depName = dep.name;
      this.depManagerId = dep.manager?.id?.toString() || '';
    } else {
      const u = item as User;
      this.userName = u.name;
      this.userEmail = u.email;
      this.userRole = u.role;
      this.userDepId = u.department?.id?.toString() || '';
    }
    this.showModal = true;
  }

  public closeModal() {
    this.showModal = false;
  }

  public getRoleLabel(role: RoleType): string {
    return this.authService.getRoleLabel(role);
  }

  public saveItem() {
    if (this.modalType === 'category') {
      if (!this.catName || !this.catCode) return;
      const catPayload: any = { name: this.catName, code: this.catCode.toUpperCase() };
      if (this.editMode && this.selectedItemId !== null) {
        catPayload.id = this.selectedItemId;
      }
      this.adminData.saveCategory(
        catPayload,
        this.editMode ? this.selectedItemId ?? undefined : undefined
      ).subscribe({
        error: err => alert('Erreur : ' + (err.error?.message ?? err.message))
      });
    } else if (this.modalType === 'department') {
      if (!this.depName) return;
      const mgrId = this.depManagerId ? +this.depManagerId : null;
      const depPayload: any = { name: this.depName, manager: mgrId ? { id: mgrId } : null };
      if (this.editMode && this.selectedItemId !== null) {
        depPayload.id = this.selectedItemId;
      }
      this.adminData.saveDepartment(
        depPayload,
        this.editMode ? this.selectedItemId ?? undefined : undefined
      ).subscribe({
        error: err => alert('Erreur : ' + (err.error?.message ?? err.message))
      });
    } else if (this.modalType === 'user') {
      if (!this.userName || !this.userEmail) return;
      const depId = this.userDepId ? +this.userDepId : null;
      const userPayload: any = {
        name: this.userName,
        email: this.userEmail,
        role: this.userRole,
        department: depId ? { id: depId } : null,
        ...(this.editMode ? {} : { password: this.userInitialPassword })
      };

      if (this.editMode && this.selectedItemId !== null) {
        this.adminData.updateUser(this.selectedItemId, userPayload).subscribe({
          error: err => alert('Erreur mise à jour : ' + (err.error?.message ?? err.message))
        });
      } else {
        this.adminData.createUser(userPayload).subscribe({
          error: err => alert('Erreur création : ' + (err.error?.message ?? err.message))
        });
      }
    }
    this.closeModal();
  }

  public showDeleteModal = signal(false);
  public deleteTarget = signal<{ type: 'category' | 'department' | 'user'; id: number; label: string } | null>(null);

  public deleteCategory(id: number, label: string = 'cette catégorie') {
    this.deleteTarget.set({ type: 'category', id, label });
    this.showDeleteModal.set(true);
  }

  public deleteDepartment(id: number, label: string = 'ce département') {
    this.deleteTarget.set({ type: 'department', id, label });
    this.showDeleteModal.set(true);
  }

  public deleteUser(id: number, label: string = 'cet utilisateur') {
    this.deleteTarget.set({ type: 'user', id, label });
    this.showDeleteModal.set(true);
  }

  public closeDeleteModal() {
    this.showDeleteModal.set(false);
    this.deleteTarget.set(null);
  }

  public confirmDelete() {
    const target = this.deleteTarget();
    if (!target) return;

    if (target.type === 'category') {
      this.adminData.deleteCategory(target.id).subscribe({
        next: () => this.closeDeleteModal(),
        error: err => alert('Erreur : ' + (err.error?.message ?? err.message))
      });
    } else if (target.type === 'department') {
      this.adminData.deleteDepartment(target.id).subscribe({
        next: () => this.closeDeleteModal(),
        error: err => alert('Erreur : ' + (err.error?.message ?? err.message))
      });
    } else if (target.type === 'user') {
      this.adminData.deleteUser(target.id).subscribe({
        next: () => this.closeDeleteModal(),
        error: err => alert('Erreur : ' + (err.error?.message ?? err.message))
      });
    }
  }
}
