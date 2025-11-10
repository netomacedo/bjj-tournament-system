# GitHub Setup Guide

This guide will walk you through setting up your BJJ Tournament System on GitHub and cloning it to your local machine.

## 📋 Prerequisites

- Git installed on your computer ([Download Git](https://git-scm.com/downloads))
- GitHub account ([Create account](https://github.com/join))
- Terminal/Command Prompt access

## 🚀 Step 1: Create GitHub Repository

### Option A: Via GitHub Website (Easiest)

1. **Go to GitHub**: Visit [https://github.com/new](https://github.com/new)

2. **Fill in repository details**:
   - **Repository name**: `bjj-tournament-system`
   - **Description**: `Professional BJJ Tournament Management System with IBJJF compliance - Java Spring Boot + PostgreSQL`
   - **Visibility**: Choose `Public` or `Private`
   - **DO NOT** initialize with README, .gitignore, or license (we already have these)

3. **Click "Create repository"**

4. **Copy the repository URL** (you'll need this):
   - SSH: `git@github.com:YOUR-USERNAME/bjj-tournament-system.git`
   - HTTPS: `https://github.com/YOUR-USERNAME/bjj-tournament-system.git`

### Option B: Via GitHub CLI

```bash
gh repo create bjj-tournament-system --public --description "Professional BJJ Tournament Management System"
```

## 📦 Step 2: Prepare Your Project

### Extract the project (if using the .tar.gz file)

```bash
# Extract the archive
tar -xzf bjj-tournament-system.tar.gz
cd bjj-tournament-system

# OR if you already have the bjj-tournament directory
cd bjj-tournament
```

## 🔧 Step 3: Initialize Git and Push to GitHub

```bash
# Initialize git repository
git init

# Add all files
git add .

# Create initial commit
git commit -m "Initial commit: BJJ Tournament Management System

- Complete IBJJF-compliant tournament system
- Spring Boot backend with PostgreSQL
- Athlete registration and management
- Automatic and manual bracket generation
- Real-time match scoring
- Docker support for easy deployment
- Comprehensive unit tests
- Full API documentation"

# Add your GitHub repository as remote (replace YOUR-USERNAME)
git remote add origin https://github.com/YOUR-USERNAME/bjj-tournament-system.git

# Push to GitHub
git push -u origin main

# If you get an error about 'master' vs 'main', rename the branch:
git branch -M main
git push -u origin main
```

## 🎉 Success! Your project is now on GitHub!

Visit: `https://github.com/YOUR-USERNAME/bjj-tournament-system`

## 💻 Step 4: Clone to Your Local Machine

Now you can clone the repository to any computer:

```bash
# Clone the repository
git clone https://github.com/YOUR-USERNAME/bjj-tournament-system.git

# Navigate into the project
cd bjj-tournament-system

# Verify everything is there
ls -la
```

## 🛠️ Step 5: Start Development

### Using Docker (Recommended)

```bash
# Start the application
docker-compose up -d

# Check if it's running
docker-compose ps

# View logs
docker-compose logs -f app

# Stop the application
docker-compose down
```

### Using Maven (Local Development)

```bash
# Install dependencies and run tests
mvn clean install

# Run the application
mvn spring-boot:run

# Run only tests
mvn test
```

## 📝 Step 6: Making Changes

```bash
# Create a new branch for your feature
git checkout -b feature/your-feature-name

# Make your changes...

# Stage your changes
git add .

# Commit your changes
git commit -m "Add: Description of your changes"

# Push to GitHub
git push origin feature/your-feature-name
```

## 🌿 Branch Naming Conventions

Use these prefixes for your branches:

- `feature/` - New features (e.g., `feature/react-frontend`)
- `fix/` - Bug fixes (e.g., `fix/scoring-calculation`)
- `docs/` - Documentation updates (e.g., `docs/api-examples`)
- `test/` - Test additions (e.g., `test/match-service`)
- `refactor/` - Code refactoring (e.g., `refactor/bracket-service`)

## 📋 Commit Message Guidelines

Follow this format:

```
Type: Short description (50 chars or less)

More detailed explanation if needed (wrap at 72 chars)

- Bullet points for specific changes
- Another change
- Final change
```

**Types:**
- `Add:` New feature or capability
- `Fix:` Bug fix
- `Update:` Modification to existing feature
- `Remove:` Remove feature or code
- `Docs:` Documentation changes
- `Test:` Test additions or modifications
- `Refactor:` Code refactoring without changing functionality

**Examples:**

```bash
git commit -m "Add: React frontend with drag-and-drop brackets"

git commit -m "Fix: Match winner determination logic for tied scores"

git commit -m "Update: IBJJF weight classes for 2025 rules"

git commit -m "Test: Add integration tests for bracket generation"
```

## 🔐 Step 7: Set Up SSH (Optional but Recommended)

SSH makes pushing/pulling easier without entering your password every time.

### Generate SSH Key

```bash
# Generate SSH key (press Enter for default location)
ssh-keygen -t ed25519 -C "your-email@example.com"

# Start SSH agent
eval "$(ssh-agent -s)"

# Add SSH key
ssh-add ~/.ssh/id_ed25519

# Copy public key to clipboard
# On Mac:
pbcopy < ~/.ssh/id_ed25519.pub

# On Linux:
cat ~/.ssh/id_ed25519.pub | xclip -selection clipboard

# On Windows (Git Bash):
cat ~/.ssh/id_ed25519.pub | clip
```

### Add SSH Key to GitHub

1. Go to [GitHub SSH Settings](https://github.com/settings/keys)
2. Click "New SSH key"
3. Paste your public key
4. Click "Add SSH key"

### Change Remote to SSH

```bash
git remote set-url origin git@github.com:YOUR-USERNAME/bjj-tournament-system.git
```

## 📊 Step 8: Project Structure on GitHub

Your GitHub repository will have this structure:

```
bjj-tournament-system/
├── .github/              # GitHub Actions (CI/CD) - Add later
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/             # Unit tests ✅
├── docker-compose.yml    # Docker configuration
├── Dockerfile           # Docker build
├── pom.xml              # Maven dependencies
├── .gitignore           # Git ignore rules
├── README.md            # Main documentation
├── API-EXAMPLES.md      # API testing guide
├── QUICK-START.md       # Quick start guide
└── ADVANCED-IDEAS.md    # Future enhancements
```

## 🎯 Step 9: Collaborate with Others

### Add Collaborators

1. Go to your repository on GitHub
2. Click "Settings" → "Collaborators"
3. Click "Add people"
4. Enter their GitHub username or email

### Review Pull Requests

When someone (or you) creates a pull request:

1. Go to the "Pull requests" tab
2. Review the changes
3. Add comments if needed
4. Click "Merge pull request" when ready

## 🚀 Step 10: Set Up GitHub Actions (Optional CI/CD)

Create `.github/workflows/maven.yml`:

```yaml
name: Java CI with Maven

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build with Maven
      run: mvn clean install
    
    - name: Run tests
      run: mvn test
```

This will automatically run tests on every push!

## 📱 Quick Reference Commands

### Daily Workflow

```bash
# Update your local repository
git pull origin main

# Create feature branch
git checkout -b feature/my-feature

# See what changed
git status

# Add and commit changes
git add .
git commit -m "Add: My feature description"

# Push to GitHub
git push origin feature/my-feature

# Switch back to main
git checkout main
```

### Common Git Commands

```bash
# Check status
git status

# View commit history
git log --oneline

# View remote repositories
git remote -v

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Discard local changes
git checkout -- filename

# View differences
git diff

# Create and switch to branch
git checkout -b branch-name

# Delete local branch
git branch -d branch-name

# Update from remote
git pull
```

## 🆘 Troubleshooting

### Problem: "Permission denied (publickey)"

**Solution**: Set up SSH keys (see Step 7) or use HTTPS instead of SSH

```bash
git remote set-url origin https://github.com/YOUR-USERNAME/bjj-tournament-system.git
```

### Problem: "fatal: refusing to merge unrelated histories"

**Solution**: Force merge (first time only)

```bash
git pull origin main --allow-unrelated-histories
```

### Problem: "Updates were rejected because the tip of your current branch is behind"

**Solution**: Pull first, then push

```bash
git pull origin main
git push origin main
```

### Problem: Large files rejected

**Solution**: Use Git LFS for large files

```bash
git lfs install
git lfs track "*.jar"
git add .gitattributes
git commit -m "Add Git LFS support"
```

## 🎓 Learning Resources

- **Git Documentation**: https://git-scm.com/doc
- **GitHub Guides**: https://guides.github.com/
- **Interactive Git Tutorial**: https://learngitbranching.js.org/
- **Git Cheat Sheet**: https://education.github.com/git-cheat-sheet-education.pdf

## 📞 Getting Help

If you encounter issues:

1. Check this guide's troubleshooting section
2. Search on [Stack Overflow](https://stackoverflow.com/questions/tagged/git)
3. Ask on [GitHub Community](https://github.community/)
4. Check repository issues

---

## ✅ Checklist

- [ ] GitHub account created
- [ ] Git installed locally
- [ ] Repository created on GitHub
- [ ] Project pushed to GitHub
- [ ] Repository cloned locally
- [ ] Docker/Maven working
- [ ] Tests passing (`mvn test`)
- [ ] Application running (`docker-compose up`)
- [ ] SSH keys configured (optional)
- [ ] Collaborators added (if needed)

**You're all set! Start building amazing features! 🥋🏆**

## 🔗 Useful Links

- **Your Repository**: `https://github.com/YOUR-USERNAME/bjj-tournament-system`
- **GitHub Desktop**: https://desktop.github.com/ (GUI alternative)
- **VS Code Git Extension**: Built-in Git support
- **IntelliJ Git Integration**: Built-in Git support

---

**Pro Tip**: Commit often, push frequently, and always write clear commit messages! 🚀
