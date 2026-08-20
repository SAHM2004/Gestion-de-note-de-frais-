import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { API_ENDPOINTS } from '../../../core/constants/api-endpoints';

@Component({
  selector: 'app-force-password-change',
  imports: [FormsModule, CommonModule],
  templateUrl: './force-password-change.html'
})
export class ForcePasswordChangeComponent {
  private authService = inject(AuthService);
  private http = inject(HttpClient);
  private router = inject(Router);

  public oldPassword = '';
  public newPassword = '';
  public confirmPassword = '';
  
  public errorMessage = '';
  public isLoading = false;

  public onSubmit() {
    this.errorMessage = '';

    if (!this.oldPassword || !this.newPassword || !this.confirmPassword) {
      this.errorMessage = 'Veuillez remplir tous les champs.';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Les nouveaux mots de passe ne correspondent pas.';
      return;
    }

    if (this.newPassword.length < 6) {
      this.errorMessage = 'Le nouveau mot de passe doit contenir au moins 6 caractères.';
      return;
    }

    this.isLoading = true;
    
    this.http.put(API_ENDPOINTS.auth.changePassword, {
      oldPassword: this.oldPassword,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.isLoading = false;
        // Mettre à jour l'état local pour indiquer que le mdp est changé
        const user = JSON.parse(sessionStorage.getItem('ids_current_user') || '{}');
        user.requirePasswordChange = false;
        sessionStorage.setItem('ids_current_user', JSON.stringify(user));
        
        // Recharger la session en forçant la mise à jour du signal via une méthode qu'on va ajouter ou en rechargeant la page
        // Le plus sûr pour que le guard se rafraîchisse proprement est de recharger l'URL complète
        window.location.href = '/dashboard';
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'L\'ancien mot de passe est incorrect ou une erreur est survenue.';
      }
    });
  }
}
