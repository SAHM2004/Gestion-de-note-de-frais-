import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { API_BASE_URL } from '../constants/api-endpoints';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = sessionStorage.getItem('ids_jwt_token');

  // Injecter le token JWT uniquement pour les requêtes vers l'API backend
  let authReq = req;
  if (token && req.url.startsWith(API_BASE_URL)) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Token expiré ou invalide → déconnexion et redirection
        sessionStorage.removeItem('ids_jwt_token');
        sessionStorage.removeItem('ids_current_user');
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
