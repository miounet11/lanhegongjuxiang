# 极简主义UI改造计划

## 当前状态
- 多个布局文件（如 `fragment_home.xml`, `fragment_dashboard.xml`, `fragment_functions.xml` 等）使用复杂的卡片、渐变背景和多色彩装饰。
- 颜色资源 `colors.xml` 提供大量强调色，导航栏和按钮采用多种高饱和色彩，整体视觉信息密度高。
- 导航结构包含底部导航与复杂的面板块状布局，局部存在多级卡片和图标组合，初次使用可能显得繁杂。
- 图标资源数量多且风格不完全统一，部分图标与背景对比不足。

## 目标状态
- 统一采用单色系（建议以中性灰+品牌青色为强调色），背景与卡片使用低对比度色彩，突出核心数据与操作。
- 布局层级简化：减少嵌套卡片、阴影与装饰性元素，仅保留核心功能入口和必要状态提示。
- 导航栏简化：使用更少的主入口，图标线条化并保持统一尺寸与描边；文字标签清晰。
- 图标风格统一，轮廓清晰；保留必要的状态图形化展示，避免过度装饰。

## 涉及文件
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/styles.xml` 与相关主题/组件样式
- 主要布局文件：
  - `app/src/main/res/layout/fragment_home.xml`
  - `app/src/main/res/layout/fragment_dashboard.xml`
  - `app/src/main/res/layout/fragment_functions.xml`
  - `app/src/main/res/layout/fragment_security.xml`
  - 各 Activity 布局（如 `activity_core_optimization.xml`, `activity_network_diagnostic.xml` 等）
- 图标资源（`app/src/main/res/drawable/`、`mipmap/`）按需更新

## 任务清单
- [ ] 定义极简主题调色板：背景、文本、强调色，更新 `colors.xml` 并调整主题样式。
- [ ] 梳理通用组件样式（按钮、卡片、文本风格），在 `styles.xml` 中统一定义。
- [ ] 重构主要 Fragment 布局，移除冗余装饰，突出核心数据和操作按钮。
- [ ] 优化底部导航与顶栏，采用单色线条图标和清晰标签。
- [ ] 清理或替换图标资源，确保线性图标风格一致。
- [ ] 复查每个 Activity 布局，保持与极简设计一致。
- [ ] 进行 UI 自测，确保颜色对比度与可访问性满足要求。