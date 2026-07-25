import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { RoleType } from '../../core/models/models';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class SidebarComponent {
  public authService = inject(AuthService);
  private router = inject(Router);

  public RoleType = RoleType;

  public logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
