import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout';
import { DashboardComponent } from './features/dashboard/dashboard';
import { Login } from './features/auth/login/login';
import { ExpenseCreate } from './features/expenses/expense-create/expense-create';
import { ExpenseListComponent } from './features/expenses/expense-list/expense-list';
import { ApprovalList } from './features/approvals/approval-list/approval-list';
import { FinancialDashboard } from './features/analytics/financial-dashboard/financial-dashboard';
import { Settings } from './features/admin/settings/settings';
import { ProfileComponent } from './features/profile/profile';
import { authGuard } from './core/guards/auth.guard';
import { RoleType } from './core/models/models';

export const routes: Routes = [
    {
        path: 'login',
        component: Login
    },
    {
        path: '',
        component: MainLayoutComponent,
        canActivate: [authGuard],
        children: [
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
            { path: 'dashboard', component: DashboardComponent },
            { path: 'profile', component: ProfileComponent },
            { 
                path: 'expenses/list', 
                component: ExpenseListComponent,
                data: { roles: [RoleType.EMPLOYEE, RoleType.MANAGER, RoleType.TECHNICAL_DIRECTOR, RoleType.GENERAL_DIRECTOR, RoleType.ACCOUNTANT] }
            },
            { 
                path: 'expenses/new', 
                component: ExpenseCreate,
                data: { roles: [RoleType.EMPLOYEE, RoleType.MANAGER, RoleType.TECHNICAL_DIRECTOR, RoleType.GENERAL_DIRECTOR, RoleType.ACCOUNTANT] }
            },
            { 
                path: 'expenses/edit/:id', 
                component: ExpenseCreate,
                data: { roles: [RoleType.EMPLOYEE, RoleType.MANAGER, RoleType.TECHNICAL_DIRECTOR, RoleType.GENERAL_DIRECTOR, RoleType.ACCOUNTANT] }
            },
            { 
                path: 'approvals', 
                component: ApprovalList,
                data: { roles: [RoleType.MANAGER, RoleType.TECHNICAL_DIRECTOR, RoleType.GENERAL_DIRECTOR, RoleType.ACCOUNTANT] }
            },
            { 
                path: 'analytics', 
                component: FinancialDashboard,
                data: { roles: [RoleType.MANAGER, RoleType.TECHNICAL_DIRECTOR, RoleType.GENERAL_DIRECTOR, RoleType.ACCOUNTANT] }
            },
            { 
                path: 'admin/users', 
                component: Settings,
                data: { roles: [RoleType.ADMIN], tab: 'users' }
            },
            { 
                path: 'admin/departments', 
                component: Settings,
                data: { roles: [RoleType.ADMIN], tab: 'departments' }
            },
            { 
                path: 'admin/categories', 
                component: Settings,
                data: { roles: [RoleType.ADMIN], tab: 'categories' }
            },
            { path: 'admin/settings', redirectTo: 'admin/users', pathMatch: 'full' }
        ]
    }
];
