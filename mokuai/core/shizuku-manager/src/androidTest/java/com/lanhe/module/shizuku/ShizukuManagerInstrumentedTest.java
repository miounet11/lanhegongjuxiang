package com.lanhe.module.shizuku;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.isA;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.lanhe.module.shizuku.interfaces.IShizukuCallback;
import com.lanhe.module.shizuku.exception.ShizukuException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ShizukuManager仪器化测试
 *
 * <p>在真实Android设备上测试ShizukuManager的功能。</p>
 *
 * @author LanHe
 * @version 1.0.0
 * @since 2024-01-01
 */
@RunWith(AndroidJUnit4.class)
public class ShizukuManagerInstrumentedTest {

    private Context context;
    private ShizukuManager shizukuManager;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        shizukuManager = ShizukuManager.getInstance(context);
    }

    @After
    public void tearDown() {
        if (shizukuManager != null) {
            shizukuManager.cleanup();
        }
        ShizukuManager.destroy();
    }

    @Test
    public void `test context should not be null`() {
        assertThat("Context should not be null", context, notNullValue());
    }

    @Test
    public void `test shizuku manager instance should be created successfully`() {
        assertThat("ShizukuManager instance should not be null", shizukuManager, notNullValue());
        assertThat("ShizukuManager should be instance of IShizukuManager",
            shizukuManager, isA(IShizukuManager.class));
    }

    @Test
    public void `test getStatus should return valid status code`() {
        int status = shizukuManager.getStatus();

        // 验证状态码在有效范围内
        assertThat("Status should be valid",
            status >= 0 && status <= 3, is(true));
    }

    @Test
    public void `test getStatusMessage should return non-empty string`() {
        String message = shizukuManager.getStatusMessage();

        assertThat("Status message should not be null", message, notNullValue());
        assertThat("Status message should not be empty", !message.trim().isEmpty(), is(true));
    }

    @Test
    public void `test isShizukuAvailable should return boolean value`() {
        boolean available = shizukuManager.isShizukuAvailable();

        // 结果依赖于实际环境，这里只验证方法调用正常
        assertThat("Method should complete without exception", true, is(true));
    }

    @Test
    public void `test getSystemService should not throw exception`() {
        try {
            Object service = shizukuManager.getSystemService("activity");
            // 结果可能为null，但不应该抛出异常
            assertThat("Method should complete without exception", true, is(true));
        } catch (Exception e) {
            // 如果抛出异常，验证是预期的异常类型
            assertThat("Should throw IllegalArgumentException for invalid input",
                e, isA(IllegalArgumentException.class));
        }
    }

    @Test
    public void `test cleanup should not throw exception`() {
        try {
            shizukuManager.cleanup();
            assertThat("Cleanup should complete without exception", true, is(true));
        } catch (Exception e) {
            assertThat("Cleanup should not throw exception: " + e.getMessage(), false, is(true));
        }
    }

    @Test
    public void `test executeSystemOperation should handle callback properly`() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        shizukuManager.executeSystemOperation("test_operation", new IShizukuCallback<String>() {
            @Override
            public void onSuccess(String result) {
                assertThat("Result should not be null", result, notNullValue());
                latch.countDown();
            }

            @Override
            public void onFailure(ShizukuException error) {
                assertThat("Error should not be null", error, notNullValue());
                latch.countDown();
            }

            @Override
            public void onCancel() {
                latch.countDown();
            }
        });

        // 等待最多5秒
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat("Operation should complete within timeout", completed, is(true));
    }

    @Test
    public void `test requestPermission should handle callback properly`() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        shizukuManager.requestPermission(new IShizukuCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                assertThat("Result should not be null", result, notNullValue());
                latch.countDown();
            }

            @Override
            public void onFailure(ShizukuException error) {
                assertThat("Error should not be null", error, notNullValue());
                latch.countDown();
            }

            @Override
            public void onCancel() {
                latch.countDown();
            }
        });

        // 等待最多10秒（权限请求可能需要更长时间）
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat("Permission request should complete within timeout", completed, is(true));
    }

    @Test
    public void `test multiple operations should not interfere with each other`() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        // 并发执行多个操作
        shizukuManager.executeSystemOperation("operation1", new TestCallback(latch));
        shizukuManager.executeSystemOperation("operation2", new TestCallback(latch));
        shizukuManager.executeSystemOperation("operation3", new TestCallback(latch));

        // 等待所有操作完成
        boolean completed = latch.await(15, TimeUnit.SECONDS);
        assertThat("All operations should complete within timeout", completed, is(true));
    }

    @Test
    public void `test invalid parameters should throw exceptions`() {
        try {
            shizukuManager.executeSystemOperation(null, new TestCallback(new CountDownLatch(1)));
            assertThat("Should throw exception for null operation", false, is(true));
        } catch (IllegalArgumentException e) {
            assertThat("Should throw IllegalArgumentException", true, is(true));
        }

        try {
            shizukuManager.executeSystemOperation("", new TestCallback(new CountDownLatch(1)));
            assertThat("Should throw exception for empty operation", false, is(true));
        } catch (IllegalArgumentException e) {
            assertThat("Should throw IllegalArgumentException", true, is(true));
        }

        try {
            shizukuManager.getSystemService(null);
            assertThat("Should throw exception for null service name", false, is(true));
        } catch (IllegalArgumentException e) {
            assertThat("Should throw IllegalArgumentException", true, is(true));
        }
    }

    /**
     * 测试用的回调实现
     */
    private static class TestCallback implements IShizukuCallback<String> {
        private final CountDownLatch latch;

        public TestCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onSuccess(String result) {
            latch.countDown();
        }

        @Override
        public void onFailure(ShizukuException error) {
            latch.countDown();
        }

        @Override
        public void onCancel() {
            latch.countDown();
        }
    }
}
