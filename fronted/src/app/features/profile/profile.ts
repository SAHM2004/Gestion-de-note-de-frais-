import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class ProfileComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  public name = signal('');
  public email = signal('');
  public oldPassword = '';
  public newPassword = '';
  public confirmPassword = '';
  public successMessage = signal<string | null>(null);
  public errorMessage = signal<string | null>(null);

  constructor() {
    const user = this.authService.currentUser();
    if (user) {
      this.name.set(user.name);
      this.email.set(user.email);
    }
  }

  public getRoleLabel(): string {
    const role = this.authService.userRole();
    if (!role) return '';
    return this.authService.getRoleLabel(role);
  }

  public onSubmit() {
    this.successMessage.set(null);
    this.errorMessage.set(null);

    if (!this.name().trim()) {
      this.errorMessage.set('Le nom est obligatoire.');
      return;
    }

    const doUpdateProfile = () => {
      this.authService.updateProfileName(this.name().trim()).subscribe({
        next: () => this.successMessage.set('Profil mis à jour avec succès.'),
        error: (err) => this.errorMessage.set('Erreur lors de la mise à jour : ' + (err.error?.message ?? err.message))
      });
    };

    if (this.newPassword || this.oldPassword || this.confirmPassword) {
      if (!this.oldPassword) {
        this.errorMessage.set('Veuillez saisir votre mot de passe actuel.');
        return;
      }
      if (this.newPassword.length < 6) {
        this.errorMessage.set('Le nouveau mot de passe doit contenir au moins 6 caractères.');
        return;
      }
      if (this.newPassword !== this.confirmPassword) {
        this.errorMessage.set('Les mots de passe ne correspondent pas.');
        return;
      }
      this.authService.changePassword(this.oldPassword, this.newPassword).subscribe({
        next: () => {
          this.oldPassword = '';
          this.newPassword = '';
          this.confirmPassword = '';
          doUpdateProfile();
          alert('Mot de passe modifié avec succès !');
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.errorMessage.set(err.error?.message ?? 'Mot de passe actuel incorrect.');
        }
      });
    } else {
      doUpdateProfile();
    }
  }

  public goBack() {
    this.router.navigate(['/dashboard']);
  }
}
