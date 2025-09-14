# 📦 UI模块 (User Interface Module)

## 🎯 概述

蓝河工具箱UI模块提供完整的用户界面组件和Activity管理功能，包括Gallery浏览、下载管理、设置界面、浏览器组件等。该模块基于Android原生UI框架和Chromium界面规范，提供了丰富的用户交互体验。

## ✨ 主要特性

- ✅ **完整的Activity架构**：MainActivity、GalleryActivity、SettingsActivity等
- ✅ **Fragment管理**：高效的Fragment切换和生命周期管理
- ✅ **自定义控件**：丰富的自定义UI组件和交互效果
- ✅ **主题系统**：支持多种主题和样式切换
- ✅ **响应式布局**：适配不同屏幕尺寸和方向
- ✅ **动画效果**：流畅的页面切换和交互动画
- ✅ **无障碍支持**：完整的Accessibility支持
- ✅ **国际化**：多语言支持和本地化

## 🚀 快速开始

### 基本Activity使用

```java
// 继承BaseActivity
public class MyActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // 初始化UI组件
        initViews();

        // 加载数据
        loadData();
    }

    private void initViews() {
        // 初始化视图
        Button button = findViewById(R.id.my_button);
        button.setOnClickListener(v -> {
            // 处理点击事件
            showMessage("Button clicked!");
        });
    }

    private void loadData() {
        // 加载数据逻辑
        DataLoader loader = new DataLoader();
        loader.loadData(new DataCallback() {
            @Override
            public void onSuccess(Object data) {
                // 更新UI
                updateUI(data);
            }

            @Override
            public void onError(Exception error) {
                // 处理错误
                showError(error);
            }
        });
    }
}
```

### Fragment使用

```java
// 创建Fragment
public class MyFragment extends BaseFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my, container, false);

        // 初始化Fragment视图
        initViews(view);

        return view;
    }

    private void initViews(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 设置适配器
        MyAdapter adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);
    }
}
```

### 自定义控件使用

```java
// 使用自定义ImageView
public class GalleryItemView extends RelativeLayout {

    private ImageView thumbnailView;
    private TextView titleView;
    private TextView uploaderView;

    public GalleryItemView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.view_gallery_item, this, true);

        // 初始化子视图
        thumbnailView = findViewById(R.id.thumbnail);
        titleView = findViewById(R.id.title);
        uploaderView = findViewById(R.id.uploader);

        // 设置样式和交互
        setupView();
    }

    private void setupView() {
        // 设置圆角、阴影等效果
        thumbnailView.setClipToOutline(true);
        thumbnailView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 8);
            }
        });
    }

    public void bind(GalleryInfo info) {
        // 绑定数据
        titleView.setText(info.getTitle());
        uploaderView.setText(info.getUploader());

        // 加载缩略图
        ImageLoader.load(info.getThumb())
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error)
            .into(thumbnailView);
    }
}
```

## 📋 API 参考

### 核心Activity类

| Activity类 | 说明 |
|-----------|------|
| `MainActivity` | 主Activity，应用入口 |
| `GalleryActivity` | 画廊浏览Activity |
| `SettingsActivity` | 设置界面Activity |
| `DownloadManagerActivity` | 下载管理Activity |
| `HistoryActivity` | 历史记录Activity |
| `WebViewActivity` | 浏览器Activity |

### 核心Fragment类

| Fragment类 | 说明 |
|-----------|------|
| `GalleryListFragment` | 画廊列表Fragment |
| `DownloadFragment` | 下载管理Fragment |
| `SettingsFragment` | 设置Fragment |
| `AboutFragment` | 关于Fragment |

### 自定义控件

| 控件类 | 说明 |
|-------|------|
| `GalleryItemView` | 画廊项视图 |
| `DownloadProgressView` | 下载进度视图 |
| `SearchBarView` | 搜索栏视图 |
| `TabSwitcherView` | 标签切换器视图 |

### 主要方法

#### BaseActivity

```java
// 显示消息
void showMessage(String message)

// 显示错误
void showError(String error)

// 显示加载对话框
void showLoadingDialog(String message)

// 隐藏加载对话框
void hideLoadingDialog()

// 设置工具栏标题
void setToolbarTitle(String title)

// 启用返回按钮
void enableBackButton(boolean enable)
```

#### BaseFragment

```java
// 获取宿主Activity
BaseActivity getBaseActivity()

// 显示消息
void showMessage(String message)

// 显示加载指示器
void showLoading(boolean show)

// 设置标题
void setTitle(String title)
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `theme` | `String` | `default` | 主题样式 |
| `layoutDirection` | `String` | `ltr` | 布局方向 |
| `fontSize` | `int` | `14` | 字体大小(sp) |
| `enableAnimations` | `boolean` | `true` | 启用动画效果 |
| `enableDarkMode` | `boolean` | `false` | 启用暗色模式 |
| `gridColumns` | `int` | `2` | 网格列数 |

## 📦 依赖项

```gradle
dependencies {
    // Android核心库
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.recyclerview:recyclerview:1.3.1'
    implementation 'androidx.viewpager2:viewpager2:1.0.0'
    implementation 'com.google.android.material:material:1.9.0'

    // 图片加载
    implementation 'com.github.bumptech.glide:glide:4.16.0'

    // 蓝河工具箱 UI模块
    implementation 'com.hippo.ehviewer:ui:1.0.0'
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### 兼容性
- **最低版本**: Android API 21 (Android 5.0)
- **目标版本**: Android API 34 (Android 14)
- **编译版本**: Android API 34

### 已知问题
- 在低内存设备上可能需要调整图片缓存大小
- 某些动画效果在老设备上可能不够流畅

## 🧪 测试

### Activity测试
```java
@RunWith(AndroidJUnit4::class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testMainActivity_Launch_Success() {
        // Given
        MainActivity activity = activityRule.getActivity();

        // When
        // Activity自动启动

        // Then
        assertNotNull(activity);
        assertTrue(activity.isFinishing() == false);
    }
}
```

### Fragment测试
```java
@RunWith(AndroidJUnit4::class)
public class GalleryFragmentTest {

    @Test
    public void testGalleryFragment_DisplayGallery_Success() {
        // Given
        GalleryFragment fragment = new GalleryFragment();
        FragmentManager manager = getFragmentManager();

        // When
        manager.beginTransaction()
            .add(fragment, "gallery")
            .commit();

        // Then
        assertTrue(fragment.isAdded());
        assertNotNull(fragment.getView());
    }
}
```

### UI测试
```java
@RunWith(AndroidJUnit4::class)
public class UITest {

    @Test
    public void testGalleryItemView_DisplayData_Success() {
        // Given
        GalleryItemView view = new GalleryItemView(context);
        GalleryInfo info = createTestGalleryInfo();

        // When
        view.bind(info);

        // Then
        assertEquals(info.getTitle(), view.getTitle());
        assertEquals(info.getUploader(), view.getUploader());
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingUI`)
3. 提交更改 (`git commit -m 'Add some AmazingUI'`)
4. 推送到分支 (`git push origin feature/AmazingUI`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整API文档](https://docs.ehviewer.com/ui/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
