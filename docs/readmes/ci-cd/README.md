# 🔄 CI/CD & Automation Documentation

This directory contains documentation for Continuous Integration, Continuous Deployment, and automated workflows.

## 📚 Documents in This Section

### [PIPELINE_ARCHITECTURE.md](PIPELINE_ARCHITECTURE.md) 🏗️
**CI/CD pipeline architecture**
- Complete pipeline overview
- GitHub Actions workflows
- Pipeline stages and steps
- Integration points
- Deployment strategies

**Use this for**: Understanding the CI/CD architecture

---

### [COMPLETE_WORKFLOWS_IMPLEMENTATION.md](COMPLETE_WORKFLOWS_IMPLEMENTATION.md) ⚙️
**GitHub Actions workflows implementation**
- Detailed workflow configurations
- Build and test automation
- Deployment workflows
- Security scanning
- Code quality checks

**Use this for**: Implementing or modifying workflows

---

### [DEPENDENCY_UPDATES_PIPELINE.md](DEPENDENCY_UPDATES_PIPELINE.md) 🔄
**Automated dependency updates**
- Dependabot configuration
- Automated PR creation
- Security vulnerability scanning
- Update approval process

**Use this for**: Managing dependency updates

---

### [DEPENDENCY_UPDATES_QUICKSTART.md](DEPENDENCY_UPDATES_QUICKSTART.md) ⚡
**Quick guide to dependency updates**
- Fast setup instructions
- Common update scenarios
- Quick commands
- Best practices

**Use this for**: Quick dependency update tasks

---

### [DEPENDENCY_UPDATES_SUMMARY.md](DEPENDENCY_UPDATES_SUMMARY.md) 📝
**Dependency management summary**
- Current dependency status
- Update history
- Known issues
- Recommendations

**Use this for**: Overview of dependency status

---

## 🚀 GitHub Actions Workflows

The project includes these automated workflows:

### 1. **Build and Test** (`build.yml`)
- Triggers on: Push, Pull Request
- Actions:
  - Maven build
  - Run unit tests
  - Code coverage (JaCoCo)
  - Artifact upload

### 2. **Code Quality** (`code-quality.yml`)
- Triggers on: Push, Pull Request
- Actions:
  - Checkstyle
  - PMD
  - SpotBugs
  - Qodana analysis

### 3. **Security Scan** (`security.yml`)
- Triggers on: Schedule, Manual
- Actions:
  - OWASP Dependency Check
  - CVE scanning
  - Security report generation

### 4. **Dependency Updates** (`dependabot.yml`)
- Triggers on: Schedule
- Actions:
  - Check for updates
  - Create PRs for updates
  - Security patches

### 5. **Docker Build** (`docker.yml`)
- Triggers on: Tag push
- Actions:
  - Build Docker images
  - Push to registry
  - Multi-arch builds

---

## ⚙️ Quick Start

### Enable GitHub Actions
```yaml
# Workflows are located in .github/workflows/
# They run automatically on configured triggers
```

### Manual Workflow Trigger
```bash
# Via GitHub UI:
# Actions → Select workflow → Run workflow

# Via GitHub CLI:
gh workflow run build.yml
```

### View Workflow Results
```bash
# Via GitHub UI:
# Actions tab → Select workflow run

# Via GitHub CLI:
gh run list
gh run view <run-id>
```

---

## 🔄 Dependency Management

### Automated Updates
```yaml
# Dependabot checks for updates daily
# Creates PRs automatically for:
# - Maven dependencies
# - GitHub Actions versions
# - Docker base images
```

### Manual Dependency Check
```bash
# Check for outdated dependencies
mvn versions:display-dependency-updates

# Check for plugin updates
mvn versions:display-plugin-updates

# Update dependencies interactively
mvn versions:update-properties
```

### Security Scanning
```bash
# Run OWASP Dependency Check
mvn org.owasp:dependency-check-maven:check

# View security report
open target/dependency-check-report.html
```

---

## 📊 Pipeline Stages

```
┌─────────────────┐
│   Code Push     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Build Stage   │
│  • Compile      │
│  • Unit Tests   │
│  • Package      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Quality Stage   │
│  • Checkstyle   │
│  • PMD          │
│  • SpotBugs     │
│  • Coverage     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Security Stage  │
│  • OWASP Check  │
│  • CVE Scan     │
│  • License Check│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Docker Build   │
│  • Build Images │
│  • Tag & Push   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│    Deploy       │
│  • K8s Deploy   │
│  • Health Check │
└─────────────────┘
```

---

## 🛠️ Configuration Files

### GitHub Actions Workflows
```
.github/
└── workflows/
    ├── build.yml              # Build and test
    ├── code-quality.yml       # Code quality checks
    ├── security.yml           # Security scanning
    ├── dependabot.yml         # Dependency updates
    └── docker.yml             # Docker image builds
```

### Dependabot Configuration
```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "daily"
```

### OWASP Configuration
```xml
<!-- dependency-check-suppressions.xml -->
<!-- Suppression rules for known false positives -->
```

---

## 🔐 Security Automation

### Automated Scans
- ✅ **Daily dependency vulnerability scans**
- ✅ **PR-triggered security checks**
- ✅ **OWASP Top 10 validation**
- ✅ **License compliance checking**

### Security Thresholds
```xml
<!-- Fail build on CVSS score >= 7 -->
<configuration>
  <failBuildOnCVSS>7</failBuildOnCVSS>
</configuration>
```

### Security Reports
- Location: `target/dependency-check-report.html`
- Format: HTML, JSON
- Auto-uploaded as GitHub artifacts

---

## 📈 Metrics & Reporting

### Code Coverage
- Tool: JaCoCo
- Minimum coverage: 50% line coverage
- Reports: `target/site/jacoco/index.html`

### Code Quality
- Tools: Checkstyle, PMD, SpotBugs
- Standards: Google Java Style Guide
- Reports: Available in build logs

### Build Metrics
- Build time tracking
- Test execution time
- Artifact sizes
- Available in GitHub Actions UI

---

## 🔗 Integration Points

### External Services
- **GitHub Actions**: CI/CD platform
- **Dependabot**: Dependency updates
- **JetBrains Qodana**: Code quality
- **Docker Hub**: Image registry (optional)
- **Kubernetes**: Deployment target

### Notifications
- GitHub PR comments
- Workflow status checks
- Email notifications (configurable)

---

## 🚦 Workflow Best Practices

1. **Keep workflows fast**: Use caching, parallel jobs
2. **Fail fast**: Run quick checks first
3. **Security first**: Always scan dependencies
4. **Automate everything**: Reduce manual steps
5. **Monitor and alert**: Set up notifications
6. **Document changes**: Update workflow docs

---

## 🔧 Troubleshooting

### Workflow Fails
1. Check workflow logs in GitHub Actions
2. Review [COMPLETE_WORKFLOWS_IMPLEMENTATION.md](COMPLETE_WORKFLOWS_IMPLEMENTATION.md)
3. Verify secrets and environment variables
4. Check branch protection rules

### Dependency Update Issues
1. Review Dependabot logs
2. Check [DEPENDENCY_UPDATES_PIPELINE.md](DEPENDENCY_UPDATES_PIPELINE.md)
3. Verify version compatibility
4. Test updates locally first

### Security Scan Failures
1. Review OWASP report
2. Check suppression rules
3. Update vulnerable dependencies
4. Consult [DEPENDENCY_UPDATES_SUMMARY.md](DEPENDENCY_UPDATES_SUMMARY.md)

---

## 📝 Common Tasks

### Update GitHub Actions Versions
```bash
# Check for action updates
gh api repos/:owner/:repo/actions/workflows

# Update in workflow files
# .github/workflows/*.yml
```

### Configure Branch Protection
```bash
# Via GitHub UI:
# Settings → Branches → Add rule

# Recommended rules:
# - Require PR reviews
# - Require status checks
# - Require up-to-date branches
```

### Set Up Secrets
```bash
# Via GitHub UI:
# Settings → Secrets → New repository secret

# Required secrets:
# - AZURE_OPENAI_API_KEY
# - MILVUS_TOKEN
# - DOCKER_USERNAME (optional)
# - DOCKER_PASSWORD (optional)
```

---

## 🔗 Related Documentation

- [Build & Deployment](../build-deployment/) - Build configuration
- [Setup Guides](../setup-guides/) - Local setup
- [Monitoring](../monitoring/) - Observability

---

## 📚 External Resources

- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Dependabot Docs](https://docs.github.com/en/code-security/dependabot)
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [Maven Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/)

---

**Last Updated**: January 8, 2026

