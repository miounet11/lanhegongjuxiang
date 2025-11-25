# Shizuku APK 下载指南

## 自动下载失败，请手动下载

由于网络限制，自动下载Shizuku APK失败。请按照以下步骤手动下载：

## 方法1：直接下载（推荐）

1. 打开浏览器，访问以下地址：
   ```
   https://github.com/RikkaApps/Shizuku/releases/latest
   ```

2. 在页面中找到"Assets"部分，下载文件：
   ```
   shizuku-v13.5.4.r1038.05cd6fc-release.apk
   ```
   （或者最新版本的release.apk文件）

3. 将下载的文件重命名为：`shizuku.apk`

4. 将文件移动到项目的assets目录：
   ```
   /Users/lu/Downloads/lanhezhushou/app/src/main/assets/shizuku.apk
   ```

## 方法2：使用命令行下载

在终端中执行：

```bash
cd /Users/lu/Downloads/lanhezhushou

# 删除失败的文件
rm app/src/main/assets/shizuku.apk

# 下载最新版本
curl -L -o app/src/main/assets/shizuku.apk https://github.com/RikkaApps/Shizuku/releases/download/v13.5.4.r1038.05cd6fc/shizuku-v13.5.4.r1038.05cd6fc-release.apk
```

## 方法3：从其他下载源

如果GitHub访问困难，可以尝试：

1. **Gitee镜像**（如果有）
2. **酷安应用市场**搜索"Shizuku"
3. **F-Droid**应用商店

## 验证下载

下载完成后，验证文件大小：

```bash
ls -lh app/src/main/assets/shizuku.apk
```

正常情况下，文件大小应该是 **8-10 MB** 左右。

如果文件只有几个字节（如9B），说明下载失败，需要重新下载。

## 下载完成后

将APK文件放到正确位置后，继续按照集成步骤操作：

1. ✅ assets目录已创建
2. ⏳ 下载shizuku.apk（需要手动完成）
3. ⏳ 配置AndroidManifest.xml
4. ⏳ 创建file_paths.xml
5. ⏳ 修改ShizukuAuthActivity

后续步骤将自动继续配置其他文件。
