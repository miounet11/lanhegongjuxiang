#!/bin/bash

# 蓝河助手测试运行脚本
# 使用方法: ./run-tests.sh [test-type]
# test-type: unit, integration, performance, coverage, all

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 检查Gradle权限
check_gradle() {
    if [ ! -x "./gradlew" ]; then
        print_info "赋予Gradle执行权限..."
        chmod +x ./gradlew
    fi
}

# 清理之前的结果
clean_project() {
    print_info "清理之前的构建结果..."
    ./gradlew clean
}

# 运行单元测试
run_unit_tests() {
    print_info "运行单元测试..."
    
    if ./gradlew testDebugUnitTest; then
        print_success "单元测试通过"
        
        # 显示测试结果摘要
        if [ -f "app/build/reports/tests/testDebugUnitTest/index.html" ]; then
            print_info "测试报告: app/build/reports/tests/testDebugUnitTest/index.html"
        fi
    else
        print_error "单元测试失败"
        exit 1
    fi
}

# 运行集成测试
run_integration_tests() {
    print_info "运行集成测试..."
    print_warning "需要连接Android模拟器或设备"
    
    # 检查设备连接
    if ! adb devices | grep -q "device$"; then
        print_warning "未检测到Android设备，跳过集成测试"
        return
    fi
    
    if ./gradlew connectedDebugAndroidTest; then
        print_success "集成测试通过"
        
        # 显示测试结果摘要
        if [ -f "app/build/reports/androidTests/connected/index.html" ]; then
            print_info "测试报告: app/build/reports/androidTests/connected/index.html"
        fi
    else
        print_error "集成测试失败"
        exit 1
    fi
}

# 运行性能测试
run_performance_tests() {
    print_info "运行性能测试..."
    
    # 检查设备连接
    if ! adb devices | grep -q "device$"; then
        print_warning "未检测到Android设备，跳过性能测试"
        return
    fi
    
    if ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lanhe.gongjuxiang.performance.PerformanceTest; then
        print_success "性能测试通过"
    else
        print_error "性能测试失败"
        exit 1
    fi
}

# 生成测试覆盖率报告
generate_coverage() {
    print_info "生成测试覆盖率报告..."
    
    # 生成单元测试覆盖率
    if ./gradlew jacocoTestReport; then
        print_success "单元测试覆盖率报告生成成功"
        
        if [ -f "app/build/reports/jacoco/jacocoTestReport/html/index.html" ]; then
            print_info "覆盖率报告: app/build/reports/jacoco/jacocoTestReport/html/index.html"
            
            # 提取覆盖率百分比
            if command -v python3 &> /dev/null; then
                COVERAGE=$(python3 -c "
import re
with open('app/build/reports/jacoco/jacocoTestReport/html/index.html', 'r') as f:
    content = f.read()
match = re.search(r'Total.*?(\d+%)', content)
if match:
    print(match.group(1))
else:
    print('0%')
" 2>/dev/null || echo "0%")
                print_info "测试覆盖率: $COVERAGE"
            fi
        fi
    else
        print_error "覆盖率报告生成失败"
        exit 1
    fi
}

# 运行Lint检查
run_lint() {
    print_info "运行Lint检查..."
    
    if ./gradlew lintDebug; then
        print_success "Lint检查通过"
        
        if [ -f "app/build/reports/lint-results-debug.html" ]; then
            print_info "Lint报告: app/build/reports/lint-results-debug.html"
        fi
    else
        print_warning "Lint检查发现问题，请查看报告"
    fi
}

# 构建APK
build_apk() {
    print_info "构建Debug APK..."
    
    if ./gradlew assembleDebug; then
        print_success "APK构建成功"
        
        # 查找APK文件
        APK_PATH=$(find app/build/outputs/apk/debug -name "*.apk" | head -n1)
        if [ -f "$APK_PATH" ]; then
            print_info "APK位置: $APK_PATH"
            print_info "APK大小: $(du -h "$APK_PATH" | cut -f1)"
        fi
    else
        print_error "APK构建失败"
        exit 1
    fi
}

# 显示帮助信息
show_help() {
    echo "蓝河助手测试运行脚本"
    echo ""
    echo "使用方法:"
    echo "  ./run-tests.sh [test-type]"
    echo ""
    echo "测试类型:"
    echo "  unit        - 只运行单元测试"
    echo "  integration - 只运行集成测试（需要设备）"
    echo "  performance - 只运行性能测试（需要设备）"
    echo "  coverage    - 只生成测试覆盖率报告"
    echo "  lint        - 只运行Lint检查"
    echo "  build       - 只构建APK"
    echo "  quick       - 快速测试（单元测试 + Lint）"
    echo "  full        - 完整测试套件（除性能测试外）"
    echo "  all         - 运行所有测试（需要设备）"
    echo "  help        - 显示此帮助信息"
    echo ""
}

# 显示测试概要
show_summary() {
    print_info "测试运行完成！"
    echo ""
    echo "📊 生成的报告:"
    echo "  - 单元测试: app/build/reports/tests/testDebugUnitTest/"
    echo "  - 集成测试: app/build/reports/androidTests/connected/"
    echo "  - 覆盖率报告: app/build/reports/jacoco/jacocoTestReport/html/"
    echo "  - Lint报告: app/build/reports/lint-results-debug.html"
    echo "  - APK文件: app/build/outputs/apk/debug/"
    echo ""
    echo "📱 查看HTML报告:"
    echo "  在浏览器中打开对应的HTML文件查看详细报告"
    echo ""
}

# 主逻辑
main() {
    local test_type="${1:-all}"
    
    print_info "开始运行蓝河助手测试套件..."
    echo "测试类型: $test_type"
    echo ""
    
    check_gradle
    
    case "$test_type" in
        "unit")
            clean_project
            run_unit_tests
            ;;
        "integration")
            clean_project
            run_integration_tests
            ;;
        "performance")
            clean_project
            run_performance_tests
            ;;
        "coverage")
            run_unit_tests
            generate_coverage
            ;;
        "lint")
            clean_project
            run_lint
            ;;
        "build")
            clean_project
            build_apk
            ;;
        "quick")
            print_info "快速测试模式..."
            run_unit_tests
            run_lint
            ;;
        "full")
            print_info "完整测试模式..."
            clean_project
            run_unit_tests
            run_lint
            generate_coverage
            build_apk
            run_integration_tests
            ;;
        "all")
            print_info "运行所有测试..."
            clean_project
            run_unit_tests
            run_lint
            generate_coverage
            build_apk
            run_integration_tests
            run_performance_tests
            ;;
        "help"|"-h"|"--help")
            show_help
            exit 0
            ;;
        *)
            print_error "未知的测试类型: $test_type"
            show_help
            exit 1
            ;;
    esac
    
    show_summary
}

# 运行主函数
main "$@"
