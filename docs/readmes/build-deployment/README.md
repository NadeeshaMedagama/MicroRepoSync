# 🔨 Build & Deployment Documentation

This directory contains documentation related to building, compiling, and deploying the RepoSync microservices application.

## 📚 Documents in This Section

### [BUILD_STATUS.md](BUILD_STATUS.md) 📊
**Current build status and health**
- Latest build results
- Module compilation status
- Known build issues
- Build environment requirements
- Maven configuration

**Use this for**: Checking current build health and status

---

### [BUILD_FIX_SUMMARY.md](BUILD_FIX_SUMMARY.md) 🔧
**Solutions to common build issues**
- Common compilation errors and fixes
- Dependency resolution problems
- Maven plugin issues
- Checkstyle violations
- PMD and SpotBugs issues
- Quick fixes and workarounds

**Use this for**: Troubleshooting build failures

---

### [JAVA21_BUILD_FIX.md](JAVA21_BUILD_FIX.md) ☕
**Java 21 migration and specific fixes**
- Java 21 upgrade guide
- Compatibility issues
- Deprecated API replacements
- Module system considerations
- Performance optimizations

**Use this for**: Java 21 specific build problems

---

## 🚀 Quick Build Commands

### Build Everything
```bash
# Full build with tests
mvn clean install

# Build without tests (faster)
mvn clean package -DskipTests

# Build skipping code quality checks
mvn clean package -DskipTests -Dcheckstyle.skip=true
```

### Build Specific Service
```bash
# Build only monitoring service
mvn clean package -pl monitoring-service -am -DskipTests

# Build github-service and its dependencies
mvn clean package -pl github-service -am -DskipTests
```

### Check Build Status
```bash
# Verify all modules compile
mvn clean compile

# Run tests
mvn test

# Run code quality checks
mvn checkstyle:check pmd:check spotbugs:check
```

---

## 🔧 Common Build Issues

### Issue: Checkstyle Violations
**Solution**: See [BUILD_FIX_SUMMARY.md](BUILD_FIX_SUMMARY.md#checkstyle-violations)
```bash
mvn clean package -Dcheckstyle.skip=true
```

### Issue: Java Version Mismatch
**Solution**: See [JAVA21_BUILD_FIX.md](JAVA21_BUILD_FIX.md)
```bash
# Verify Java version
java -version
# Should show Java 21
```

### Issue: Dependency Conflicts
**Solution**: See [BUILD_FIX_SUMMARY.md](BUILD_FIX_SUMMARY.md#dependency-conflicts)
```bash
# Clean and rebuild
mvn clean install -U
```

### Issue: Out of Memory
**Solution**: Increase Maven memory
```bash
export MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
mvn clean package
```

---

## 📦 Build Artifacts

After successful build, artifacts are located in:

```
each-service/target/
├── {service-name}-1.0.0-SNAPSHOT.jar     # Executable JAR
├── {service-name}-1.0.0-SNAPSHOT.jar.original  # Before Spring Boot repackaging
├── classes/                               # Compiled classes
├── generated-sources/                     # Generated code
├── maven-archiver/                        # Maven metadata
└── maven-status/                          # Build status
```

---

## 🐳 Docker Build

### Build Docker Images
```bash
# Build all service images
docker-compose build

# Build specific service image
docker build -f github-service/Dockerfile -t reposync/github-service:latest .

# Build with no cache
docker-compose build --no-cache
```

### Build and Run
```bash
# Build and start all services
docker-compose up -d --build

# Build specific service and start
docker-compose up -d --build monitoring-service
```

---

## ☸️ Kubernetes Deployment

### Prerequisites
```bash
# Ensure Docker images are built
mvn clean package -DskipTests
docker-compose build
```

### Deploy to Kubernetes
```bash
# Apply all manifests
kubectl apply -f k8s/

# Deploy specific service
kubectl apply -f k8s/07-monitoring-service.yaml

# Check deployment status
kubectl get pods -n reposync
kubectl get services -n reposync
```

---

## 🔍 Build Verification

### Verify Local Build
```bash
# Run verification script
./docs/scripts/verify-build.sh
```

### Manual Verification Checklist
- [ ] All modules compile successfully
- [ ] No checkstyle violations (or acceptable level)
- [ ] All tests pass
- [ ] JAR files created in target/ directories
- [ ] Docker images build successfully
- [ ] Services start without errors

---

## 📊 Build Profiles

### Development Profile
```bash
mvn clean package -Pdev -DskipTests
```

### Production Profile
```bash
mvn clean package -Pprod
```

### Skip Quality Checks (faster builds)
```bash
mvn clean package -DskipTests -Dcheckstyle.skip=true -Dpmd.skip=true -Dspotbugs.skip=true
```

---

## 🛠️ Build Tools & Plugins

The project uses these Maven plugins:

- **maven-compiler-plugin** (3.13.0) - Java compilation
- **spring-boot-maven-plugin** - Spring Boot packaging
- **maven-checkstyle-plugin** (3.3.1) - Code style
- **maven-pmd-plugin** (3.21.2) - Code analysis
- **spotbugs-maven-plugin** (4.8.3.0) - Bug detection
- **jacoco-maven-plugin** (0.8.11) - Code coverage
- **dependency-check-maven** (9.0.9) - Security scanning

See `pom.xml` for complete configuration.

---

## 🔗 Related Documentation

- [Setup Guides](../setup-guides/) - Environment setup
- [CI/CD](../ci-cd/) - Automated builds
- [Project Overview](../project-overview/) - Architecture

---

## 📝 Troubleshooting Steps

1. **Clean everything first**
   ```bash
   mvn clean
   docker-compose down -v
   ```

2. **Check Java version**
   ```bash
   java -version  # Should be Java 21
   mvn -version   # Check Maven version
   ```

3. **Update dependencies**
   ```bash
   mvn clean install -U
   ```

4. **Check for port conflicts**
   ```bash
   netstat -tulpn | grep LISTEN
   ```

5. **Review build logs**
   ```bash
   mvn clean install > build.log 2>&1
   cat build.log | grep ERROR
   ```

6. **Consult specific guides**
   - Checkstyle issues → [BUILD_FIX_SUMMARY.md](BUILD_FIX_SUMMARY.md)
   - Java 21 issues → [JAVA21_BUILD_FIX.md](JAVA21_BUILD_FIX.md)

---

## 📈 Build Performance Tips

1. **Use parallel builds**
   ```bash
   mvn clean package -T 4
   ```

2. **Skip unnecessary plugins**
   ```bash
   mvn package -DskipTests -Dcheckstyle.skip=true
   ```

3. **Use Maven Daemon**
   ```bash
   # Install mvnd for faster builds
   # https://github.com/apache/maven-mvnd
   ```

4. **Increase memory**
   ```bash
   export MAVEN_OPTS="-Xmx4g"
   ```

---

**Last Updated**: January 8, 2026

