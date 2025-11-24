/**
 * 蓝河助手文件管理器核心模块
 * 基于Chromium的现代文件管理界面
 */

class LanheFileManager {
    constructor() {
        this.currentPath = '/';
        this.currentFiles = [];
        this.selectedFiles = new Set();
        this.viewMode = 'grid';
        this.sortBy = 'name';
        this.sortOrder = 'asc';
        this.searchQuery = '';
        this.history = [];
        this.historyIndex = -1;

        this.initializeElements();
        this.bindEvents();
        this.loadInitialPath();
    }

    initializeElements() {
        // 主要元素
        this.fileGrid = document.getElementById('file-grid');
        this.pathNavigator = document.getElementById('path-navigator');
        this.searchInput = document.getElementById('search-input');
        this.loading = document.getElementById('loading');
        this.storageInfo = document.getElementById('storage-info');

        // 工具栏按钮
        this.backBtn = document.getElementById('back-btn');
        this.forwardBtn = document.getElementById('forward-btn');
        this.upBtn = document.getElementById('up-btn');
        this.sortSelect = document.getElementById('sort-select');

        // 视图切换
        this.viewButtons = document.querySelectorAll('.view-switcher button');

        // 右键菜单
        this.contextMenu = document.getElementById('context-menu');

        // 详情面板
        this.detailPanel = document.getElementById('detail-panel');
        this.detailContent = document.getElementById('detail-content');
    }

    bindEvents() {
        // 导航事件
        this.backBtn.addEventListener('click', () => this.goBack());
        this.forwardBtn.addEventListener('click', () => this.goForward());
        this.upBtn.addEventListener('click', () => this.goUp());

        // 搜索事件
        this.searchInput.addEventListener('input', (e) => {
            this.searchQuery = e.target.value;
            this.filterFiles();
        });

        document.getElementById('clear-search').addEventListener('click', () => {
            this.searchInput.value = '';
            this.searchQuery = '';
            this.filterFiles();
        });

        // 排序事件
        this.sortSelect.addEventListener('change', (e) => {
            this.sortBy = e.target.value;
            this.sortFiles();
        });

        document.getElementById('sort-order').addEventListener('click', () => {
            this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
            this.sortFiles();
        });

        // 视图切换
        this.viewButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                this.viewButtons.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                this.viewMode = btn.dataset.view;
                this.renderFiles();
            });
        });

        // 文件容器事件
        this.fileGrid.addEventListener('click', (e) => this.handleFileClick(e));
        this.fileGrid.addEventListener('dblclick', (e) => this.handleFileDoubleClick(e));
        this.fileGrid.addEventListener('contextmenu', (e) => this.handleContextMenu(e));

        // 右键菜单事件
        document.addEventListener('click', (e) => {
            if (!this.contextMenu.contains(e.target)) {
                this.hideContextMenu();
            }
        });

        this.contextMenu.addEventListener('click', (e) => {
            const action = e.target.dataset.action;
            if (action) {
                this.handleContextAction(action);
            }
        });

        // 关闭详情面板
        document.getElementById('close-detail').addEventListener('click', () => {
            this.hideDetailPanel();
        });

        // 键盘事件
        document.addEventListener('keydown', (e) => this.handleKeyPress(e));
    }

    async loadInitialPath() {
        // 获取初始路径（外部存储根目录）
        const initialPath = await this.getRootPath();
        this.navigateToPath(initialPath);
    }

    async navigateToPath(path) {
        this.showLoading();
        try {
            // 添加到历史记录
            if (this.historyIndex < this.history.length - 1) {
                this.history = this.history.slice(0, this.historyIndex + 1);
            }
            this.history.push(path);
            this.historyIndex++;

            this.currentPath = path;

            // 加载文件列表
            const files = await window.lanheFileManager.getFileList(path);
            this.currentFiles = files;

            // 更新界面
            this.updatePathNavigator(path);
            await this.updateStorageInfo(path);
            this.sortFiles();
            this.renderFiles();

            this.updateNavigationButtons();
        } catch (error) {
            this.showToast('加载文件失败: ' + error.message, 'error');
        } finally {
            this.hideLoading();
        }
    }

    updatePathNavigator(path) {
        this.pathNavigator.innerHTML = '';

        const segments = path.split('/').filter(segment => segment.length > 0);

        // 添加根目录
        const rootItem = this.createPathItem('/', '📱 存储');
        this.pathNavigator.appendChild(rootItem);

        // 添加路径段
        let currentPath = '';
        segments.forEach((segment, index) => {
            currentPath += '/' + segment;
            const item = this.createPathItem(currentPath, segment);
            this.pathNavigator.appendChild(item);
        });
    }

    createPathItem(path, name) {
        const item = document.createElement('div');
        item.className = 'path-item';
        if (path === this.currentPath) {
            item.classList.add('active');
        }

        item.textContent = name;
        item.addEventListener('click', () => {
            if (path !== this.currentPath) {
                this.navigateToPath(path);
            }
        });

        return item;
    }

    async updateStorageInfo(path) {
        try {
            const storageInfo = await window.lanheFileManager.getStorageInfo(path);
            const usedPercent = (storageInfo.usedSpace / storageInfo.totalSpace) * 100;

            document.getElementById('storage-used').style.width = `${usedPercent}%`;
            document.getElementById('storage-used-text').textContent =
                this.formatFileSize(storageInfo.usedSpace);
            document.getElementById('storage-total-text').textContent =
                this.formatFileSize(storageInfo.totalSpace);

            this.storageInfo.style.display = 'block';
        } catch (error) {
            console.warn('Failed to get storage info:', error);
            this.storageInfo.style.display = 'none';
        }
    }

    sortFiles() {
        this.currentFiles.sort((a, b) => {
            let valueA, valueB;

            switch (this.sortBy) {
                case 'name':
                    valueA = a.name.toLowerCase();
                    valueB = b.name.toLowerCase();
                    break;
                case 'size':
                    valueA = a.size || 0;
                    valueB = b.size || 0;
                    break;
                case 'date':
                    valueA = a.lastModified || 0;
                    valueB = b.lastModified || 0;
                    break;
                case 'type':
                    valueA = this.getFileType(a);
                    valueB = this.getFileType(b);
                    break;
                default:
                    valueA = a.name.toLowerCase();
                    valueB = b.name.toLowerCase();
            }

            if (valueA < valueB) return this.sortOrder === 'asc' ? -1 : 1;
            if (valueA > valueB) return this.sortOrder === 'asc' ? 1 : -1;
            return 0;
        });
    }

    filterFiles() {
        this.renderFiles();
    }

    renderFiles() {
        this.fileGrid.innerHTML = '';

        // 过滤文件
        let filesToRender = this.currentFiles;
        if (this.searchQuery) {
            const query = this.searchQuery.toLowerCase();
            filesToRender = this.currentFiles.filter(file =>
                file.name.toLowerCase().includes(query)
            );
        }

        if (filesToRender.length === 0) {
            this.renderEmptyState();
            return;
        }

        // 根据视图模式渲染
        switch (this.viewMode) {
            case 'grid':
                this.renderGridView(filesToRender);
                break;
            case 'list':
                this.renderListView(filesToRender);
                break;
            case 'detail':
                this.renderDetailView(filesToRender);
                break;
        }
    }

    renderGridView(files) {
        this.fileGrid.className = 'file-grid grid-view';

        files.forEach(file => {
            const fileElement = this.createFileElement(file, 'grid');
            this.fileGrid.appendChild(fileElement);
        });
    }

    renderListView(files) {
        this.fileGrid.className = 'file-grid list-view';

        files.forEach(file => {
            const fileElement = this.createFileElement(file, 'list');
            this.fileGrid.appendChild(fileElement);
        });
    }

    renderDetailView(files) {
        this.fileGrid.className = 'file-grid detail-view';

        files.forEach(file => {
            const fileElement = this.createFileElement(file, 'detail');
            this.fileGrid.appendChild(fileElement);
        });
    }

    createFileElement(file, viewType) {
        const element = document.createElement('div');
        element.className = `file-item file-${viewType}`;
        element.dataset.path = file.path;

        if (this.selectedFiles.has(file.path)) {
            element.classList.add('selected');
        }

        // 图标
        const icon = document.createElement('div');
        icon.className = 'file-icon';
        icon.textContent = this.getFileIcon(file);
        element.appendChild(icon);

        // 内容区域
        const content = document.createElement('div');
        content.className = 'file-content';

        // 文件名
        const name = document.createElement('div');
        name.className = 'file-name';
        name.textContent = file.name;
        content.appendChild(name);

        // 详细视图的额外信息
        if (viewType === 'detail') {
            const details = document.createElement('div');
            details.className = 'file-details';
            details.innerHTML = `
                <span class="file-size">${this.formatFileSize(file.size)}</span>
                <span class="file-date">${this.formatDate(file.lastModified)}</span>
                <span class="file-type">${this.getFileType(file)}</span>
            `;
            content.appendChild(details);
        }

        element.appendChild(content);

        return element;
    }

    handleFileClick(event) {
        const fileElement = event.target.closest('.file-item');
        if (!fileElement) return;

        const path = fileElement.dataset.path;
        const file = this.currentFiles.find(f => f.path === path);
        if (!file) return;

        if (event.ctrlKey || event.metaKey) {
            // 多选
            this.toggleFileSelection(path);
        } else {
            // 单选
            this.clearSelection();
            this.selectFile(path);
        }
    }

    async handleFileDoubleClick(event) {
        const fileElement = event.target.closest('.file-item');
        if (!fileElement) return;

        const path = fileElement.dataset.path;
        const file = this.currentFiles.find(f => f.path === path);
        if (!file) return;

        if (file.isDirectory) {
            await this.navigateToPath(path);
        } else {
            await this.openFile(file);
        }
    }

    async openFile(file) {
        try {
            // 检查是否为APK文件
            if (file.name.toLowerCase().endsWith('.apk')) {
                await this.handleAPKFile(file);
            } else {
                // 尝试预览文件
                await window.lanheFileManager.previewFile(file.path);
            }
        } catch (error) {
            this.showToast('无法打开文件: ' + error.message, 'error');
        }
    }

    async handleAPKFile(file) {
        try {
            // 分析APK
            const analysis = await window.lanheFileManager.analyzeAPK(file.path);

            // 显示APK安装对话框
            this.showAPKInstallDialog(file, analysis);
        } catch (error) {
            this.showToast('APK分析失败: ' + error.message, 'error');
        }
    }

    showAPKInstallDialog(file, analysis) {
        const dialog = document.getElementById('apk-install-dialog');
        const apkInfo = document.getElementById('apk-info');

        apkInfo.innerHTML = `
            <div class="apk-header">
                <div class="apk-icon">${this.getFileIcon(file)}</div>
                <div class="apk-basic-info">
                    <h4>${file.name}</h4>
                    <p>大小: ${this.formatFileSize(file.size)}</p>
                </div>
            </div>
            ${analysis.packageInfo ? `
                <div class="apk-package-info">
                    <h5>应用信息</h5>
                    <p><strong>包名:</strong> ${analysis.packageInfo.packageName}</p>
                    <p><strong>版本:</strong> ${analysis.packageInfo.versionName} (${analysis.packageInfo.versionCode})</p>
                    <p><strong>名称:</strong> ${analysis.packageInfo.appName}</p>
                    <p><strong>最低系统:</strong> Android ${analysis.packageInfo.minSdkVersion}</p>
                    <p><strong>目标系统:</strong> Android ${analysis.packageInfo.targetSdkVersion}</p>
                </div>
            ` : ''}
            ${analysis.permissions.length > 0 ? `
                <div class="apk-permissions">
                    <h5>权限 (${analysis.permissions.length})</h5>
                    <div class="permissions-list">
                        ${analysis.permissions.slice(0, 5).map(p => `
                            <span class="permission-tag ${p.isDangerous ? 'dangerous' : 'normal'}">
                                ${p.description}
                            </span>
                        `).join('')}
                        ${analysis.permissions.length > 5 ? `<span class="permission-more">+${analysis.permissions.length - 5} 更多</span>` : ''}
                    </div>
                </div>
            ` : ''}
            ${analysis.securityCheck.warnings.length > 0 ? `
                <div class="apk-warnings">
                    <h5>安全警告</h5>
                    ${analysis.securityCheck.warnings.map(w => `<p class="warning">⚠️ ${w}</p>`).join('')}
                </div>
            ` : ''}
        `;

        dialog.style.display = 'flex';

        // 绑定按钮事件
        document.getElementById('apk-cancel').onclick = () => {
            dialog.style.display = 'none';
        };

        document.getElementById('apk-install').onclick = () => {
            this.installAPK(file.path);
            dialog.style.display = 'none';
        };
    }

    async installAPK(filePath) {
        this.showToast('开始安装APK...', 'info');

        try {
            const result = await window.lanheFileManager.installAPK(filePath);

            if (result.success) {
                this.showToast('APK安装成功', 'success');
            } else if (result.requiresUserAction) {
                this.showToast('需要用户确认安装', 'info');
            } else {
                this.showToast('APK安装失败: ' + result.error, 'error');
            }
        } catch (error) {
            this.showToast('APK安装失败: ' + error.message, 'error');
        }
    }

    // 工具方法
    getFileIcon(file) {
        if (file.isDirectory) {
            return '📁';
        }

        const ext = file.name.split('.').pop()?.toLowerCase();
        const iconMap = {
            // 图片
            'jpg': '🖼️', 'jpeg': '🖼️', 'png': '🖼️', 'gif': '🖼️', 'webp': '🖼️',
            // 视频
            'mp4': '🎬', 'mkv': '🎬', 'avi': '🎬', 'mov': '🎬',
            // 音频
            'mp3': '🎵', 'aac': '🎵', 'flac': '🎵', 'wav': '🎵',
            // 文档
            'pdf': '📄', 'doc': '📄', 'docx': '📄', 'txt': '📝',
            // APK
            'apk': '📲',
            // 压缩文件
            'zip': '📦', 'rar': '📦', '7z': '📦',
            // 代码
            'js': '📜', 'py': '🐍', 'java': '☕', 'kt': '🎯',
        };

        return iconMap[ext] || '📄';
    }

    getFileType(file) {
        if (file.isDirectory) return '文件夹';

        const ext = file.name.split('.').pop()?.toLowerCase();
        const typeMap = {
            // 图片
            'jpg': '图片', 'jpeg': '图片', 'png': '图片', 'gif': '图片', 'webp': '图片',
            // 视频
            'mp4': '视频', 'mkv': '视频', 'avi': '视频', 'mov': '视频',
            // 音频
            'mp3': '音频', 'aac': '音频', 'flac': '音频', 'wav': '音频',
            // 文档
            'pdf': '文档', 'doc': '文档', 'docx': '文档', 'txt': '文本',
            // APK
            'apk': '应用',
            // 压缩文件
            'zip': '压缩包', 'rar': '压缩包', '7z': '压缩包',
            // 代码
            'js': 'JavaScript', 'py': 'Python', 'java': 'Java', 'kt': 'Kotlin',
        };

        return typeMap[ext] || '未知';
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 B';

        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    formatDate(timestamp) {
        if (!timestamp) return '未知';

        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;

        const seconds = Math.floor(diff / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (days > 30) {
            return date.toLocaleDateString();
        } else if (days > 0) {
            return `${days}天前`;
        } else if (hours > 0) {
            return `${hours}小时前`;
        } else if (minutes > 0) {
            return `${minutes}分钟前`;
        } else {
            return '刚刚';
        }
    }

    showToast(message, type = 'info') {
        const toast = document.getElementById('toast');
        const toastMessage = document.getElementById('toast-message');

        toastMessage.textContent = message;
        toast.className = `toast ${type}`;
        toast.style.display = 'block';

        setTimeout(() => {
            toast.style.display = 'none';
        }, 3000);
    }

    showLoading() {
        this.loading.style.display = 'flex';
    }

    hideLoading() {
        this.loading.style.display = 'none';
    }

    // 更多方法...
    async getRootPath() {
        // 获取外部存储根路径
        return '/storage/emulated/0';
    }

    goBack() {
        if (this.historyIndex > 0) {
            this.historyIndex--;
            const path = this.history[this.historyIndex];
            this.currentPath = path;
            // 重新加载但不添加到历史记录
        }
    }

    goForward() {
        if (this.historyIndex < this.history.length - 1) {
            this.historyIndex++;
            const path = this.history[this.historyIndex];
            this.currentPath = path;
        }
    }

    goUp() {
        const pathParts = this.currentPath.split('/').filter(part => part.length > 0);
        if (pathParts.length > 0) {
            pathParts.pop();
            const parentPath = '/' + pathParts.join('/');
            this.navigateToPath(parentPath);
        }
    }

    updateNavigationButtons() {
        this.backBtn.disabled = this.historyIndex <= 0;
        this.forwardBtn.disabled = this.historyIndex >= this.history.length - 1;
        this.upBtn.disabled = this.currentPath === '/' || !this.currentPath.includes('/');
    }

    toggleFileSelection(path) {
        if (this.selectedFiles.has(path)) {
            this.selectedFiles.delete(path);
        } else {
            this.selectedFiles.add(path);
        }
        this.renderFiles();
    }

    selectFile(path) {
        this.selectedFiles.clear();
        this.selectedFiles.add(path);
        this.renderFiles();
    }

    clearSelection() {
        this.selectedFiles.clear();
        this.renderFiles();
    }
}

// 初始化文件管理器
document.addEventListener('DOMContentLoaded', () => {
    window.fileManager = new LanheFileManager();
});