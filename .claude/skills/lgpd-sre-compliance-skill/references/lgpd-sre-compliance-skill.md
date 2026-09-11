# LGPD-SRE Compliance and Sanitization Skill (SKILL.md)

This skill provides the `lgpd-sre-compliance-auditor` agent with the operational procedures, regulatory alignments (Lei 13.709/2018), and programmatic templates necessary to identify, mask, and audit personal data (PII) exposure in logs, tracing pipelines, and SRE environments.

## 1. Regulatory Context and Alignment

We align system operations with key principles of the **Brazilian General Data Protection Law (LGPD)**:
*   **Art. 6º, III (Minimização):** Telemetry must only ingest, process, and retain the absolute minimum metadata required for system reliability. No arbitrary user payloads should reside in telemetry indices.
*   **Art. 6º, VII (Segurança):** System logs, tracing spans, and operational backups must be structured securely, ensuring confidentiality and integrity against unauthorized leaks.
*   **Art. 46 (Medidas de Segurança e Prevenção):** Operational environments must prevent access to raw user PII during crisis response (Incident Response / postmortems).

---

## 2. Step-by-Step Compliance & Security Audit Workflow

When auditing a repository or log stream, follow this workflow:

### Step 1: Detect PII and Security Exposure
*   Actively look for variables or keys named: `cpf`, `email`, `phone`, `telefone`, `password`, `card`, `token`, `address`, `endereco`, `ip_address`, `username`.
*   Locate any raw outputs or logger calls registering these parameters directly (e.g., `logger.info(f"User: {email}")`).

### Step 2: Evaluate Sanitization Mechanisms
*   Inspect the codebase to determine if there is an active Data Scrubbing layer (middleware, interceptor, or a pipeline processor).
*   If absent, flags this as an immediate High Severity Compliance Gap.

### Step 3: Implement Masking and Redaction
*   Inject dynamic redactors (masking algorithms) to replace sensitive strings before writing.
*   Format masks to preserve debug value while obfuscating identity (e.g., `user@domain.com` -> `u***@domain.com`, `123.456.789-10` -> `***.456.789-**`).

### Step 4: Audit SRE Access Paths
*   Evaluate how engineers interact with production data.
*   Recommend the elimination of manual SSH log reading in favor of a central, immutable log collection engine with Role-Based Access Control (RBAC).

---

## 3. Reference Implementation: Masking Processor Template

### Python Logging Masking Filter (Few-Shot)

```python
import re
import logging

class LGPDPIIMaskFilter(logging.Filter):
    # Regex definitions for Brazilian CPF and typical E-mails
    CPF_REGEX = re.compile(r'\b\d{3}\.\d{3}\.\d{3}-\d{2}\b|\b\d{11}\b')
    EMAIL_REGEX = re.compile(r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b')

    def filter(self, record):
        if isinstance(record.msg, str):
            record.msg = self.mask_data(record.msg)
        return True

    def mask_data(self, text):
        # Mask CPF preserving only middle digits for debugging alignment
        text = self.CPF_REGEX.sub(lambda m: self._mask_cpf(m.group(0)), text)
        # Mask Email preserving domain
        text = self.EMAIL_REGEX.sub(lambda m: self._mask_email(m.group(0)), text)
        return text

    def _mask_cpf(self, cpf):
        clean = re.sub(r'\D', '', cpf)
        if len(clean) == 11:
            return f"***.{clean[3:6]}.{clean[6:9]}-**"
        return "[MASKED_CPF]"

    def _mask_email(self, email):
        parts = email.split('@')
        if len(parts) == 2:
            local, domain = parts
            masked_local = local[0] + "*" * (len(local) - 1) if len(local) > 1 else "*"
            return f"{masked_local}@{domain}"
        return "[MASKED_EMAIL]"

# Application Example
logger = logging.getLogger("secure-app")
handler = logging.StreamHandler()
handler.addFilter(LGPDPIIMaskFilter())
logger.addHandler(handler)
logger.setLevel(logging.INFO)

# Will output: "Processando dados do usuario: a*****@empresa.com com CPF: ***.456.789-**"
logger.info("Processando dados do usuario: alberto@empresa.com com CPF: 123.456.789-10")
```
