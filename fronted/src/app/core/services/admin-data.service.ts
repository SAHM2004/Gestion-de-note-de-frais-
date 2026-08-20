import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, switchMap } from 'rxjs';
import { User, Department, ExpenseCategory, RoleType } from '../models/models';
import { API_ENDPOINTS } from '../constants/api-endpoints';

@Injectable({ providedIn: 'root' })
export class AdminDataService {
  private categoriesSignal = signal<ExpenseCategory[]>([]);
  private departmentsSignal = signal<Department[]>([]);
  private usersSignal = signal<User[]>([]);

  public categories = this.categoriesSignal.asReadonly();
  public departments = this.departmentsSignal.asReadonly();
  public users = this.usersSignal.asReadonly();

  constructor(private http: HttpClient) {
    this.loadAll();
  }

  public loadAll() {
    this.loadCategories();
    this.loadDepartments();
    this.loadUsers().subscribe();
  }

  // ---- Catégories ----

  public loadCategories() {
    this.http.get<any>(API_ENDPOINTS.references.categories).subscribe({
      next: response => {
        const cats: ExpenseCategory[] = response.content ?? response;
        this.categoriesSignal.set(cats);
      },
      error: err => console.error('Erreur chargement catégories', err)
    });
  }

  public getCategoryList(): ExpenseCategory[] {
    return [...this.categoriesSignal()];
  }

  // ---- Départements ----

  public loadDepartments() {
    this.http.get<any>(`${API_ENDPOINTS.departments}?size=1000`).subscribe({
      next: response => {
        const depts: Department[] = response.content ?? response;
        this.departmentsSignal.set(depts);
      },
      error: err => console.error('Erreur chargement départements', err)
    });
  }

  // ---- Utilisateurs ----

  public loadUsers(): Observable<any> {
    return this.http.get<any>(`${API_ENDPOINTS.users.base}?size=1000`).pipe(
      tap(response => {
        // Le backend retourne une Page<User>
        const userList: User[] = response.content ?? response;
        this.usersSignal.set(userList);
      })
    );
  }

  public createUser(user: Partial<User> & { password?: string }): Observable<any> {
    return this.http.post<User>(API_ENDPOINTS.users.base, user).pipe(
      switchMap(() => {
        this.loadDepartments();
        return this.loadUsers();
      })
    );
  }

  public updateUser(id: number, user: Partial<User>): Observable<any> {
    return this.http.put<User>(`${API_ENDPOINTS.users.base}/${id}`, user).pipe(
      switchMap(() => {
        this.loadDepartments();
        return this.loadUsers();
      })
    );
  }

  public deleteUser(id: number): Observable<any> {
    return this.http.delete<void>(`${API_ENDPOINTS.users.base}/${id}`).pipe(
      switchMap(() => {
        this.loadDepartments();
        return this.loadUsers();
      })
    );
  }

  public toggleUserActive(id: number): Observable<any> {
    return this.http.put<User>(API_ENDPOINTS.users.toggleActive(id), {}).pipe(
      switchMap(() => {
        this.loadDepartments();
        return this.loadUsers();
      })
    );
  }

  // ---- Catégories (admin) ----

  public saveCategory(category: Partial<ExpenseCategory>, editId?: number): Observable<any> {
    if (editId != null) {
      return this.http.put<ExpenseCategory>(`${API_ENDPOINTS.categories}/${editId}`, category).pipe(
        switchMap(() => { this.loadCategories(); return [null]; })
      );
    } else {
      return this.http.post<ExpenseCategory>(API_ENDPOINTS.categories, category).pipe(
        switchMap(() => { this.loadCategories(); return [null]; })
      );
    }
  }

  public deleteCategory(id: number): Observable<any> {
    return this.http.delete<void>(`${API_ENDPOINTS.categories}/${id}`).pipe(
      switchMap(() => { this.loadCategories(); return [null]; })
    );
  }

  // ---- Départements (admin) ----

  public saveDepartment(department: Partial<Department>, editId?: number): Observable<any> {
    if (editId != null) {
      return this.http.put<Department>(`${API_ENDPOINTS.departments}/${editId}`, department).pipe(
        switchMap(() => {
          this.loadDepartments();
          return this.loadUsers();
        })
      );
    } else {
      return this.http.post<Department>(API_ENDPOINTS.departments, department).pipe(
        switchMap(() => {
          this.loadDepartments();
          return this.loadUsers();
        })
      );
    }
  }

  public deleteDepartment(id: number): Observable<any> {
    return this.http.delete<void>(`${API_ENDPOINTS.departments}/${id}`).pipe(
      switchMap(() => {
        this.loadDepartments();
        return this.loadUsers();
      })
    );
  }

  public getAssignableRoles(): RoleType[] {
    return [
      RoleType.EMPLOYEE,
      RoleType.MANAGER,
      RoleType.TECHNICAL_DIRECTOR,
      RoleType.GENERAL_DIRECTOR,
      RoleType.ACCOUNTANT
    ];
  }

  public isTechnicalDepartment(dept?: Department | null): boolean {
    if (!dept || !dept.name) return false;
    const nameLower = dept.name.toLowerCase();
    return nameLower.includes('direction technique') || nameLower.includes('alvanet') || nameLower.includes('slf') || nameLower.includes('scr');
  }
}
