# 🚀 Setup & Getting Started Guides

This directory contains all the guides you need to get the RepoSync microservices application up and running.

## 📚 Documents in This Section

### [QUICKSTART.md](QUICKSTART.md) ⚡
**Fast track to running the application**
- Minimal steps to get started
- Quick commands
- Essential configuration
- Common first-time issues

**Start here if**: You want to run the app ASAP

---

### [SETUP_CHECKLIST.md](SETUP_CHECKLIST.md) ✅
**Complete setup checklist**
- Step-by-step verification
- Prerequisites checklist
- Configuration validation
- Environment setup steps

**Start here if**: You want a guided setup process

---

### [LOCAL_SETUP_GUIDE.md](LOCAL_SETUP_GUIDE.md) 💻
**Detailed local development setup**
- Complete environment setup
- Dependency installation
- Database configuration
- Service configuration
- Local development workflow

**Start here if**: You're setting up for development

---

### [LOCAL_RUN_GUIDE.md](LOCAL_RUN_GUIDE.md) ▶️
**Running the application locally**
- Starting services individually
- Docker Compose usage
- Local testing procedures
- Debugging tips

**Start here if**: You need to run services locally

---

### [INTELLIJ_GUIDE.md](INTELLIJ_GUIDE.md) 🎯
**IntelliJ IDEA setup and configuration**
- IDE installation and setup
- Project import
- Run configurations
- Debugging setup
- Useful plugins

**Start here if**: You're using IntelliJ IDEA

---

### [QODANA_SETUP.md](QODANA_SETUP.md) 🔍
**Code quality analysis with Qodana**
- Qodana installation
- Quality checks configuration
- Running code analysis
- Understanding reports

**Start here if**: You want code quality analysis

---

### [VISUAL_GUIDE.md](VISUAL_GUIDE.md) 📊
**Visual diagrams and architecture**
- System architecture diagrams
- Service interaction flows
- Component diagrams
- Data flow visualizations

**Start here if**: You're a visual learner

---

## 🎯 Quick Start Flow

Follow this recommended order for first-time setup:

```
1. SETUP_CHECKLIST.md      → Verify prerequisites
2. LOCAL_SETUP_GUIDE.md    → Set up environment
3. QUICKSTART.md           → Run the application
4. INTELLIJ_GUIDE.md       → Configure your IDE (optional)
5. VISUAL_GUIDE.md         → Understand the architecture
```

---

## 💻 Prerequisites

Before starting, ensure you have:
- ✅ Java 21 (JDK)
- ✅ Maven 3.9+
- ✅ Docker & Docker Compose
- ✅ Git
- ✅ IntelliJ IDEA (recommended) or any Java IDE

See [SETUP_CHECKLIST.md](SETUP_CHECKLIST.md) for detailed requirements.

---

## 🚀 Fastest Way to Get Started

```bash
# 1. Clone the repository
git clone <repository-url>
cd Microservices_with_RepoSync

# 2. Set up environment variables
cp .env.example .env
# Edit .env with your credentials

# 3. Start all services
docker-compose up -d

# 4. Verify services are running
docker-compose ps

# 5. Access the application
# Orchestrator: http://localhost:8086
# Grafana: http://localhost:3000
```

For detailed instructions, see [QUICKSTART.md](QUICKSTART.md)

---

## 🛠️ Common Setup Tasks

### Setting Up Environment Variables
See: [LOCAL_SETUP_GUIDE.md](LOCAL_SETUP_GUIDE.md#environment-variables)

### Configuring IntelliJ
See: [INTELLIJ_GUIDE.md](INTELLIJ_GUIDE.md)

### Running Individual Services
See: [LOCAL_RUN_GUIDE.md](LOCAL_RUN_GUIDE.md)

### Troubleshooting Setup Issues
See: [SETUP_CHECKLIST.md](SETUP_CHECKLIST.md#troubleshooting)

---

## 🔗 Related Documentation

- [Build & Deployment](../build-deployment/) - Build configuration and fixes
- [Monitoring](../monitoring/) - Setting up monitoring
- [Project Overview](../project-overview/) - Understanding the project

---

## 🆘 Having Issues?

| Issue | Solution |
|-------|----------|
| Services won't start | Check [LOCAL_RUN_GUIDE.md](LOCAL_RUN_GUIDE.md#troubleshooting) |
| Environment setup fails | See [SETUP_CHECKLIST.md](SETUP_CHECKLIST.md) |
| IDE configuration issues | Review [INTELLIJ_GUIDE.md](INTELLIJ_GUIDE.md) |
| Can't understand architecture | Check [VISUAL_GUIDE.md](VISUAL_GUIDE.md) |

---

## 📝 Next Steps

After completing setup:
1. ✅ Run the application using [QUICKSTART.md](QUICKSTART.md)
2. ✅ Set up monitoring following [../monitoring/MONITORING_QUICKSTART.md](../monitoring/MONITORING_QUICKSTART.md)
3. ✅ Review the architecture in [VISUAL_GUIDE.md](VISUAL_GUIDE.md)
4. ✅ Check build status in [../build-deployment/BUILD_STATUS.md](../build-deployment/BUILD_STATUS.md)

---

**Last Updated**: January 8, 2026

