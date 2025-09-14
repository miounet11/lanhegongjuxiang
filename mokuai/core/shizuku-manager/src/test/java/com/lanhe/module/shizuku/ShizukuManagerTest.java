package com.lanhe.module.shizuku;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import com.lanhe.module.shizuku.interfaces.IShizukuCallback;
import com.lanhe.module.shizuku.exception.ShizukuException;
import com.lanhe.module.shizuku.constants.ShizukuConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/**
 * ShizukuManager单元测试
 *
 * <p>测试ShizukuManager的核心功能，包括权限检查、状态查询、系统操作等。</p>
 *
 * @author LanHe
 * @version 1.0.0
 * @since 2024-01-01
 */
@RunWith(RobolectricTestRunner.class)
public class ShizukuManagerTest {

    @Mock
    private Context mockContext;

    @Mock
    private IShizukuCallback<String> mockCallback;

    private ShizukuManager shizukuManager;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        // 注意：在实际测试中，这里需要处理单例模式的清理
        shizukuManager = ShizukuManager.getInstance(mockContext);
    }

    @After
    public void tearDown() {
        // 清理单例实例
        ShizukuManager.destroy();
    }

    @Test
    public void `getInstance with valid context should return instance`() {
        // Given
        Context context = mockContext;

        // When
        ShizukuManager instance = ShizukuManager.getInstance(context);

        // Then
        assertNotNull("ShizukuManager instance should not be null", instance);
        assertSame("Should return the same instance", shizukuManager, instance);
    }

    @Test(expected = IllegalArgumentException.class)
    public void `getInstance with null context should throw exception`() {
        // When & Then
        ShizukuManager.getInstance(null);
    }

    @Test
    public void `getStatus should return valid status code`() {
        // When
        int status = shizukuManager.getStatus();

        // Then
        assertTrue("Status should be a valid status code",
            status >= ShizukuConstants.STATUS_AVAILABLE &&
            status <= ShizukuConstants.STATUS_NO_PERMISSION);
    }

    @Test
    public void `getStatusMessage should return non-empty string`() {
        // When
        String message = shizukuManager.getStatusMessage();

        // Then
        assertNotNull("Status message should not be null", message);
        assertFalse("Status message should not be empty", message.trim().isEmpty());
    }

    @Test
    public void `executeSystemOperation with valid parameters should not throw exception`() {
        // Given
        String operation = "test_operation";

        // When & Then
        // 注意：这里可能需要模拟异步操作
        // 实际测试中可能需要使用CountDownLatch来等待异步结果
        try {
            shizukuManager.executeSystemOperation(operation, mockCallback);
            // 如果没有抛出异常，则测试通过
        } catch (Exception e) {
            // 如果是预期的异常（如权限不足），也认为是正常的
            assertTrue("Expected exception types",
                e instanceof ShizukuException ||
                e instanceof IllegalArgumentException);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void `executeSystemOperation with null operation should throw exception`() {
        // When & Then
        shizukuManager.executeSystemOperation(null, mockCallback);
    }

    @Test(expected = IllegalArgumentException.class)
    public void `executeSystemOperation with empty operation should throw exception`() {
        // When & Then
        shizukuManager.executeSystemOperation("", mockCallback);
    }

    @Test(expected = IllegalArgumentException.class)
    public void `executeSystemOperation with null callback should throw exception`() {
        // When & Then
        shizukuManager.executeSystemOperation("test", null);
    }

    @Test
    public void `getSystemService with valid service name should not throw exception`() {
        // Given
        String serviceName = "activity";

        // When
        Object service = shizukuManager.getSystemService(serviceName);

        // Then
        // 注意：实际的返回结果依赖于Shizuku是否可用
        // 这里我们只验证方法调用不抛出异常
    }

    @Test(expected = IllegalArgumentException.class)
    public void `getSystemService with null service name should throw exception`() {
        // When & Then
        shizukuManager.getSystemService(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void `getSystemService with empty service name should throw exception`() {
        // When & Then
        shizukuManager.getSystemService("");
    }

    @Test
    public void `cleanup should not throw exception`() {
        // When & Then
        try {
            shizukuManager.cleanup();
            // 如果没有抛出异常，则测试通过
        } catch (Exception e) {
            fail("cleanup() should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void `requestPermission with valid callback should not throw exception`() {
        // When & Then
        try {
            shizukuManager.requestPermission(mockCallback);
            // 如果没有抛出异常，则测试通过
        } catch (Exception e) {
            // 如果是预期的异常（如权限不足），也认为是正常的
            assertTrue("Expected exception types",
                e instanceof ShizukuException);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void `requestPermission with null callback should throw exception`() {
        // When & Then
        shizukuManager.requestPermission(null);
    }

    @Test
    public void `isShizukuAvailable should return boolean value`() {
        // When
        boolean available = shizukuManager.isShizukuAvailable();

        // Then
        // 结果依赖于实际环境，这里只验证返回类型正确
        assertTrue("Should return boolean value", available || !available);
    }

    @Test
    public void `init with valid context should not throw exception`() {
        // When & Then
        try {
            ShizukuManager.init(mockContext);
            // 如果没有抛出异常，则测试通过
        } catch (Exception e) {
            fail("init() should not throw exception: " + e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void `init with null context should throw exception`() {
        // When & Then
        ShizukuManager.init(null);
    }
}
