# GitHub Actions Pipeline Documentation

This document provides a comprehensive overview of the GitHub Actions CI/CD pipelines configured for the **RepoSync Microservices** project.

## Overview

The project has **TWO** main GitHub Actions workflows:

1. **CI/CD Pipeline** (`ci-cd.yml`) - Continuous Integration and Deployment
2. **Daily Sync Pipeline** (`daily-sync.yml`) - Automated daily repository synchronization at 8:00 AM

---

## 1. CI/CD Pipeline (`ci-cd.yml`)

**Location:** `.github/workflows/ci-cd.yml`

### Purpose
Automates the build, test, Docker image creation, and Kubernetes deployment process for all microservices.

### Trigger Events
- **Push** to `main` or `develop` branches
- **Pull Requests** targeting `main` or `develop` branches

### Pipeline Stages

#### Stage 1: Build and Test
**Job Name:** `build-and-test`

**What it does:**
- ✅ Checks out the source code
- ✅ Sets up Java 17 environment (⚠️ **Needs update to Java 21**)
- ✅ Caches Maven dependencies for faster builds
- ✅ Builds all services using `mvn clean install`
- ✅ Runs all unit and integration tests
- ✅ Uploads test results as artifacts for review

**Runs on:** Every push and pull request

**Key Commands:**
```bash
mvn clean install
mvn test
```

**Outputs:**
- Test results uploaded as artifacts
- Build artifacts cached for subsequent jobs

---

#### Stage 2: Build Docker Images
**Job Name:** `build-docker-images`

**Dependencies:** Requires `build-and-test` to pass

**What it does:**
- ✅ Builds Docker images for **all 5 microservices** in parallel
- ✅ Pushes images to Docker Hub registry
- ✅ Tags images with multiple tags (branch, SHA, version, latest)
- ✅ Uses Docker layer caching for faster builds

**Runs on:** Only on pushes to `main` branch (not on pull requests)

**Services Built:**
1. `github-service`
2. `document-processor-service`
3. `embedding-service`
4. `milvus-service`
5. `orchestrator-service`

**Matrix Strategy:**
Uses GitHub Actions matrix strategy to build all services in parallel, significantly reducing build time.

**Docker Image Tags:**
- `reposync/github-service:main`
- `reposync/github-service:sha-abc1234`
- `reposync/github-service:latest`
- `reposync/github-service:1.0.0` (if using semver)

**Required Secrets:**
- `DOCKER_USERNAME` - Docker Hub username
- `DOCKER_PASSWORD` - Docker Hub password or access token

---

#### Stage 3: Deploy to Kubernetes
**Job Name:** `deploy-to-kubernetes`

**Dependencies:** Requires `build-docker-images` to pass

**What it does:**
- ✅ Configures `kubectl` CLI tool
- ✅ Creates/updates Kubernetes secrets with sensitive data
- ✅ Creates/updates Kubernetes ConfigMaps with configuration
- ✅ Deploys all microservices to Kubernetes cluster
- ✅ Waits for all deployments to be ready (rollout status)
- ✅ Verifies deployment by listing pods and services

**Runs on:** Only on pushes to `main` branch

**Deployment Order:**
1. Namespace and configuration
2. GitHub Service
3. Document Processor Service
4. Embedding Service
5. Milvus Service
6. Orchestrator Service

**Kubernetes Resources Created:**
- **Secrets:** `reposync-secrets` (GitHub token, Azure API key, Milvus token)
- **ConfigMap:** `reposync-config` (Organization, keywords, endpoints, URLs)
- **Deployments:** All 5 microservices
- **Services:** ClusterIP services for internal communication

**Required Secrets:**
- `KUBE_CONFIG` - Base64-encoded kubeconfig file for cluster access
- `REPOSYNC_GITHUB_TOKEN`
- `REPOSYNC_ORGANIZATION`
- `REPOSYNC_FILTER_KEYWORD`
- `AZURE_OPENAI_API_KEY`
- `AZURE_OPENAI_ENDPOINT`
- `AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT`
- `MILVUS_URI`
- `MILVUS_TOKEN`
- `MILVUS_COLLECTION_NAME`

**Timeout:** 5 minutes per deployment

---

## 2. Daily Sync Pipeline (`daily-sync.yml`)

**Location:** `.github/workflows/daily-sync.yml`

### Purpose
Automatically runs the repository synchronization job **every day at 8:00 AM UTC** to fetch new repositories, process documents, generate embeddings, and update the Milvus vector database.

### Trigger Events
1. **Scheduled:** Daily at 8:00 AM UTC (`cron: '0 8 * * *'`)
2. **Manual:** Can be triggered manually via GitHub UI (`workflow_dispatch`)

### Cron Schedule
```yaml
schedule:
  - cron: '0 8 * * *'  # Every day at 8:00 AM UTC
```

**To adjust timezone:** Convert to UTC from your local time
- 8:00 AM IST (UTC+5:30) = 2:30 AM UTC → `cron: '30 2 * * *'`
- 8:00 AM PST (UTC-8) = 4:00 PM UTC → `cron: '0 16 * * *'`

### Pipeline Workflow

#### Step 1: Setup Environment
**What it does:**
- ✅ Checks out repository code
- ✅ Sets up Java 17 environment (⚠️ **Needs update to Java 21**)
- ✅ Caches Maven dependencies
- ✅ Builds all services (without running tests for speed)
- ✅ Sets all environment variables from GitHub Secrets

**Key Command:**
```bash
mvn clean package -DskipTests
```

---

#### Step 2: Start Microservices (Sequential)
Services are started in dependency order with appropriate sleep intervals to ensure proper initialization:

1. **GitHub Service** (Port 8081)
   - Sleep: 30 seconds
   - Handles GitHub API communication
   
2. **Document Processor Service** (Port 8082)
   - Sleep: 20 seconds
   - Processes README and API definition files
   
3. **Embedding Service** (Port 8083)
   - Sleep: 30 seconds
   - Generates embeddings using Azure OpenAI
   
4. **Milvus Service** (Port 8084)
   - Sleep: 30 seconds
   - Manages Milvus vector database operations
   
5. **Orchestrator Service** (Port 8080)
   - Sleep: 60 seconds
   - Coordinates the entire sync workflow

**Process Management:**
- Each service runs in background using `nohup`
- Process IDs (PIDs) saved to files for cleanup
- Logs captured to individual log files

---

#### Step 3: Execute Sync Job
**What it does:**
- ✅ Waits for services to be fully initialized (30 seconds)
- ✅ Triggers sync job via REST API call
- ✅ Saves sync result to JSON file

**API Call:**
```bash
curl -X POST http://localhost:8080/api/orchestrator/sync \
  -H "Content-Type: application/json" \
  -o sync-result.json
```

---

#### Step 4: Verify Sync Status
**What it does:**
- ✅ Parses sync result JSON
- ✅ Checks if status is `SUCCESS`
- ✅ Displays sync statistics:
  - Repositories processed
  - Documents processed
  - Chunks created
  - Vectors stored in Milvus
- ✅ Fails the workflow if sync failed

**Example Output:**
```
Sync completed successfully!
Repositories processed: 15
Documents processed: 42
Chunks created: 387
Vectors stored: 387
```

---

#### Step 5: Upload Logs (Always)
**What it does:**
- ✅ Uploads all service logs as artifacts
- ✅ Uploads sync result JSON
- ✅ Runs even if previous steps failed (`if: always()`)

**Artifact Name:** `service-logs`

**Files Included:**
- `github-service.log`
- `processor-service.log`
- `embedding-service.log`
- `milvus-service.log`
- `orchestrator-service.log`
- `sync-result.json`

---

#### Step 6: Cleanup (Always)
**What it does:**
- ✅ Kills all running service processes
- ✅ Ensures no processes left running
- ✅ Runs even if previous steps failed

**Cleanup Logic:**
```bash
for service in github-service document-processor-service embedding-service milvus-service orchestrator-service; do
  if [ -f $service/$service.pid ]; then
    kill $(cat $service/$service.pid) || true
  fi
done
```

---

## Required GitHub Secrets

All secrets must be configured in GitHub repository settings: **Settings → Secrets and variables → Actions**

### Secrets List

| Secret Name | Description | Example Value |
|------------|-------------|---------------|
| `REPOSYNC_GITHUB_TOKEN` | GitHub Personal Access Token with `repo` scope | `ghp_xxxxxxxxxxxxx` |
| `REPOSYNC_ORGANIZATION` | GitHub organization name to sync | `microsoft` |
| `REPOSYNC_FILTER_KEYWORD` | Keyword to filter repositories (optional) | `java` or empty |
| `AZURE_OPENAI_API_KEY` | Azure OpenAI API key | `abc123...` |
| `AZURE_OPENAI_ENDPOINT` | Azure OpenAI endpoint URL | `https://your-resource.openai.azure.com/` |
| `AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT` | Deployment name for embeddings model | `text-embedding-ada-002` |
| `MILVUS_URI` | Milvus database URI | `https://your-milvus.cloud:19530` |
| `MILVUS_TOKEN` | Milvus authentication token | `token123...` |
| `MILVUS_COLLECTION_NAME` | Milvus collection name | `reposync_collection` |
| `DOCKER_USERNAME` | Docker Hub username (for CI/CD) | `your-docker-username` |
| `DOCKER_PASSWORD` | Docker Hub password/token (for CI/CD) | `dckr_pat_xxxxx` |
| `KUBE_CONFIG` | Base64-encoded kubeconfig (for deployment) | `YXBpVmVyc2lvb...` |

### How to Add Secrets

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Enter secret name and value
5. Click **Add secret**

---

## Pipeline Execution Flow

### CI/CD Pipeline Flow
```
Push/PR to main/develop
         ↓
    Build & Test
         ↓
  [Success?] → No → Fail (stop)
         ↓ Yes
  Build Docker Images (only on main)
         ↓
  [Success?] → No → Fail (stop)
         ↓ Yes
  Deploy to Kubernetes (only on main)
         ↓
    Verify Deployment
         ↓
       Done ✅
```

### Daily Sync Pipeline Flow
```
8:00 AM UTC Daily (or Manual Trigger)
         ↓
    Setup & Build
         ↓
  Start All Services (Sequential)
         ↓
    Trigger Sync Job
         ↓
  [Success?] → No → Upload Logs → Cleanup → Fail ❌
         ↓ Yes
   Display Statistics
         ↓
    Upload Logs
         ↓
      Cleanup
         ↓
       Done ✅
```

---

## Manual Triggering

### Trigger Daily Sync Manually

1. Go to GitHub repository
2. Click **Actions** tab
3. Select **Daily RepoSync** workflow
4. Click **Run workflow** button
5. Select branch (usually `main`)
6. Click **Run workflow**

### Trigger CI/CD Manually
Simply push code to `main` or `develop` branch, or create a pull request.

---

## Monitoring and Debugging

### View Workflow Runs
1. Go to **Actions** tab in GitHub
2. Select the workflow (CI/CD or Daily Sync)
3. Click on a specific run to see details

### View Logs
1. Open a workflow run
2. Click on a specific job
3. Expand steps to see detailed logs
4. Download artifacts for service logs

### Common Issues and Solutions

#### ❌ Build Fails with Java Version Error
**Problem:** Java 17 not compatible (should be Java 21)

**Solution:** Update workflows:
```yaml
- name: Set up Java 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
```

#### ❌ Docker Build Fails
**Problem:** Invalid Docker credentials

**Solution:** 
1. Verify `DOCKER_USERNAME` and `DOCKER_PASSWORD` secrets
2. Generate new Docker access token if needed
3. Ensure Docker Hub account is active

#### ❌ Kubernetes Deployment Fails
**Problem:** Invalid kubeconfig or insufficient permissions

**Solution:**
1. Verify `KUBE_CONFIG` secret is correct
2. Ensure service account has required permissions
3. Check cluster connectivity

#### ❌ Daily Sync Fails
**Problem:** Service startup timeout or API failures

**Solution:**
1. Check service logs in artifacts
2. Verify all secrets are configured
3. Increase sleep intervals if needed
4. Check Azure OpenAI and Milvus connectivity

#### ❌ Milvus Connection Failed
**Problem:** Milvus URI or token incorrect

**Solution:**
1. Verify `MILVUS_URI` format: `https://host:19530`
2. Check `MILVUS_TOKEN` is valid
3. Ensure Milvus cluster is accessible from GitHub runners

---

## Performance Optimization

### Build Speed
- ✅ Maven dependency caching enabled
- ✅ Docker layer caching configured
- ✅ Matrix builds run in parallel
- ✅ Tests skipped in daily sync for speed

### Resource Usage
- Each workflow runs on GitHub-hosted `ubuntu-latest` runners
- 2-core CPU, 7 GB RAM, 14 GB SSD available
- Sufficient for building and running microservices

### Recommended Improvements
1. **Add timeouts** to prevent hung jobs
2. **Implement retry logic** for API calls
3. **Use build cache** for faster Maven builds
4. **Add notifications** for failed runs (Slack/Email)

---

## Security Best Practices

✅ **Secrets Management**
- All sensitive data stored in GitHub Secrets
- Never commit credentials to repository
- Use minimal permission tokens

✅ **Access Control**
- Docker Hub token with read/write access only
- GitHub token with `repo` scope only
- Kubernetes service account with namespace-specific permissions

✅ **Network Security**
- Services communicate via localhost in daily sync
- Kubernetes services use ClusterIP (internal only)
- Milvus accessed via secure TLS connection

---

## Future Enhancements

### Planned Features
1. ✅ Update to Java 21
2. ✅ Add Slack/Email notifications
3. ✅ Implement blue-green deployment
4. ✅ Add integration tests in CI pipeline
5. ✅ Create staging environment
6. ✅ Add rollback mechanism
7. ✅ Implement canary deployments
8. ✅ Add performance testing
9. ✅ Create monitoring dashboards

### Suggested Workflow Additions
```yaml
# Example: Slack Notification
- name: Notify Slack
  if: failure()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

---

## Summary

The GitHub Actions pipelines provide a comprehensive automation solution for the RepoSync project:

### CI/CD Pipeline
- 🔄 **Continuous Integration:** Automated testing on every push/PR
- 🐳 **Docker Build:** Automatic image creation and registry push
- 🚀 **Kubernetes Deployment:** Automated deployment to cluster
- ✅ **Quality Gates:** Tests must pass before deployment

### Daily Sync Pipeline
- ⏰ **Scheduled Execution:** Runs daily at 8:00 AM UTC
- 🔄 **Automated Sync:** Fetches repos, processes documents, updates Milvus
- 📊 **Status Reporting:** Detailed statistics and logs
- 🛡️ **Error Handling:** Cleanup and artifact upload on failure

Both pipelines work together to ensure:
- Code quality and reliability
- Automated deployments
- Daily data synchronization
- Complete audit trail via logs and artifacts

---

## Quick Reference

### Pipeline Files
- `.github/workflows/ci-cd.yml` - Build, test, and deploy
- `.github/workflows/daily-sync.yml` - Daily synchronization

### Key Commands
```bash
# Build locally
mvn clean install

# Build without tests
mvn clean package -DskipTests

# Run specific service
cd orchestrator-service
mvn spring-boot:run

# Trigger sync manually
curl -X POST http://localhost:8080/api/orchestrator/sync
```

---

## 3. Dependency Updates Pipeline (`dependency-updates.yml`)

**Location:** `.github/workflows/dependency-updates.yml`

### Purpose
Automates weekly dependency updates and comprehensive security scanning to keep the project secure and up-to-date.

### Trigger Events
- **Scheduled**: Every Monday at 9:00 AM UTC
- **Manual**: Via GitHub Actions UI (workflow_dispatch)
- **Pull Request**: When any `pom.xml` file is modified (validation only)

### Pipeline Jobs

#### Job 1: Dependency Security Scan
**Job Name:** `dependency-check`

**What it does:**
- ✅ Runs OWASP Dependency Check for CVE scanning
- ✅ Checks for outdated dependencies
- ✅ Generates vulnerability reports
- ✅ Uploads reports as artifacts (30-day retention)

**Reports Generated:**
- OWASP Dependency Check Report (HTML format)
- Dependency Updates List (TXT format)

**Failure Threshold:** CVSS score ≥ 7

---

#### Job 2: Dependency Tree Analysis
**Job Name:** `maven-dependency-tree`

**What it does:**
- ✅ Generates complete Maven dependency tree
- ✅ Helps identify transitive dependencies
- ✅ Useful for debugging dependency conflicts

**Output:** Dependency tree artifact (TXT format)

---

#### Job 3: Security Vulnerability Audit
**Job Name:** `security-audit`

**What it does:**
- ✅ Runs Trivy vulnerability scanner
- ✅ Scans for HIGH, MEDIUM, and CRITICAL vulnerabilities
- ✅ Uploads results to GitHub Security tab (SARIF format)
- ✅ Creates security alerts in GitHub Advanced Security

**Integration:** Results appear in repository's Security tab

---

#### Job 4: License Compliance Check
**Job Name:** `license-check`

**What it does:**
- ✅ Scans all dependencies for license information
- ✅ Generates third-party license report
- ✅ Helps ensure license compliance

**Output:** All licenses report (TXT format)

---

#### Job 5: Create Dependency Update PR
**Job Name:** `create-update-pr`

**Dependencies:** Requires `dependency-check` and `security-audit` to pass

**What it does:**
- ✅ Updates dependencies to latest stable versions
- ✅ Builds and tests with updated dependencies
- ✅ Creates Pull Request if successful
- ✅ Creates GitHub Issue if updates fail

**Runs on:** Only scheduled or manual triggers (not on PRs)

**PR Details:**
- Title: "🔄 Weekly Dependency Updates"
- Branch: `dependency-updates/weekly-{run_number}`
- Labels: `dependencies`, `automated`, `security`
- Includes: Test results, security reports, review checklist

**On Failure:** Creates issue titled "⚠️ Dependency Update Failed" with details

---

#### Job 6: Generate Security Summary
**Job Name:** `summary`

**Dependencies:** Runs after all other jobs complete

**What it does:**
- ✅ Aggregates results from all jobs
- ✅ Creates GitHub Actions Summary with status
- ✅ Lists all generated reports

**Always runs** regardless of other job outcomes

---

### Renovate Bot Integration

**Configuration:** `.github/renovate.json`

**Features:**
- 🔄 Intelligent dependency grouping (Spring Boot, Azure SDK, Maven plugins, etc.)
- 📅 Scheduled updates (Mondays before 10 AM)
- 🔒 Security updates processed immediately
- ✅ Minimum release age for stability (3 days)
- 📊 Dependency Dashboard in GitHub Issues
- 🏷️ Automatic labeling and PR creation

**Package Rules:**
1. **Spring Boot**: All Spring dependencies grouped together
2. **Maven Plugins**: Plugin updates grouped
3. **Azure SDK**: Azure dependencies grouped
4. **Milvus SDK**: Milvus dependencies grouped
5. **Security**: High priority, immediate processing
6. **Major Updates**: Separate PRs with "requires-review" label

---

### Security Tools Used

| Tool | Purpose | Output Format |
|------|---------|---------------|
| **OWASP Dependency Check** | CVE scanning from NVD | HTML, JSON |
| **Trivy** | Multi-purpose security scanner | SARIF |
| **Maven Versions Plugin** | Dependency version analysis | Console, TXT |
| **License Maven Plugin** | License compliance | TXT |

---

### Workflow Artifacts

All reports are available as downloadable artifacts:

| Artifact | Description | Retention |
|----------|-------------|-----------|
| `owasp-dependency-check-report` | Security vulnerability report | 30 days |
| `dependency-updates-report` | List of outdated dependencies | 30 days |
| `dependency-tree` | Full dependency graph | 30 days |
| `trivy-security-report` | Trivy scan results (SARIF) | 30 days |
| `license-report` | Third-party licenses | 30 days |

---

### Required Secrets

**None** - This workflow does not require additional secrets beyond `GITHUB_TOKEN` (automatically provided)

---

### How It Works Together

```
Monday 9:00 AM UTC
       │
       ▼
┌──────────────────────────────────────┐
│  Dependency Updates Pipeline Starts  │
└───────────────┬──────────────────────┘
                │
    ┌───────────┴───────────┐
    │                       │
    ▼                       ▼
┌─────────┐         ┌──────────────┐
│Security │         │  Dependency  │
│  Scans  │         │   Analysis   │
└────┬────┘         └──────┬───────┘
     │                     │
     └──────────┬──────────┘
                ▼
        ┌──────────────┐
        │   All Pass?  │
        └──────┬───────┘
               │
        ┌──────┴──────┐
        │             │
        ▼             ▼
    ┌─────┐       ┌──────┐
    │ YES │       │  NO  │
    └──┬──┘       └───┬──┘
       │              │
       ▼              ▼
  ┌─────────┐   ┌──────────┐
  │Create PR│   │Create    │
  │         │   │Issue     │
  └────┬────┘   └──────────┘
       │
       ▼
  ┌─────────┐
  │ CI/CD   │  ◄── Existing pipeline runs on PR
  │ Runs    │
  └────┬────┘
       │
       ▼
  ┌─────────┐
  │ Manual  │
  │ Review  │
  └────┬────┘
       │
       ▼
  ┌─────────┐
  │  Merge  │
  └─────────┘
```

---

### Documentation

For detailed information about the dependency updates pipeline, see:
📄 **[Dependency Updates Pipeline Documentation](../docs/readmes/DEPENDENCY_UPDATES_PIPELINE.md)**

---

### Important URLs
- GitHub Actions: `https://github.com/YOUR-ORG/YOUR-REPO/actions`
- Docker Hub: `https://hub.docker.com/r/reposync/`
- Kubernetes Dashboard: Configure based on your cluster

---

**Last Updated:** January 8, 2026  
**Project:** RepoSync Microservices  
**Version:** 1.0.0

