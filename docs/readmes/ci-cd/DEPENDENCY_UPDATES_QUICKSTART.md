# Dependency Updates Quick Start Guide

## 🚀 Getting Started

This guide helps you quickly understand and use the automated dependency updates pipeline.

## ⚡ Quick Actions

### Run Dependency Scan Manually

1. Go to GitHub repository
2. Click **Actions** tab
3. Select **Dependency Updates & Security Checks**
4. Click **Run workflow** button
5. Select branch (usually `main`)
6. Click green **Run workflow** button

### View Security Reports

1. Go to **Actions** → **Dependency Updates & Security Checks**
2. Click on latest workflow run
3. Scroll to **Artifacts** section
4. Download desired report

### Review Weekly Update PR

Every Monday at 9 AM UTC, an automated PR is created if updates are available:

1. Check for PR titled "🔄 Weekly Dependency Updates"
2. Review changes in **Files changed** tab
3. Download security reports from artifacts
4. Check that CI/CD pipeline passes
5. Approve and merge if everything looks good

## 📋 What Gets Scanned

✅ **All Maven dependencies** across all services:
- Spring Boot and Spring Framework
- Azure OpenAI SDK
- Milvus SDK
- Third-party libraries

✅ **Security vulnerabilities** (CVEs)
- Known vulnerabilities from NVD database
- Severity levels: CRITICAL, HIGH, MEDIUM, LOW

✅ **License compliance**
- All third-party licenses
- Potential license conflicts

✅ **Outdated versions**
- Available updates for dependencies
- Available updates for Maven plugins

## 🔐 Security Scanning Tools

| Tool | What It Checks | Report Format |
|------|---------------|---------------|
| **OWASP Dependency Check** | Known CVEs in dependencies | HTML |
| **Trivy** | Comprehensive security scan | SARIF (GitHub Security) |
| **Maven Versions** | Outdated dependencies | Text |
| **License Plugin** | License compliance | Text |

## 📊 Understanding Reports

### OWASP Dependency Check Report

**Location:** Workflow artifacts → `owasp-dependency-check-report`

**How to read:**
- Open HTML file in browser
- Check **Summary** section for vulnerability count
- Review **Dependencies** section for details
- Focus on HIGH and CRITICAL severity items

**Action required if:**
- Red "FAIL" badge appears
- CVSS score ≥ 7.0

### Trivy Security Report

**Location:** Repository → **Security** tab → **Code scanning**

**How to read:**
- View alerts in GitHub Security interface
- Click alert for detailed information
- Check severity and recommendations

**Action required if:**
- CRITICAL or HIGH severity alerts appear

### Dependency Updates Report

**Location:** Workflow artifacts → `dependency-updates-report`

**How to read:**
```
[INFO] The following dependencies in Dependencies have newer versions:
[INFO]   org.springframework.boot:spring-boot .......... 3.2.1 -> 3.2.5
[INFO]   com.azure:azure-ai-openai .................. 1.0.0-beta.6 -> 1.0.0
```

**Action:**
- Review available updates
- PR will be created automatically with updates

### License Report

**Location:** Workflow artifacts → `license-report`

**How to read:**
- Lists all third-party dependencies
- Shows license for each
- Check for incompatible licenses (GPL, AGPL with commercial projects)

## 🔄 Automated PR Process

### What Happens Automatically

```
Monday 9:00 AM UTC
       ↓
Security Scans Run
       ↓
If vulnerabilities found → Issue created
       ↓
Dependencies Updated
       ↓
Build & Test
       ↓
If tests pass → PR created
If tests fail → Issue created
```

### PR Contents

**Title:** 🔄 Weekly Dependency Updates

**Branch:** `dependency-updates/weekly-{number}`

**Labels:** 
- `dependencies`
- `automated`
- `security`

**Includes:**
- List of updated dependencies
- Test results
- Links to security reports
- Review checklist

## ✅ Review Checklist

When reviewing dependency update PR:

- [ ] Check PR description for list of changes
- [ ] Download and review OWASP report
- [ ] Check GitHub Security tab for new alerts
- [ ] Verify CI/CD pipeline passes
- [ ] Review major version updates for breaking changes
- [ ] Check changelogs for significant updates
- [ ] Test locally if concerned about changes
- [ ] Approve and merge

## 🛠️ Manual Dependency Commands

Run these commands locally when needed:

### Check for vulnerabilities
```bash
mvn org.owasp:dependency-check-maven:check
```

### Check for outdated dependencies
```bash
mvn versions:display-dependency-updates
```

### Check for outdated plugins
```bash
mvn versions:display-plugin-updates
```

### Generate dependency tree
```bash
mvn dependency:tree
```

### Check licenses
```bash
mvn license:add-third-party
```

### Update to latest versions (careful!)
```bash
mvn versions:use-latest-releases
```

## 🚨 Handling Security Issues

### Critical Vulnerability Found

1. **Check the alert details**
   - Which dependency is affected?
   - What's the vulnerability?
   - Is there a fix available?

2. **Update immediately**
   ```bash
   # Update specific dependency
   mvn versions:use-latest-versions -Dincludes=group:artifact
   ```

3. **Test thoroughly**
   ```bash
   mvn clean install
   mvn test
   ```

4. **Deploy quickly** if tests pass

### False Positive CVE

1. Research the CVE to confirm it's a false positive
2. Add to suppression file: `dependency-check-suppressions.xml`
   ```xml
   <suppress>
       <notes>Reason: This CVE doesn't apply because...</notes>
       <cve>CVE-2024-12345</cve>
   </suppress>
   ```
3. Commit and push the suppression file
4. Re-run workflow

## 📅 Schedule

| Day | Time (UTC) | Activity |
|-----|-----------|----------|
| **Monday** | 9:00 AM | Dependency scan runs |
| **Monday** | ~9:30 AM | PR created (if updates available) |
| **Monday** | 8:00 AM | Daily sync runs (separate pipeline) |
| **Any time** | On PR | Validation runs when pom.xml changes |

## 🔔 Notifications

You'll receive notifications for:
- ✅ Successful dependency update PR
- ❌ Failed dependency update (issue created)
- 🔒 New security vulnerabilities found

**Configure notifications:**
- Repository → Settings → Notifications
- Or GitHub account settings

## 💡 Best Practices

### DO ✅
- Review and merge weekly update PRs promptly
- Test major updates in staging before production
- Document reasons for CVE suppressions
- Keep suppression file minimal and up-to-date
- Monitor security alerts regularly

### DON'T ❌
- Ignore security update PRs
- Suppress CVEs without investigation
- Skip testing after major updates
- Disable the workflow
- Merge without reviewing changes

## 🆘 Troubleshooting

### PR Not Created

**Possible reasons:**
- No updates available
- Security scan failed
- Build/test failed

**Check:**
- Workflow run logs
- Recent issues for failure notifications

### Build Fails After Update

**Solution:**
1. Check workflow logs for error
2. Review dependency changelog
3. Check for breaking API changes
4. Update code to handle changes
5. Push fix to PR branch

### Too Many False Positives

**Solution:**
- Add suppressions with good documentation
- Consider updating suppression threshold
- Report false positives to OWASP

## 📞 Getting Help

1. **Check workflow logs** - Most issues have detailed error messages
2. **Review documentation** - See `DEPENDENCY_UPDATES_PIPELINE.md`
3. **Check dependency changelog** - Look for breaking changes
4. **Search CVE database** - Verify security issues
5. **Team discussion** - Discuss in PR comments

## 🔗 Useful Links

- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [National Vulnerability Database](https://nvd.nist.gov/)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
- [Maven Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/)
- [Full Pipeline Documentation](DEPENDENCY_UPDATES_PIPELINE.md)

---

**Need more details?** See the complete [Dependency Updates Pipeline Documentation](DEPENDENCY_UPDATES_PIPELINE.md)

