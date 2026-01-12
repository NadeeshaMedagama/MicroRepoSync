# Dependency Updates & Security Pipeline

## 📋 Overview

This document describes the automated dependency updates and security checking pipeline for the RepoSync Microservices project.

## 🎯 Purpose

The dependency updates pipeline ensures:
- **Security**: Regular scanning for known vulnerabilities (CVEs)
- **Maintenance**: Keeping dependencies up-to-date with latest stable versions
- **Compliance**: Tracking and validating license usage
- **Stability**: Automated testing before merging updates

## 🔄 Pipeline Components

### 1. **Dependency Updates Workflow** (`dependency-updates.yml`)

**Location:** `.github/workflows/dependency-updates.yml`

**Triggers:**
- **Scheduled**: Every Monday at 9:00 AM UTC
- **Manual**: Via GitHub Actions UI (workflow_dispatch)
- **Pull Request**: When pom.xml files are modified

### 2. **Renovate Bot** (`renovate.json`)

**Location:** `.github/renovate.json`

**Purpose:** Advanced automated dependency management with intelligent grouping and scheduling.

## 🔐 Security Scanning

### OWASP Dependency Check

Scans all Maven dependencies for known security vulnerabilities.

**Features:**
- CVE detection from National Vulnerability Database
- CVSS score evaluation (fails on score ≥ 7)
- HTML and JSON reports
- Suppression file support for false positives

**Run manually:**
```bash
mvn org.owasp:dependency-check-maven:check
```

**View reports:**
- Location: `target/dependency-check-report.html`
- Artifacts in GitHub Actions workflow runs

### Trivy Security Scanner

Comprehensive security scanner for vulnerabilities and misconfigurations.

**Scans:**
- Dependencies (HIGH, MEDIUM, CRITICAL severity)
- Container images
- Infrastructure as Code (K8s manifests)

**Features:**
- SARIF output for GitHub Security tab
- Integration with GitHub Advanced Security
- Detailed vulnerability reports

### License Compliance

Tracks and validates third-party licenses.

**Features:**
- Automatic license detection
- Third-party license aggregation
- Compliance reporting

**Run manually:**
```bash
mvn license:add-third-party
mvn license:download-licenses
```

## 📊 Workflow Jobs

### 1. `dependency-check`

**Purpose:** Scan dependencies for security vulnerabilities

**Steps:**
1. Checkout code
2. Setup Java 21
3. Run OWASP Dependency Check
4. Generate vulnerability reports
5. Check for outdated dependencies
6. Upload reports as artifacts

**Outputs:**
- OWASP Dependency Check Report (HTML)
- Dependency updates list (TXT)

---

### 2. `maven-dependency-tree`

**Purpose:** Generate complete dependency tree for analysis

**Steps:**
1. Checkout code
2. Setup Java 21
3. Generate dependency tree
4. Upload tree as artifact

**Outputs:**
- Complete dependency tree visualization

---

### 3. `security-audit`

**Purpose:** Comprehensive security vulnerability scanning

**Steps:**
1. Checkout code
2. Setup Java 21
3. Build project
4. Run Trivy scanner
5. Upload results to GitHub Security
6. Upload reports as artifacts

**Outputs:**
- Trivy Security Report (SARIF)
- GitHub Security Alerts

---

### 4. `license-check`

**Purpose:** License compliance verification

**Steps:**
1. Checkout code
2. Setup Java 21
3. Check all dependency licenses
4. Generate license reports
5. Upload reports as artifacts

**Outputs:**
- All licenses report (TXT)

---

### 5. `create-update-pr`

**Purpose:** Automatically create PRs with dependency updates

**Runs:** Only on scheduled/manual triggers

**Steps:**
1. Checkout code
2. Setup Java 21
3. Update to latest stable versions
4. Build and test with updates
5. Create Pull Request if successful
6. Create issue if updates fail

**Outputs:**
- Pull Request with dependency updates
- Or GitHub Issue on failure

**PR Details:**
- Title: "🔄 Weekly Dependency Updates"
- Labels: `dependencies`, `automated`, `security`
- Branch: `dependency-updates/weekly-{run_number}`
- Auto-delete branch after merge

---

### 6. `summary`

**Purpose:** Generate consolidated security summary

**Runs:** After all other jobs complete

**Outputs:**
- GitHub Actions Summary with all results
- Links to downloadable reports

## 🔧 Configuration Files

### `dependency-check-suppressions.xml`

Suppression file for OWASP Dependency Check to handle false positives.

**Location:** Project root

**Usage:**
```xml
<suppress>
    <notes>Reason for suppression</notes>
    <cve>CVE-2024-XXXX</cve>
</suppress>
```

### `renovate.json`

Renovate Bot configuration for intelligent dependency updates.

**Key Features:**
- Grouped updates by category (Spring Boot, Azure SDK, Maven plugins)
- Scheduled updates (Mondays before 10 AM)
- Security updates processed immediately
- Minimum release age for stability (3 days for minor/patch)
- Dependency Dashboard in GitHub Issues

**Package Rules:**
1. **Spring Boot Group**: All Spring dependencies updated together
2. **Maven Plugins**: All Maven plugins grouped
3. **Azure SDK**: Azure dependencies grouped
4. **Milvus SDK**: Milvus dependencies grouped
5. **Security Updates**: High priority, immediate processing
6. **Major Updates**: Separate PRs with "requires-review" label

## 🚀 Usage

### Automatic Execution

The pipeline runs automatically:
- **Every Monday at 9:00 AM UTC** (scheduled)
- **On any PR modifying pom.xml files** (validation)

### Manual Execution

1. Go to **Actions** tab in GitHub
2. Select **Dependency Updates & Security Checks**
3. Click **Run workflow**
4. Select branch
5. Click **Run workflow** button

### Reviewing Updates

When a dependency update PR is created:

1. **Review the PR description**
   - Lists all updated dependencies
   - Shows test results
   - Links to security reports

2. **Download and review artifacts**
   - OWASP Dependency Check Report
   - Trivy Security Scan
   - License Compliance Report
   - Dependency Tree

3. **Check for breaking changes**
   - Review changelog of major version updates
   - Check migration guides
   - Verify API compatibility

4. **Test locally** (optional)
   ```bash
   git fetch origin
   git checkout dependency-updates/weekly-XXX
   mvn clean install
   mvn test
   ```

5. **Approve and merge** when satisfied

## 📈 Reports & Artifacts

All reports are available as workflow artifacts:

| Report | Description | Format | Retention |
|--------|-------------|--------|-----------|
| OWASP Dependency Check | Security vulnerabilities | HTML | 30 days |
| Dependency Updates | Outdated dependencies | TXT | 30 days |
| Dependency Tree | Full dependency graph | TXT | 30 days |
| Trivy Security | Vulnerability scan | SARIF | 30 days |
| License Report | Third-party licenses | TXT | 30 days |

### Accessing Reports

1. Go to workflow run
2. Scroll to **Artifacts** section
3. Download desired report
4. Open in browser/text editor

## 🔔 Notifications

### Success
- Pull Request created with updates
- GitHub Actions Summary shows all green checks

### Failure
- GitHub Issue created automatically
- Title: "⚠️ Dependency Update Failed"
- Labels: `dependencies`, `bug`, `needs-investigation`
- Contains link to failed workflow run

## 🛡️ Security Best Practices

### 1. Regular Reviews
- Review weekly dependency update PRs promptly
- Don't let security updates accumulate

### 2. Suppression Management
- Document all suppressions in `dependency-check-suppressions.xml`
- Regularly review and remove outdated suppressions
- Only suppress verified false positives

### 3. Testing
- Always run full test suite after updates
- Test in staging environment before production
- Monitor services after deployment

### 4. Version Strategy
- Use semantic versioning
- Prefer stable releases over snapshots
- Test major updates thoroughly

## 🔗 Integration with Existing Pipelines

### CI/CD Pipeline Integration

The dependency updates workflow **does not interfere** with existing pipelines:

- **ci-cd.yml**: Continues to run on push/PR as before
- **daily-sync.yml**: Runs independently on schedule
- **dependency-updates.yml**: Runs on separate schedule and creates PRs

### Workflow Interaction

```
┌─────────────────────────────────────┐
│  Dependency Updates (Monday 9 AM)  │
│  - Scans dependencies               │
│  - Creates update PR                │
└────────────────┬────────────────────┘
                 │
                 ▼
        ┌────────────────┐
        │  PR Created    │
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────┐
        │   CI/CD Runs   │ ◄─── Existing pipeline
        │  - Build       │
        │  - Test        │
        │  - Scan        │
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────┐
        │  Manual Review │
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────┐
        │  Merge to Main │
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────┐
        │  CI/CD Deploys │ ◄─── Existing pipeline
        └────────────────┘
```

## 🐛 Troubleshooting

### Build Fails After Updates

**Symptom:** Tests fail or build breaks after dependency update

**Solution:**
1. Check workflow logs for specific error
2. Review dependency changelog for breaking changes
3. Update code to handle API changes
4. Add migration steps to PR

### OWASP Check False Positives

**Symptom:** Build fails due to CVEs that don't apply

**Solution:**
1. Verify CVE is actually a false positive
2. Add suppression to `dependency-check-suppressions.xml`
3. Document reason for suppression
4. Re-run workflow

### License Violations

**Symptom:** Incompatible license detected

**Solution:**
1. Review license report
2. Check if dependency is necessary
3. Find alternative with compatible license
4. Document license exception if acceptable

## 📚 Additional Resources

- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
- [Renovate Documentation](https://docs.renovatebot.com/)
- [Maven Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/)

## 🤝 Contributing

When adding new dependencies:

1. Check for security vulnerabilities first
2. Verify license compatibility
3. Update documentation
4. Test thoroughly
5. Monitor first few weeks after merge

## 📝 Maintenance

### Weekly
- Review and merge dependency update PRs
- Check security reports

### Monthly
- Review suppression file
- Clean up outdated suppressions
- Update Renovate configuration if needed

### Quarterly
- Review overall dependency strategy
- Evaluate new security tools
- Update pipeline configuration

---

**Last Updated:** January 2026
**Maintained By:** RepoSync Team

