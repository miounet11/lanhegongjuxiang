#!/bin/bash
# 模块功能库发布脚本
# 使用方法: ./release.sh <version>
# 例如: ./release.sh 1.1.0

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查参数
if [ $# -ne 1 ]; then
    log_error "Usage: $0 <version>"
    log_error "Example: $0 1.2.0"
    exit 1
fi

VERSION=$1

# 验证版本号格式
if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?(\+[a-zA-Z0-9.]+)?$ ]]; then
    log_error "Invalid version format: $VERSION"
    log_error "Expected format: x.y.z[-pre-release][+build]"
    exit 1
fi

log_info "Starting release process for version $VERSION"

# 检查Git状态
if [ -n "$(git status --porcelain)" ]; then
    log_error "Working directory is not clean. Please commit or stash changes first."
    git status
    exit 1
fi

# 检查当前分支
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ] && [ "$CURRENT_BRANCH" != "master" ]; then
    log_warning "Not on main/master branch. Current branch: $CURRENT_BRANCH"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 1. 代码质量检查
log_info "Running code quality checks..."
./gradlew ktlintCheck detekt test
if [ $? -ne 0 ]; then
    log_error "Code quality checks failed. Please fix the issues before releasing."
    exit 1
fi
log_success "Code quality checks passed"

# 2. 构建检查
log_info "Building release version..."
./gradlew clean build
if [ $? -ne 0 ]; then
    log_error "Build failed. Please fix the build issues before releasing."
    exit 1
fi
log_success "Build completed successfully"

# 3. 更新版本号
log_info "Updating version to $VERSION..."
echo "version=$VERSION" > version.properties
echo "versionCode=$(( $(date +%s) / 86400 ))" >> version.properties
echo "versionName=$VERSION" >> version.properties
echo "buildTime=$(date +%Y-%m-%d)" >> version.properties
log_success "Version updated to $VERSION"

# 4. 更新CHANGELOG
log_info "Updating CHANGELOG..."
if [ -f CHANGELOG.md ]; then
    # 在CHANGELOG顶部添加新版本
    sed -i "1a ## [$VERSION] - $(date +%Y-%m-%d)\n\n### Added\n- \n\n### Changed\n- \n\n### Fixed\n- \n\n" CHANGELOG.md
    log_success "CHANGELOG updated"
else
    log_warning "CHANGELOG.md not found, skipping..."
fi

# 5. 提交更改
log_info "Committing changes..."
git add version.properties CHANGELOG.md
git commit -m "Release version $VERSION"
log_success "Changes committed"

# 6. 创建标签
log_info "Creating git tag..."
if git tag -l | grep -q "^v$VERSION$"; then
    log_warning "Tag v$VERSION already exists. Removing old tag..."
    git tag -d "v$VERSION"
    git push origin ":refs/tags/v$VERSION" 2>/dev/null || true
fi
git tag -a "v$VERSION" -m "Release version $VERSION"
log_success "Git tag v$VERSION created"

# 7. 推送到远程仓库
log_info "Pushing to remote repository..."
git push origin "$CURRENT_BRANCH"
git push origin "v$VERSION"
log_success "Pushed to remote repository"

# 8. 发布到Maven仓库（如果配置了）
if [ -f "gradle.properties" ] && grep -q "mavenCentral" gradle.properties; then
    log_info "Publishing to Maven Central..."
    ./gradlew publishToMavenCentral
    if [ $? -eq 0 ]; then
        log_success "Published to Maven Central"
    else
        log_warning "Failed to publish to Maven Central"
    fi
fi

# 9. 创建GitHub Release（如果配置了）
if command -v gh &> /dev/null; then
    log_info "Creating GitHub release..."
    gh release create "v$VERSION" \
        --title "Release v$VERSION" \
        --notes "Release version $VERSION" \
        --latest
    if [ $? -eq 0 ]; then
        log_success "GitHub release created"
    else
        log_warning "Failed to create GitHub release"
    fi
fi

log_success "🎉 Release $VERSION completed successfully!"
log_info ""
log_info "Next steps:"
log_info "1. Monitor CI/CD pipeline if applicable"
log_info "2. Update documentation if needed"
log_info "3. Notify stakeholders about the release"
log_info "4. Start development for next version"

exit 0
