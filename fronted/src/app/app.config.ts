import { ApplicationConfig, provideZonelessChangeDetection, provideBrowserGlobalErrorListeners, LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';

// Modifier le séparateur de milliers pour utiliser le point au lieu de l'espace insécable
const localeFrCustom = localeFr as any;
if (localeFrCustom && localeFrCustom[13]) {
  localeFrCustom[13][1] = '.';
}
registerLocaleData(localeFrCustom, 'fr-FR');

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: LOCALE_ID, useValue: 'fr-FR' }
  ]
};
