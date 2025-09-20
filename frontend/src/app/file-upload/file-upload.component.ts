

import { Component, EventEmitter, Output, OnInit } from '@angular/core';

@Component({
  selector: 'app-file-upload',
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.scss']
})
export class FileUploadComponent implements OnInit {
  @Output() fileSelected = new EventEmitter<File>();
  @Output() languageSelected = new EventEmitter<string>();

  supportedLanguages: string[] = [
    'en', 'fr', 'de', 'es', 'zh', 'ja', 'ru', 'ar', 'pt', 'it',
    // Add 40+ more supported languages as per MBART
  ];

  selectedLanguage: string = 'en';
  dragOver: boolean = false;

  // Stripe payment elements placeholders (types removed due to missing module)
  private stripe: any = null;
  private elements: any = null;
  private card: any = null;
  paymentError: string | null = null;
  paymentProcessing: boolean = false;

  async ngOnInit(): Promise<void> {
    // Stripe loading logic commented out due to missing module
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.fileSelected.emit(input.files[0]);
    }
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
    if (event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files.length > 0) {
      this.fileSelected.emit(event.dataTransfer.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
  }

  onLanguageChange(event: Event): void {
    this.languageSelected.emit(this.selectedLanguage);
  }

  async handlePayment(): Promise<void> {
    if (!this.stripe || !this.card) {
      this.paymentError = 'Stripe has not loaded correctly.';
      return;
    }
    this.paymentProcessing = true;
    // Payment handling logic commented out due to missing module
  }
}

// Define TypeScript interfaces for translation job status with detailed phases
export type TranslationJobStatus =
  | 'UPLOADED'
  | 'VALIDATING'
  | 'PROCESSING'
  | 'TRANSLATING'
  | 'QUALITY_CHECK'
  | 'COMPLETED'
  | 'FAILED';
