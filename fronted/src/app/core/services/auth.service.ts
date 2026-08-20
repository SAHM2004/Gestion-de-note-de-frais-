import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map, catchError, of } from 'rxjs';
import { User, RoleType } from '../models/models';
import { API_ENDPOINTS } from '../constants/api-endpoints';

export interface AuthResponse {
  token: string;
  id: number;
  name: string;
  email: string;
  role: string;
  requirePasswordChange?: boolean;
  departmentId?: number;
  departmentName?: string;
}

export interface UserProfileResponse {
  id: number;
  name: string;
  email: string;
  role: string;
  departmentId?: number;
  departmentName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private currentUserSignal = signal<User | null>(null);
  public currentUser = this.currentUserSignal.asReadonly();
  public isLoggedIn = computed(() => this.currentUserSignal() !== null);
  public userRole = computed(() => this.currentUserSignal()?.role ?? null);
  public requirePasswordChange = computed(() => {
    const user = this.currentUserSignal();
    if (!user) return false;
    if (user.role === RoleType.ADMIN) return false;
    return user.requirePasswordChange ?? false;
  });

  constructor(private http: HttpClient) {
    this.loadSession();
  }

  private loadSession() {
    const savedUser = sessionStorage.getItem('ids_current_user');
    const token = sessionStorage.getItem('ids_jwt_token');
    if (savedUser && token) {
      try {
        this.currentUserSignal.set(JSON.parse(savedUser));
      } catch {
        sessionStorage.removeItem('ids_current_user');
        sessionStorage.removeItem('ids_jwt_token');
      }
    }
  }

  public login(email: string, password: string): Observable<boolean> {
    return this.http.post<AuthResponse>(API_ENDPOINTS.auth.login, { email, password }).pipe(
      tap(response => {
        sessionStorage.setItem('ids_jwt_token', response.token);
        const user: User = {
          id: response.id,
          name: response.name,
          email: response.email,
          role: response.role as RoleType,
          requirePasswordChange: response.requirePasswordChange,
          department: response.departmentId ? { id: response.departmentId, name: response.departmentName ?? '' } : undefined
        };
        sessionStorage.setItem('ids_current_user', JSON.stringify(user));
        this.currentUserSignal.set(user);
      }),
      map(() => true)
    );
  }

  public logout() {
    this.currentUserSignal.set(null);
    sessionStorage.removeItem('ids_jwt_token');
    sessionStorage.removeItem('ids_current_user');
  }

  public getToken(): string | null {
    return sessionStorage.getItem('ids_jwt_token');
  }

  public getProfile(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(API_ENDPOINTS.auth.me).pipe(
      tap(profile => {
        const user: User = {
          id: profile.id,
          name: profile.name,
          email: profile.email,
          role: profile.role as RoleType,
          department: profile.departmentId ? { id: profile.departmentId, name: profile.departmentName ?? '' } : undefined
        };
        sessionStorage.setItem('ids_current_user', JSON.stringify(user));
        this.currentUserSignal.set(user);
      })
    );
  }

  public updateProfileName(name: string): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(API_ENDPOINTS.auth.profile, { name }).pipe(
      tap(profile => {
        const user: User = {
          id: profile.id,
          name: profile.name,
          email: profile.email,
          role: profile.role as RoleType,
          department: profile.departmentId ? { id: profile.departmentId, name: profile.departmentName ?? '' } : undefined
        };
        sessionStorage.setItem('ids_current_user', JSON.stringify(user));
        this.currentUserSignal.set(user);
      })
    );
  }

  public changePassword(oldPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(API_ENDPOINTS.auth.changePassword, { oldPassword, newPassword });
  }

  public hasRole(roles: RoleType[]): boolean {
    const current = this.userRole();
    return current ? roles.includes(current) : false;
  }

  public isEmployee(): boolean {
    return this.userRole() === RoleType.EMPLOYEE;
  }

  public isAdmin(): boolean {
    return this.userRole() === RoleType.ADMIN;
  }

  public isAccountant(): boolean {
    return this.userRole() === RoleType.ACCOUNTANT;
  }

  public canViewGlobalAnalytics(): boolean {
    return this.hasRole([
      RoleType.TECHNICAL_DIRECTOR,
      RoleType.GENERAL_DIRECTOR,
      RoleType.ACCOUNTANT
    ]);
  }

  public canViewDepartmentAnalytics(): boolean {
    return this.hasRole([RoleType.MANAGER]);
  }

  public getRoleLabel(role: RoleType): string {
    switch (role) {
      case RoleType.EMPLOYEE: return 'Employé';
      case RoleType.MANAGER: return 'Manager';
      case RoleType.TECHNICAL_DIRECTOR: return 'Directeur Technique';
      case RoleType.GENERAL_DIRECTOR: return 'Directeur Général';
      case RoleType.ACCOUNTANT: return 'Comptable';
      case RoleType.ADMIN: return 'Administrateur';
      default: return role;
    }
  }
}
