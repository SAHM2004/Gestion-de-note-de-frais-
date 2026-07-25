import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ExpenseAttachment } from '../models/models';
import { API_ENDPOINTS, ALLOWED_ATTACHMENT_TYPES, MAX_ATTACHMENT_SIZE_BYTES } from '../constants/api-endpoints';

@Injectable({ providedIn: 'root' })
export class AttachmentService {

  constructor(private http: HttpClient) {}

  public validateFile(file: File): string | null {
    if (!ALLOWED_ATTACHMENT_TYPES.includes(file.type)) {
      return 'Format non autorisé. Utilisez PDF, JPEG ou PNG.';
    }
    if (file.size > MAX_ATTACHMENT_SIZE_BYTES) {
      return 'Fichier trop volumineux (maximum 10 Mo).';
    }
    return null;
  }

  public getByReportId(reportId: number): Observable<ExpenseAttachment[]> {
    return this.http.get<ExpenseAttachment[]>(API_ENDPOINTS.expenses.attachments(reportId));
  }

  public async addAttachment(
    reportId: number,
    file: File,
    lineId?: number
  ): Promise<ExpenseAttachment> {
    const error = this.validateFile(file);
    if (error) throw new Error(error);

    const formData = new FormData();
    formData.append('file', file);
    if (lineId != null) formData.append('lineId', String(lineId));

    return new Promise((resolve, reject) => {
      this.http.post<ExpenseAttachment>(
        API_ENDPOINTS.expenses.uploadAttachment(reportId),
        formData
      ).subscribe({
        next: att => resolve(att),
        error: err => reject(err)
      });
    });
  }

  public deleteAttachment(attachmentId: number): Observable<void> {
    return this.http.delete<void>(API_ENDPOINTS.expenses.deleteAttachment(attachmentId));
  }

  public downloadUrl(attachmentId: number): string {
    return API_ENDPOINTS.expenses.downloadAttachment(attachmentId);
  }

  public downloadFile(attachmentId: number, filename: string): void {
    const url = this.downloadUrl(attachmentId);
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
      },
      error: (err) => console.error('Erreur téléchargement', err)
    });
  }

  public formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' o';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
  }

  public isImage(contentType: string): boolean {
    return contentType.startsWith('image/');
  }
}
