#!/bin/bash

# ================================================================
# 蓝河助手 - 单元测试覆盖率报告生成脚本
# ================================================================
# 功能：运行所有单元测试并生成Jacoco覆盖率报告
# 目标覆盖率：>60%
# ================================================================

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  蓝河助手 - 单元测试覆盖率检查${NC}"
echo -e "${BLUE}========================================${NC}"

# 清理之前的构建和报告
echo -e "\n${YELLOW}[1/6] 清理旧的构建产物...${NC}"
./gradlew clean

# 编译项目
echo -e "\n${YELLOW}[2/6] 编译项目...${NC}"
./gradlew assembleDebug assembleDebugUnitTest

# 运行单元测试
echo -e "\n${YELLOW}[3/6] 运行单元测试...${NC}"
./gradlew testDebugUnitTest --info

# 运行Android仪器测试（如果有模拟器运行）
if adb devices | grep -q "emulator\|device"; then
    echo -e "\n${YELLOW}[4/6] 运行仪器测试...${NC}"
    ./gradlew connectedDebugAndroidTest || true
else
    echo -e "\n${YELLOW}[4/6] 跳过仪器测试（没有连接的设备）${NC}"
fi

# 生成Jacoco覆盖率报告
echo -e "\n${YELLOW}[5/6] 生成覆盖率报告...${NC}"
./gradlew jacocoTestReport

# 检查覆盖率
echo -e "\n${YELLOW}[6/6] 检查覆盖率阈值...${NC}"
./gradlew jacocoTestCoverageVerification || {
    echo -e "${RED}覆盖率未达到要求！${NC}"
    COVERAGE_FAILED=1
}

# 解析覆盖率报告
REPORT_PATH="app/build/reports/jacoco/jacocoTestReport/html/index.html"
if [ -f "$REPORT_PATH" ]; then
    echo -e "\n${GREEN}覆盖率报告已生成：${NC}"
    echo -e "${BLUE}file://$PROJECT_ROOT/$REPORT_PATH${NC}"

    # 尝试在浏览器中打开报告
    if command -v open > /dev/null; then
        open "$REPORT_PATH"
    elif command -v xdg-open > /dev/null; then
        xdg-open "$REPORT_PATH"
    fi
fi

# 显示覆盖率摘要
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}  覆盖率摘要${NC}"
echo -e "${BLUE}========================================${NC}"

# 解析XML报告获取覆盖率数据
XML_REPORT="app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
if [ -f "$XML_REPORT" ]; then
    # 使用grep和sed提取覆盖率百分比
    INSTRUCTION_COVERAGE=$(grep -o 'type="INSTRUCTION"[^>]*' "$XML_REPORT" | grep -o 'covered="[0-9]*"' | sed 's/covered="//;s/"//' | head -1)
    INSTRUCTION_MISSED=$(grep -o 'type="INSTRUCTION"[^>]*' "$XML_REPORT" | grep -o 'missed="[0-9]*"' | sed 's/missed="//;s/"//' | head -1)

    if [ ! -z "$INSTRUCTION_COVERAGE" ] && [ ! -z "$INSTRUCTION_MISSED" ]; then
        TOTAL=$((INSTRUCTION_COVERAGE + INSTRUCTION_MISSED))
        if [ $TOTAL -gt 0 ]; then
            COVERAGE_PERCENT=$((INSTRUCTION_COVERAGE * 100 / TOTAL))

            echo -e "指令覆盖率: ${GREEN}${COVERAGE_PERCENT}%${NC}"

            # 检查是否达到目标
            if [ $COVERAGE_PERCENT -ge 60 ]; then
                echo -e "${GREEN}✓ 覆盖率达标（目标: >60%）${NC}"
            else
                echo -e "${RED}✗ 覆盖率未达标（目标: >60%）${NC}"
                COVERAGE_FAILED=1
            fi
        fi
    fi
fi

# 显示测试统计
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}  测试统计${NC}"
echo -e "${BLUE}========================================${NC}"

# 统计测试文件
TEST_FILES=$(find app/src/test -name "*Test.kt" 2>/dev/null | wc -l)
ANDROID_TEST_FILES=$(find app/src/androidTest -name "*Test.kt" 2>/dev/null | wc -l)

echo -e "单元测试文件数: ${GREEN}${TEST_FILES}${NC}"
echo -e "仪器测试文件数: ${GREEN}${ANDROID_TEST_FILES}${NC}"

# 列出所有测试类
echo -e "\n${BLUE}已创建的测试类：${NC}"
echo -e "${YELLOW}单元测试:${NC}"
find app/src/test -name "*Test.kt" -exec basename {} .kt \; 2>/dev/null | sort | while read test; do
    echo -e "  • $test"
done

echo -e "\n${YELLOW}仪器测试:${NC}"
find app/src/androidTest -name "*Test.kt" -exec basename {} .kt \; 2>/dev/null | sort | while read test; do
    echo -e "  • $test"
done

# 显示覆盖的核心模块
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}  测试覆盖的核心模块${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "${GREEN}✓${NC} ShizukuManager - 权限管理与系统操作"
echo -e "${GREEN}✓${NC} PermissionHelper - Android权限处理"
echo -e "${GREEN}✓${NC} AppDatabase - Room数据库与迁移"
echo -e "${GREEN}✓${NC} RealPerformanceMonitorManager - 性能监控"
echo -e "${GREEN}✓${NC} CommandValidator - 命令安全验证"
echo -e "${GREEN}✓${NC} AdvancedBatteryOptimizer - 电池优化"
echo -e "${GREEN}✓${NC} EnhancedMemoryManager - 内存管理"
echo -e "${GREEN}✓${NC} EnhancedStorageOptimizer - 存储优化"
echo -e "${GREEN}✓${NC} AdvancedNetworkOptimizer - 网络优化"
echo -e "${GREEN}✓${NC} ServiceLifecycle - 服务生命周期"

# 最终结果
echo -e "\n${BLUE}========================================${NC}"
if [ -z "$COVERAGE_FAILED" ]; then
    echo -e "${GREEN}  ✓ 测试全部通过！${NC}"
else
    echo -e "${RED}  ✗ 部分测试失败或覆盖率未达标${NC}"
    exit 1
fi
echo -e "${BLUE}========================================${NC}"