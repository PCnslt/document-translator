# Serverless Document Translation SaaS – Architecture

This architecture leverages AWS serverless services for a scalable, cost-optimized, and secure document translation platform.

```mermaid
flowchart TD
  subgraph Frontend
    A[Angular 17 App]
  end

  subgraph API
    B[API Gateway]
    C[Spring Boot Backend (Lambda)]
    D[Stripe Payment Integration]
  end

  subgraph Async Processing
    E[SQS Queue]
    F[Python/Flask Lambda: Document Processing]
    G[Python/Flask Lambda: Translation (Hugging Face MBART)]
  end

  subgraph Storage & Tracking
    H[S3 (Encrypted w/ KMS)]
    I[DynamoDB (Job Tracking)]
    J[Redis (Free Tier Limits, Caching)]
  end

  subgraph Monitoring & Security
    K[CloudWatch]
    L[AWS KMS]
  end

  %% User flow
  A -- "File Upload, Language Selection, Payment" --> B
  B --> C
  C -- "Validate, Encrypt, Store" --> H
  C -- "Track Job" --> I
  C -- "Send Job to SQS" --> E
  C -- "PaymentIntent" --> D
  D -- "Payment Confirmation" --> C

  %% Async processing
  E -- "Job Details" --> F
  F -- "Extract Text, Virus Scan" --> H
  F -- "Send for Translation" --> G
  G -- "Call Hugging Face MBART" --> G
  G -- "Store Translated File" --> H
  G -- "Update Job Status" --> I

  %% Monitoring & Security
  C -- "Structured Logs" --> K
  F -- "Structured Logs" --> K
  G -- "Structured Logs" --> K
  H -- "Encrypted w/ KMS" --> L
  E -- "Encrypted in Transit/At Rest" --> L

  %% Caching & Free Tier
  C -- "Check/Update Free Tier" --> J
  G -- "Cache Frequent Translations" --> J

  %% GDPR
  A -- "Right to be Forgotten" --> C
  C -- "Purge Data" --> H
  C -- "Purge Data" --> I
  C -- "Purge Logs" --> K
```

**Key Features:**
- **Frontend:** Angular 17 app for file upload, language selection, and payment.
- **Backend:** Spring Boot (serverless Lambda) for API, job management, and payment.
- **Async Processing:** SQS for job queueing, Python/Flask Lambdas for document processing and translation (Hugging Face MBART).
- **Storage:** S3 (encrypted with AWS KMS) for files, DynamoDB for job tracking, Redis for caching and free tier limits.
- **Security:** End-to-end encryption (KMS), GDPR compliance, audit logging.
- **Payments:** Stripe integration with free tier and automated refunds.
- **Monitoring:** CloudWatch dashboards and structured logging.
