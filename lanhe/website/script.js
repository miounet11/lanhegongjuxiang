// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    // 初始化导航栏
    initNavigation();

    // 初始化滚动动画
    initScrollAnimation();

    // 初始化表单处理
    initContactForm();

    // 初始化统计数字动画
    initStatsAnimation();
});

// 导航栏功能
function initNavigation() {
    const navToggle = document.querySelector('.nav-toggle');
    const navMenu = document.querySelector('.nav-menu');

    // 移动端菜单切换
    if (navToggle && navMenu) {
        navToggle.addEventListener('click', function() {
            navMenu.classList.toggle('active');
            navToggle.classList.toggle('active');
        });
    }

    // 导航链接点击处理
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();

            const targetId = this.getAttribute('href');
            const targetSection = document.querySelector(targetId);

            if (targetSection) {
                const offsetTop = targetSection.offsetTop - 80; // 减去导航栏高度
                window.scrollTo({
                    top: offsetTop,
                    behavior: 'smooth'
                });
            }

            // 关闭移动端菜单
            if (navMenu.classList.contains('active')) {
                navMenu.classList.remove('active');
                navToggle.classList.remove('active');
            }

            // 更新活动链接
            navLinks.forEach(link => link.classList.remove('active'));
            this.classList.add('active');
        });
    });

    // 滚动时更新活动导航链接
    window.addEventListener('scroll', function() {
        const scrollPosition = window.scrollY + 100;

        document.querySelectorAll('section').forEach(section => {
            const sectionTop = section.offsetTop;
            const sectionHeight = section.offsetHeight;
            const sectionId = section.getAttribute('id');

            if (scrollPosition >= sectionTop && scrollPosition < sectionTop + sectionHeight) {
                navLinks.forEach(link => {
                    link.classList.remove('active');
                    if (link.getAttribute('href') === `#${sectionId}`) {
                        link.classList.add('active');
                    }
                });
            }
        });
    });
}

// 滚动动画
function initScrollAnimation() {
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver(function(entries) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('fade-in-up');
            }
        });
    }, observerOptions);

    // 为需要动画的元素添加观察器
    document.querySelectorAll('.feature-card, .download-card, .contact-item, .stat-box').forEach(card => {
        observer.observe(card);
    });
}

// 联系表单处理
function initContactForm() {
    const contactForm = document.getElementById('contactForm');

    if (contactForm) {
        contactForm.addEventListener('submit', function(e) {
            e.preventDefault();

            // 获取表单数据
            const formData = {
                name: document.getElementById('name').value,
                email: document.getElementById('email').value,
                subject: document.getElementById('subject').value,
                message: document.getElementById('message').value
            };

            // 表单验证
            if (validateForm(formData)) {
                // 模拟发送消息
                showMessage('消息发送成功！我们会尽快回复您。', 'success');

                // 清空表单
                contactForm.reset();
            }
        });
    }
}

// 表单验证
function validateForm(data) {
    if (!data.name.trim()) {
        showMessage('请输入您的姓名', 'error');
        return false;
    }

    if (!data.email.trim()) {
        showMessage('请输入您的邮箱', 'error');
        return false;
    }

    if (!isValidEmail(data.email)) {
        showMessage('请输入有效的邮箱地址', 'error');
        return false;
    }

    if (!data.subject.trim()) {
        showMessage('请输入主题', 'error');
        return false;
    }

    if (!data.message.trim()) {
        showMessage('请输入消息内容', 'error');
        return false;
    }

    return true;
}

// 邮箱验证
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// 显示消息
function showMessage(message, type) {
    // 移除现有的消息
    const existingMessage = document.querySelector('.message');
    if (existingMessage) {
        existingMessage.remove();
    }

    // 创建新消息
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;
    messageDiv.textContent = message;

    // 添加到页面
    document.body.appendChild(messageDiv);

    // 3秒后自动移除
    setTimeout(() => {
        if (messageDiv.parentNode) {
            messageDiv.remove();
        }
    }, 3000);
}

// 统计数字动画
function initStatsAnimation() {
    const stats = document.querySelectorAll('.stat-number');

    const observerOptions = {
        threshold: 0.5
    };

    const observer = new IntersectionObserver(function(entries) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                animateNumber(entry.target);
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    stats.forEach(stat => {
        observer.observe(stat);
    });
}

// 数字动画效果
function animateNumber(element) {
    const target = parseFloat(element.textContent.replace(/[^\d.]/g, ''));
    const isPercentage = element.textContent.includes('%');
    const hasPlus = element.textContent.includes('+');

    let current = 0;
    const increment = target / 50; // 50帧动画
    const duration = 1500; // 1.5秒
    const step = duration / 50;

    const timer = setInterval(() => {
        current += increment;

        if (current >= target) {
            current = target;
            clearInterval(timer);
        }

        let displayValue = Math.floor(current);
        if (target % 1 !== 0) {
            displayValue = current.toFixed(1);
        }

        element.textContent = displayValue +
            (isPercentage ? '%' : '') +
            (hasPlus ? '+' : '');
    }, step);
}

// 页面加载动画
window.addEventListener('load', function() {
    document.body.classList.add('loaded');

    // 为初始可见的元素添加动画
    setTimeout(() => {
        document.querySelectorAll('.hero-title, .hero-subtitle, .hero-buttons').forEach((element, index) => {
            setTimeout(() => {
                element.style.opacity = '1';
                element.style.transform = 'translateY(0)';
            }, index * 200);
        });
    }, 300);
});

// 添加一些CSS动画样式
const style = document.createElement('style');
style.textContent = `
    .hero-title, .hero-subtitle, .hero-buttons {
        opacity: 0;
        transform: translateY(30px);
        transition: all 0.6s ease;
    }

    .message {
        position: fixed;
        top: 100px;
        right: 20px;
        padding: 1rem 2rem;
        border-radius: 8px;
        color: white;
        font-weight: 500;
        z-index: 1000;
        animation: slideInRight 0.3s ease;
    }

    .message.success {
        background: #28a745;
    }

    .message.error {
        background: #dc3545;
    }

    @keyframes slideInRight {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }

    body.loaded {
        opacity: 1;
    }

    body {
        opacity: 0;
        transition: opacity 0.3s ease;
    }

    /* 移动端导航菜单样式 */
    .nav-menu.active {
        display: flex;
        flex-direction: column;
        position: absolute;
        top: 100%;
        left: 0;
        right: 0;
        background: white;
        padding: 1rem;
        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }

    .nav-menu.active .nav-link {
        margin: 0.5rem 0;
        padding: 0.5rem;
        border-bottom: 1px solid #e9ecef;
    }

    .nav-menu.active .nav-link:last-child {
        border-bottom: none;
    }

    .nav-toggle.active span:nth-child(1) {
        transform: rotate(45deg) translate(5px, 5px);
    }

    .nav-toggle.active span:nth-child(2) {
        opacity: 0;
    }

    .nav-toggle.active span:nth-child(3) {
        transform: rotate(-45deg) translate(7px, -6px);
    }
`;
document.head.appendChild(style);

// 下载按钮点击跟踪
document.addEventListener('click', function(e) {
    const target = e.target.closest('.btn-download');
    if (target) {
        // 这里可以添加下载统计代码
        console.log('下载按钮被点击');

        // 显示下载提示
        showMessage('开始下载蓝河工具箱...', 'success');
    }
});

// 添加一些有用的工具函数
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// 防抖处理的滚动事件
const debouncedScroll = debounce(function() {
    // 处理滚动相关的逻辑
}, 10);

window.addEventListener('scroll', debouncedScroll);

// 添加错误处理
window.addEventListener('error', function(e) {
    console.error('页面错误:', e.error);
    // 可以在这里添加错误报告逻辑
});

// 添加一些辅助功能
function copyToClipboard(text) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
            showMessage('已复制到剪贴板', 'success');
        });
    } else {
        // 兼容性处理
        const textArea = document.createElement('textarea');
        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showMessage('已复制到剪贴板', 'success');
    }
}

// 为邮箱和联系方式添加点击复制功能
document.addEventListener('click', function(e) {
    if (e.target.closest('.contact-details')) {
        const text = e.target.closest('.contact-details').querySelector('p').textContent;
        if (text.includes('@') || text.includes('http')) {
            copyToClipboard(text);
        }
    }
});
