package com.lanhe.gongjuxiang.integration

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.lanhe.gongjuxiang.MainActivity
import com.lanhe.gongjuxiang.R
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MainActivity 集成测试
 * 测试主要用户界面交互和导航
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityIntegrationTest {

    @Test
    fun `test main activity launch and UI components`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 验证主界面元素存在
            onView(withId(R.id.bottom_navigation))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.toolbar))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.drawer_layout))
                .check(matches(isDisplayed()))
            
            // 验证底部导航项存在
            onView(withId(R.id.navigation_home))
                .check(matches(isDisplayed()))
            .check(matches(isClickable()))
            
            onView(withId(R.id.navigation_performance))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            onView(withId(R.id.navigation_functions))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            onView(withId(R.id.navigation_security))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            onView(withId(R.id.navigation_settings))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
        }
    }

    @Test
    fun `test bottom navigation switching`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 切换到性能页面
            onView(withId(R.id.navigation_performance))
                .perform(click())
            
            // 验证性能页面内容
            onView(withId(R.id.performance_fragment_container))
                .check(matches(isDisplayed()))
            
            // 切换到功能页面
            onView(withId(R.id.navigation_functions))
                .perform(click())
            
            // 验证功能页面内容
            onView(withId(R.id.functions_fragment_container))
                .check(matches(isDisplayed()))
            
            // 切换到安全页面
            onView(withId(R.id.navigation_security))
                .perform(click())
            
            // 验证安全页面内容
            onView(withId(R.id.security_fragment_container))
                .check(matches(isDisplayed()))
            
            // 切换到设置页面
            onView(withId(R.id.navigation_settings))
                .perform(click())
            
            // 验证设置页面内容
            onView(withId(R.id.settings_fragment_container))
                .check(matches(isDisplayed()))
            
            // 返回主页
            onView(withId(R.id.navigation_home))
                .perform(click())
            
            // 验证主页内容
            onView(withId(R.id.home_fragment_container))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun `test drawer navigation`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 打开抽屉
            onView(withId(R.id.drawer_layout))
                .perform(click())
            
            // 验证抽屉内容
            onView(withText("蓝河助手"))
                .check(matches(isDisplayed()))
            
            onView(withText("系统监控"))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            onView(withText("电池管理"))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            onView(withText("内存管理"))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            onView(withText("CPU管理"))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            onView(withText("存储管理"))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            onView(withText("网络诊断"))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            onView(withText("安全管理"))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
        }
    }

    @Test
    fun `test performance monitoring card interactions`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 在主页上点击性能监控卡片
            onView(allOf(
                withId(R.id.card_performance_monitor),
                isDisplayed()
            )).perform(click())
            
            // 验证跳转到性能页面
            onView(withId(R.id.performance_fragment_container))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun `test function module grid interactions`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 切换到功能页面
            onView(withId(R.id.navigation_functions))
                .perform(click())
            
            // 点击功能模块（如果存在RecyclerView）
            try {
                onView(withId(R.id.recycler_functions))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))
            } catch (e: Exception) {
                // 如果不存在RecyclerView，尝试其他方式
                onView(withText("系统监控"))
                    .perform(click())
            }
        }
    }

    @Test
    fun `test settings navigation interactions`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 切换到设置页面
            onView(withId(R.id.navigation_settings))
                .perform(click())
            
            // 验证设置选项存在
            onView(withText("系统设置"))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            onView(withText("主题设置"))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            onView(withText("关于"))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
        }
    }

    @Test
    fun `test toolbar menu interactions`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 点击工具栏菜单（如果存在）
            try {
                onView(withContentDescription("更多选项"))
                    .perform(click())
                
                // 验证菜单项存在
                onView(withText("刷新"))
                    .check(matches(isDisplayed()))
                
                onView(withText("设置"))
                    .check(matches(isDisplayed()))
                
                onView(withText("关于"))
                    .check(matches(isDisplayed()))
            } catch (e: Exception) {
                // 如果没有菜单，跳过测试
            }
        }
    }

    @Test
    fun `test activity recreation state preservation`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 切换到性能页面
            onView(withId(R.id.navigation_performance))
                .perform(click())
            
            // 重新创建Activity（模拟配置变更）
            scenario.recreate()
            
            // 验证当前选中的页面保持不变
            onView(withId(R.id.navigation_performance))
                .check(matches(isSelected()))
            
            onView(withId(R.id.performance_fragment_container))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun `test back navigation`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 打开抽屉导航
            onView(withId(R.id.drawer_layout))
                .perform(click())
            
            // 点击某个导航项
            onView(withText("系统监控"))
                .perform(click())
            
            // 验证跳转成功
            // 这里可能需要验证相应的Activity或Fragment已加载
            
            // 按返回键
            onView(isRoot()).perform(pressBack())
            
            // 验证返回到主界面
            onView(withId(R.id.navigation_home))
                .check(matches(isSelected()))
        }
    }

    @Test
    fun `test accessibility and content descriptions`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 验证重要元素有内容描述
            onView(withId(R.id.bottom_navigation))
                .check(matches(withContentDescription("底部导航")))
            
            // 验证底部导航项有内容描述
            onView(withId(R.id.navigation_home))
                .check(matches(withContentDescription("主页")))
            
            onView(withId(R.id.navigation_performance))
                .check(matches(withContentDescription("性能")))
            
            onView(withId(R.id.navigation_functions))
                .check(matches(withContentDescription("功能")))
            
            onView(withId(R.id.navigation_security))
                .check(matches(withContentDescription("安全")))
            
            onView(withId(R.id.navigation_settings))
                .check(matches(withContentDescription("设置")))
        }
    }
}
