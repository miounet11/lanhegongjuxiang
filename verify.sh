#!/bin/bash
# 蓝河助手快速验证脚本
# Version: 1.0.0
# Date: 2025-11-24

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_header() {
    echo -e "\n${BLUE}=== $1 ===${NC}\n"
}

# 检查前置条件
check_prerequisites() {
    print_header "检查前置条件"

    # 检查Java
    if ! command -v java &> /dev/null; then
        print_error "Java未安装"
        exit 1
    fi
    print_success "Java已安装: $(java -version 2>&1 | head -n 1)"

    # 检查ADB
    if ! command -v adb &> /dev/null; then
        print_error "ADB未安装"
        exit 1
    fi
    print_success "ADB已安装: $(adb version | head -n 1)"

    # 检查设备连接
    if ! adb devices | grep -q "device$"; then
        print_warning "没有检测到Android设备"
        echo "请连接Android设备或启动模拟器"
        read -p "是否继续? (y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        print_success "Android设备已连接"
    fi
}

# 编译项目
compile_project() {
    print_header "Step 1: 编译项目"

    print_info "清理项目..."
    ./gradlew clean > /dev/null 2>&1

    print_info "编译项目..."
    if ./gradlew build 2>&1 | tee build.log | grep -q "BUILD SUCCESSFUL"; then
        print_success "项目编译成功"
        return 0
    else
        print_error "项目编译失败"
        echo "查看 build.log 获取详细信息"
        return 1
    fi
}

# 运行单元测试
run_unit_tests() {
    print_header "Step 2: 运行单元测试"

    print_info "执行单元测试..."
    if ./gradlew test 2>&1 | tee test.log | grep -q "BUILD SUCCESSFUL"; then
        # 统计测试结果
        local total_tests=$(grep -E "tests?" test.log | tail -1 | grep -oE "[0-9]+ tests?" | grep -oE "[0-9]+")
        local failed_tests=$(grep -E "failed" test.log | tail -1 | grep -oE "[0-9]+ failed" | grep -oE "[0-9]+" || echo "0")

        if [ "$failed_tests" -eq "0" ]; then
            print_success "所有单元测试通过 (共 $total_tests 个测试)"
        else
            print_error "$failed_tests 个测试失败 (共 $total_tests 个测试)"
            return 1
        fi
    else
        print_error "单元测试执行失败"
        echo "查看 test.log 获取详细信息"
        return 1
    fi
}

# 构建APK
build_apk() {
    print_header "Step 3: 构建APK"

    print_info "构建Debug APK..."
    if ./gradlew assembleDebug 2>&1 | tee apk_build.log | grep -q "BUILD SUCCESSFUL"; then
        local apk_path="app/build/outputs/apk/debug/app-debug.apk"
        if [ -f "$apk_path" ]; then
            local apk_size=$(du -h "$apk_path" | cut -f1)
            print_success "APK构建成功 (大小: $apk_size)"

            # 检查APK大小
            local size_mb=$(du -m "$apk_path" | cut -f1)
            if [ "$size_mb" -gt 150 ]; then
                print_warning "APK大小超过150MB，建议优化"
            fi
        else
            print_error "APK文件未找到"
            return 1
        fi
    else
        print_error "APK构建失败"
        echo "查看 apk_build.log 获取详细信息"
        return 1
    fi
}

# 安装和测试APK
install_and_test_apk() {
    print_header "Step 4: 安装和测试APK"

    # 检查设备
    if ! adb devices | grep -q "device$"; then
        print_warning "没有检测到Android设备，跳过安装测试"
        return 0
    fi

    # 卸载旧版本
    print_info "卸载旧版本..."
    adb uninstall com.lanhe.gongjuxiang.debug &> /dev/null || true

    # 安装新版本
    print_info "安装APK..."
    if adb install app/build/outputs/apk/debug/app-debug.apk &> /dev/null; then
        print_success "APK安装成功"
    else
        print_error "APK安装失败"
        return 1
    fi

    # 启动应用
    print_info "启动应用..."
    if adb shell am start -n com.lanhe.gongjuxiang.debug/.activities.MainActivity &> /dev/null; then
        print_success "应用启动成功"

        # 等待应用启动
        sleep 3

        # 检查是否崩溃
        if adb logcat -d -s AndroidRuntime:E | grep -q "com.lanhe.gongjuxiang"; then
            print_error "检测到应用崩溃"
            return 1
        else
            print_success "应用运行正常（无崩溃）"
        fi
    else
        print_error "应用启动失败"
        return 1
    fi
}

# 性能快速检查
performance_check() {
    print_header "Step 5: 性能快速检查"

    if ! adb devices | grep -q "device$"; then
        print_warning "没有检测到Android设备，跳过性能检查"
        return 0
    fi

    # 检查启动时间
    print_info "测试冷启动时间..."
    adb shell am force-stop com.lanhe.gongjuxiang.debug &> /dev/null
    sleep 1

    local start_output=$(adb shell am start -W -n com.lanhe.gongjuxiang.debug/.activities.MainActivity 2>/dev/null)
    local total_time=$(echo "$start_output" | grep TotalTime | grep -oE "[0-9]+")

    if [ -n "$total_time" ]; then
        if [ "$total_time" -lt 500 ]; then
            print_success "冷启动时间: ${total_time}ms (优秀)"
        elif [ "$total_time" -lt 2000 ]; then
            print_success "冷启动时间: ${total_time}ms (良好)"
        else
            print_warning "冷启动时间: ${total_time}ms (需要优化)"
        fi
    fi

    # 检查内存使用
    print_info "检查内存使用..."
    sleep 2  # 等待应用稳定
    local mem_info=$(adb shell dumpsys meminfo com.lanhe.gongjuxiang.debug | grep "TOTAL" | head -1)
    local total_pss=$(echo "$mem_info" | awk '{print $2}')

    if [ -n "$total_pss" ]; then
        local mem_mb=$((total_pss / 1024))
        if [ "$mem_mb" -lt 100 ]; then
            print_success "内存使用: ${mem_mb}MB (优秀)"
        elif [ "$mem_mb" -lt 150 ]; then
            print_success "内存使用: ${mem_mb}MB (良好)"
        else
            print_warning "内存使用: ${mem_mb}MB (需要优化)"
        fi
    fi
}

# Lint检查
lint_check() {
    print_header "Step 6: 代码质量检查"

    print_info "运行Lint检查..."
    if ./gradlew lint 2>&1 | tee lint.log | grep -q "BUILD SUCCESSFUL"; then
        # 检查错误数量
        if [ -f "app/build/reports/lint-results.html" ]; then
            local errors=$(grep -o "error" app/build/reports/lint-results.html | wc -l)
            local warnings=$(grep -o "warning" app/build/reports/lint-results.html | wc -l)

            if [ "$errors" -eq 0 ]; then
                print_success "Lint检查通过 (0个错误, ${warnings}个警告)"
            else
                print_warning "Lint检查发现 ${errors}个错误, ${warnings}个警告"
            fi
        else
            print_success "Lint检查完成"
        fi
    else
        print_error "Lint检查失败"
        return 1
    fi
}

# 生成报告
generate_report() {
    print_header "生成验证报告"

    local report_file="verification_report_$(date +%Y%m%d_%H%M%S).md"

    cat > "$report_file" << EOF
# 蓝河助手验证报告
生成时间: $(date '+%Y-%m-%d %H:%M:%S')

## 验证结果摘要

| 检查项 | 状态 | 备注 |
|--------|------|------|
| 项目编译 | $([[ $COMPILE_RESULT -eq 0 ]] && echo "✅ 通过" || echo "❌ 失败") | - |
| 单元测试 | $([[ $TEST_RESULT -eq 0 ]] && echo "✅ 通过" || echo "❌ 失败") | - |
| APK构建 | $([[ $APK_RESULT -eq 0 ]] && echo "✅ 通过" || echo "❌ 失败") | - |
| 安装测试 | $([[ $INSTALL_RESULT -eq 0 ]] && echo "✅ 通过" || echo "❌ 失败") | - |
| 性能检查 | $([[ $PERF_RESULT -eq 0 ]] && echo "✅ 通过" || echo "❌ 失败") | - |
| 代码质量 | $([[ $LINT_RESULT -eq 0 ]] && echo "✅ 通过" || echo "❌ 失败") | - |

## 详细日志

- 编译日志: build.log
- 测试日志: test.log
- APK构建日志: apk_build.log
- Lint日志: lint.log

## 建议

EOF

    # 添加建议
    if [[ $COMPILE_RESULT -ne 0 ]]; then
        echo "- 修复编译错误，查看build.log" >> "$report_file"
    fi
    if [[ $TEST_RESULT -ne 0 ]]; then
        echo "- 修复失败的单元测试" >> "$report_file"
    fi
    if [[ $APK_RESULT -ne 0 ]]; then
        echo "- 检查APK构建配置" >> "$report_file"
    fi

    print_success "报告已生成: $report_file"
}

# 主函数
main() {
    print_header "蓝河助手快速验证脚本"
    echo "Version: 1.0.0"
    echo "Date: $(date '+%Y-%m-%d %H:%M:%S')"

    # 初始化结果变量
    COMPILE_RESULT=0
    TEST_RESULT=0
    APK_RESULT=0
    INSTALL_RESULT=0
    PERF_RESULT=0
    LINT_RESULT=0

    # 检查前置条件
    check_prerequisites

    # 执行验证步骤
    compile_project || COMPILE_RESULT=$?

    if [ $COMPILE_RESULT -eq 0 ]; then
        run_unit_tests || TEST_RESULT=$?
        build_apk || APK_RESULT=$?

        if [ $APK_RESULT -eq 0 ]; then
            install_and_test_apk || INSTALL_RESULT=$?
            performance_check || PERF_RESULT=$?
        fi

        lint_check || LINT_RESULT=$?
    fi

    # 生成报告
    generate_report

    # 总结
    print_header "验证完成"

    local total_failures=$((COMPILE_RESULT + TEST_RESULT + APK_RESULT + INSTALL_RESULT + PERF_RESULT + LINT_RESULT))

    if [ $total_failures -eq 0 ]; then
        print_success "🎉 所有验证项通过！"
        exit 0
    else
        print_error "有 $total_failures 个验证项失败"
        print_info "请查看生成的报告和日志文件"
        exit 1
    fi
}

# 清理函数
cleanup() {
    if [ -n "$1" ]; then
        print_warning "脚本被中断"
    fi
    # 可以在这里添加清理逻辑
}

# 设置中断处理
trap 'cleanup interrupted' INT TERM

# 执行主函数
main "$@"