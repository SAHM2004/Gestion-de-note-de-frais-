import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { ExpenseService } from '../../../core/services/expense.service';
import { AdminDataService } from '../../../core/services/admin-data.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private authService = inject(AuthService);
  private expenseService = inject(ExpenseService);
  private adminDataService = inject(AdminDataService);
  private router = inject(Router);

  public email: string = '';
  public password: string = '';
  public loginError = signal<string | null>(null);
  public isLoading = signal(false);

  public onSubmit() {
    this.loginError.set(null);
    this.isLoading.set(true);

    this.authService.login(this.email, this.password).subscribe({
      next: (success) => {
        this.isLoading.set(false);
        if (success) {
          this.expenseService.loadExpenses();
          this.adminDataService.loadAll();
          this.router.navigate(['/dashboard']);
        } else {
          this.loginError.set('Identifiants invalides. Vérifiez votre e-mail et mot de passe.');
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.error && err.error.message) {
          this.loginError.set(err.error.message);
        } else {
          this.loginError.set('Erreur de connexion au serveur. Vérifiez que le backend est démarré.');
        }
      }
    });
  }
}
