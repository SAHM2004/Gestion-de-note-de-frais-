import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleType } from '../models/models';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.getToken();
  if (!token || !authService.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  const expectedRoles = route.data?.['roles'] as RoleType[];
  if (expectedRoles && expectedRoles.length > 0) {
    const userRole = authService.userRole();
    if (!userRole || !expectedRoles.includes(userRole)) {
      router.navigate(['/dashboard']);
      return false;
    }
  }

  return true;
};
