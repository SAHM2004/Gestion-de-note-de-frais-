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

  if (authService.requirePasswordChange()) {
    // Si l'utilisateur essaie d'accéder à autre chose que la page de force-password-change, on le redirige
    if (state.url !== '/force-password-change') {
      router.navigate(['/force-password-change']);
      return false;
    }
    return true; // Autoriser l'accès à la page de changement
  } else if (state.url === '/force-password-change') {
    // S'il n'en a pas besoin, il ne devrait pas être sur cette page
    router.navigate(['/dashboard']);
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
