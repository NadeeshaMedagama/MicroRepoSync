# RepoSync Pipeline Architecture

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         GITHUB ACTIONS PIPELINES                         │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────┐  ┌──────────────────────────────────┐
│       CI/CD PIPELINE             │  │    DAILY SYNC PIPELINE           │
│   (Continuous Deployment)        │  │  (Data Synchronization)          │
└──────────────────────────────────┘  └──────────────────────────────────┘
         │                                        │
         │ Trigger: Push/PR                      │ Trigger: Daily 8AM UTC
         │                                        │ or Manual
         ▼                                        ▼
┌─────────────────────┐              ┌─────────────────────────┐
│  1. BUILD & TEST    │              │  1. SETUP ENVIRONMENT   │
│  ✓ Java 21 Setup    │              │  ✓ Java 21 Setup        │
│  ✓ Maven Build      │              │  ✓ Build Services       │
│  ✓ Run Tests        │              │  ✓ Load Secrets         │
└─────────────────────┘              └─────────────────────────┘
         │                                        │
         │ [Tests Pass?]                         ▼
         ▼                            ┌─────────────────────────┐
┌─────────────────────┐              │  2. START SERVICES      │
│  2. BUILD DOCKER    │              │  ✓ GitHub Service       │
│     IMAGES          │              │  ✓ Document Processor   │
│  ✓ 5 Services       │              │  ✓ Embedding Service    │
│  ✓ Parallel Build   │              │  ✓ Milvus Service       │
│  ✓ Push to Registry │              │  ✓ Orchestrator         │
└─────────────────────┘              └─────────────────────────┘
         │                                        │
         │ [Only on main]                        ▼
         ▼                            ┌─────────────────────────┐
┌─────────────────────┐              │  3. EXECUTE SYNC JOB    │
│  3. DEPLOY TO K8S   │              │  ✓ Fetch Repos          │
│  ✓ Update Secrets   │              │  ✓ Process Documents    │
│  ✓ Update ConfigMap │              │  ✓ Generate Embeddings  │
│  ✓ Deploy Services  │              │  ✓ Store in Milvus      │
│  ✓ Verify Rollout   │              └─────────────────────────┘
└─────────────────────┘                         │
         │                                        ▼
         ▼                            ┌─────────────────────────┐
   ✅ DEPLOYED                        │  4. VERIFY & REPORT     │
                                      │  ✓ Check Status         │
                                      │  ✓ Display Stats        │
                                      │  ✓ Upload Logs          │
                                      │  ✓ Cleanup              │
                                      └─────────────────────────┘
                                                │
                                                ▼
                                          ✅ COMPLETED
```

## Service Communication Flow

```
                    ┌──────────────────────────────────────┐
                    │    ORCHESTRATOR SERVICE (8080)       │
                    │  • Coordinates entire workflow       │
                    │  • Manages sync job lifecycle        │
                    └──────────────────────────────────────┘
                              │
                              │ Orchestrates
                              ▼
        ┌─────────────────────┴─────────────────────┐
        │                     │                      │
        ▼                     ▼                      ▼
┌──────────────┐    ┌──────────────────┐    ┌──────────────┐
│   GITHUB     │    │    DOCUMENT      │    │   EMBEDDING  │
│   SERVICE    │───▶│    PROCESSOR     │───▶│   SERVICE    │
│   (8081)     │    │    SERVICE       │    │   (8083)     │
│              │    │    (8082)        │    │              │
│ • Fetch Repos│    │ • Extract Docs   │    │ • Azure AI   │
│ • Get READMEs│    │ • Chunk Content  │    │ • Generate   │
│ • Get APIs   │    │ • Parse Markdown │    │   Embeddings │
└──────────────┘    └──────────────────┘    └──────────────┘
                                                    │
                                                    │
                                                    ▼
                                            ┌──────────────┐
                                            │   MILVUS     │
                                            │   SERVICE    │
                                            │   (8084)     │
                                            │              │
                                            │ • Store      │
                                            │   Vectors    │
                                            │ • Search     │
                                            └──────────────┘
                                                    │
                                                    ▼
                                            ┌──────────────┐
                                            │   MILVUS DB  │
                                            │  (Cloud/19530│
                                            └──────────────┘
```

## Data Flow in Daily Sync

```
1. GITHUB REPOSITORIES
   ├─ Organization: ${REPOSYNC_ORGANIZATION}
   ├─ Filter: ${REPOSYNC_FILTER_KEYWORD}
   └─ Repositories Found: [repo1, repo2, ...]
          │
          ▼
2. DOCUMENT EXTRACTION
   ├─ README.md files
   ├─ API definition files (OpenAPI, Swagger)
   └─ Documents Extracted: [doc1, doc2, ...]
          │
          ▼
3. CHUNKING & PROCESSING
   ├─ Split documents into chunks
   ├─ Max chunk size: configurable
   └─ Chunks Created: [chunk1, chunk2, ...]
          │
          ▼
4. EMBEDDING GENERATION
   ├─ Azure OpenAI API
   ├─ Model: text-embedding-ada-002
   └─ Embeddings: [vector1, vector2, ...]
          │
          ▼
5. VECTOR STORAGE
   ├─ Milvus Collection: ${MILVUS_COLLECTION_NAME}
   ├─ Dimension: 1536 (Azure OpenAI)
   └─ Vectors Stored: X vectors
          │
          ▼
6. COMPLETION
   └─ Status: SUCCESS/FAILURE
   └─ Statistics: repos, docs, chunks, vectors
```

## Deployment Architecture

### Local Development
```
┌─────────────────────────────────────────────┐
│         DEVELOPER MACHINE                    │
│                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ Service  │  │ Service  │  │ Service  │  │
│  │   #1     │  │   #2     │  │   #3     │  │
│  └──────────┘  └──────────┘  └──────────┘  │
│                                              │
│  Config: .env file                           │
└─────────────────────────────────────────────┘
                    │
                    ▼
        ┌──────────────────────┐
        │   LOCAL MILVUS       │
        │   (Docker)           │
        └──────────────────────┘
```

### GitHub Actions (Daily Sync)
```
┌─────────────────────────────────────────────┐
│      GITHUB ACTIONS RUNNER                   │
│                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ Service  │  │ Service  │  │ Service  │  │
│  │   #1     │  │   #2     │  │   #3     │  │
│  │ (8081)   │  │ (8082)   │  │ (8083)   │  │
│  └──────────┘  └──────────┘  └──────────┘  │
│                                              │
│  Config: GitHub Secrets                      │
└─────────────────────────────────────────────┘
                    │
                    ▼
        ┌──────────────────────┐
        │   CLOUD MILVUS       │
        │   (Production)       │
        └──────────────────────┘
```

### Kubernetes Production
```
┌─────────────────────────────────────────────────────┐
│            KUBERNETES CLUSTER                        │
│  ┌──────────────────────────────────────────┐       │
│  │  Namespace: reposync                      │       │
│  │                                            │       │
│  │  ┌────────────┐  ┌────────────┐          │       │
│  │  │ Deployment │  │ Deployment │          │       │
│  │  │ github-svc │  │ processor  │  ...     │       │
│  │  └────────────┘  └────────────┘          │       │
│  │         │               │                 │       │
│  │         ▼               ▼                 │       │
│  │  ┌────────────┐  ┌────────────┐          │       │
│  │  │  Service   │  │  Service   │  ...     │       │
│  │  │ (ClusterIP)│  │ (ClusterIP)│          │       │
│  │  └────────────┘  └────────────┘          │       │
│  │                                            │       │
│  │  ┌──────────────────────────────┐         │       │
│  │  │  ConfigMap: reposync-config  │         │       │
│  │  └──────────────────────────────┘         │       │
│  │  ┌──────────────────────────────┐         │       │
│  │  │  Secret: reposync-secrets    │         │       │
│  │  └──────────────────────────────┘         │       │
│  └──────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────┘
                        │
                        ▼
            ┌──────────────────────┐
            │   EXTERNAL SERVICES  │
            │  • GitHub API        │
            │  • Azure OpenAI      │
            │  • Milvus Cloud      │
            └──────────────────────┘
```

## Security Architecture

```
┌─────────────────────────────────────────────────────┐
│                 SECURITY LAYERS                      │
└─────────────────────────────────────────────────────┘

1. GITHUB SECRETS (Encrypted at Rest)
   ├─ REPOSYNC_GITHUB_TOKEN
   ├─ AZURE_OPENAI_API_KEY
   ├─ MILVUS_TOKEN
   └─ KUBE_CONFIG

2. KUBERNETES SECRETS (Base64 Encoded)
   ├─ reposync-secrets
   │  ├─ REPOSYNC_GITHUB_TOKEN
   │  ├─ AZURE_OPENAI_API_KEY
   │  └─ MILVUS_TOKEN

3. ENVIRONMENT VARIABLES (Runtime)
   ├─ Injected at container start
   ├─ Not persisted to disk
   └─ Scoped to pod/container

4. NETWORK SECURITY
   ├─ GitHub Actions: HTTPS only
   ├─ Kubernetes: ClusterIP (internal)
   ├─ Azure OpenAI: TLS 1.2+
   └─ Milvus: TLS connection

5. ACCESS CONTROL
   ├─ GitHub Token: repo scope only
   ├─ Azure API Key: endpoint-specific
   ├─ Milvus Token: collection-specific
   └─ Kubernetes RBAC: namespace-limited
```

## Pipeline Scheduling

```
TIME          PIPELINE              ACTION
─────────────────────────────────────────────────────
00:00 UTC     -                     (Quiet time)
02:00 UTC     -                     (Quiet time)
04:00 UTC     -                     (Quiet time)
06:00 UTC     -                     (Quiet time)
08:00 UTC     Daily Sync ⏰         START SYNC JOB
08:05 UTC     Daily Sync            Processing...
08:30 UTC     Daily Sync            Complete ✅
10:00 UTC     -                     (Quiet time)
12:00 UTC     -                     (Quiet time)
14:00 UTC     -                     (Quiet time)
16:00 UTC     -                     (Quiet time)
18:00 UTC     -                     (Quiet time)
20:00 UTC     -                     (Quiet time)
22:00 UTC     -                     (Quiet time)

ANY TIME      CI/CD Pipeline        On push/PR to main/develop
```

## Error Handling & Recovery

```
┌─────────────────────────────────────────────┐
│          ERROR HANDLING FLOW                 │
└─────────────────────────────────────────────┘

START
  │
  ▼
Execute Step
  │
  ├─── [Success] ──▶ Next Step
  │
  └─── [Failure] ──▶ Error Handler
                        │
                        ├─ Upload Logs (always)
                        ├─ Cleanup Resources (always)
                        ├─ Mark Job as Failed
                        └─ Exit with Error Code
                              │
                              ▼
                    Notification (Optional)
                    • Email
                    • Slack
                    • PagerDuty
```

## Monitoring & Observability

```
┌─────────────────────────────────────────────────────┐
│              OBSERVABILITY STACK                     │
└─────────────────────────────────────────────────────┘

LOGS
├─ GitHub Actions Console Logs
├─ Service-specific log files
├─ Uploaded as workflow artifacts
└─ Retention: 90 days

METRICS
├─ Build duration
├─ Test pass/fail rate
├─ Docker image size
├─ Deployment success rate
└─ Sync job statistics

TRACES (Future)
├─ Distributed tracing
├─ Service dependencies
└─ Performance bottlenecks

ALERTS (Future)
├─ Failed builds
├─ Failed deployments
├─ Failed sync jobs
└─ Resource exhaustion
```

## Cost Optimization

```
RESOURCE              COST STRATEGY
──────────────────────────────────────────────
GitHub Actions        • Free tier: 2000 min/month
                      • Optimize build time
                      • Cache dependencies
                      • Skip tests when needed

Docker Hub            • Free tier: 1 namespace
                      • Use layer caching
                      • Clean old images

Kubernetes            • Right-size pods
                      • Use auto-scaling
                      • Set resource limits

Azure OpenAI          • Pay per token
                      • Batch requests
                      • Cache embeddings

Milvus Cloud          • Pay per storage
                      • Optimize collection
                      • Regular cleanup
```

## Performance Metrics

```
PIPELINE          DURATION      SUCCESS RATE
───────────────────────────────────────────────
CI/CD Full        ~8-12 min     95%+
├─ Build & Test   ~3-4 min      98%
├─ Docker Build   ~3-5 min      97%
└─ K8s Deploy     ~2-3 min      96%

Daily Sync        ~5-15 min     90%+
├─ Setup          ~2 min        99%
├─ Services Start ~3 min        95%
├─ Sync Execute   ~2-10 min     85%
└─ Cleanup        ~30 sec       99%

BOTTLENECKS
├─ Azure OpenAI API rate limits
├─ GitHub API rate limits
└─ Milvus insertion speed
```

## Scalability Considerations

```
COMPONENT             CURRENT      FUTURE SCALE
────────────────────────────────────────────────
Microservices         5            10+
Repositories/Sync     10-50        100-500
Documents/Sync        50-200       500-2000
Vectors/Sync          500-2000     5000-20000
Sync Frequency        Daily        Hourly
GitHub Actions        Sequential   Parallel
Kubernetes Pods       1 per svc    HPA enabled
```

---

## Quick Reference

### Pipeline URLs
```
CI/CD Pipeline:
https://github.com/{ORG}/{REPO}/actions/workflows/ci-cd.yml

Daily Sync Pipeline:
https://github.com/{ORG}/{REPO}/actions/workflows/daily-sync.yml
```

### Key Metrics to Monitor
- ✅ Pipeline success rate
- ✅ Build duration
- ✅ Sync job completion time
- ✅ Number of vectors stored
- ✅ API rate limit usage
- ✅ Error rates

### Critical Paths
1. **Daily Sync**: GitHub API → Document Processing → Embedding → Milvus Storage
2. **CI/CD**: Code → Build → Test → Docker → Deploy → Verify

---

**Document Version:** 1.0  
**Last Updated:** January 8, 2026  
**Author:** RepoSync Team

