<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>邮箱验证码 - Joker-Box</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background-color: #f0f2f5;
            color: #1a1a1a;
            margin: 0;
            padding: 20px 0;
            line-height: 1.6;
        }

        .wrapper {
            max-width: 560px;
            margin: 0 auto;
        }

        .card {
            background: #ffffff;
            border-radius: 20px;
            overflow: hidden;
            box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
        }

        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 48px 32px;
            text-align: center;
            position: relative;
        }

        .header::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.08'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
        }

        .header-content {
            position: relative;
            z-index: 1;
        }

        .brand {
            font-size: 14px;
            font-weight: 600;
            color: rgba(255,255,255,0.85);
            letter-spacing: 2px;
            text-transform: uppercase;
            margin-bottom: 12px;
        }

        .header-title {
            font-size: 26px;
            font-weight: 700;
            color: #ffffff;
            margin: 0;
        }

        .header-subtitle {
            font-size: 15px;
            color: rgba(255,255,255,0.8);
            margin-top: 8px;
        }

        .body {
            padding: 40px 36px;
            text-align: center;
        }

        .greeting {
            font-size: 16px;
            color: #555;
            margin-bottom: 8px;
        }

        .instruction {
            font-size: 14px;
            color: #888;
            margin-bottom: 28px;
        }

        .code-box {
            background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
            border: 2px dashed #c7d2fe;
            border-radius: 16px;
            padding: 32px 24px;
            margin: 0 auto 28px;
            max-width: 320px;
            position: relative;
        }

        .code-label {
            font-size: 12px;
            font-weight: 600;
            color: #6366f1;
            letter-spacing: 1.5px;
            text-transform: uppercase;
            margin-bottom: 16px;
            display: block;
        }

        .code-value {
            font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', monospace;
            font-size: 40px;
            font-weight: 700;
            color: #4338ca;
            letter-spacing: 8px;
            display: block;
            text-shadow: 0 2px 4px rgba(67, 56, 202, 0.1);
        }

        .timer {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: #fef3c7;
            color: #92400e;
            font-size: 13px;
            font-weight: 600;
            padding: 8px 18px;
            border-radius: 100px;
            margin-bottom: 24px;
        }

        .divider {
            height: 1px;
            background: linear-gradient(to right, transparent, #e5e7eb, transparent);
            margin: 28px 0;
        }

        .warning {
            background: #fef2f2;
            border-left: 4px solid #ef4444;
            border-radius: 0 8px 8px 0;
            padding: 14px 18px;
            text-align: left;
            margin-bottom: 24px;
        }

        .warning-text {
            font-size: 13px;
            color: #991b1b;
            margin: 0;
            line-height: 1.5;
        }

        .cta-button {
            display: inline-block;
            padding: 14px 40px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #ffffff !important;
            text-decoration: none;
            border-radius: 12px;
            font-size: 15px;
            font-weight: 600;
            box-shadow: 0 4px 14px rgba(102, 126, 234, 0.35);
        }

        .footer {
            background: #f9fafb;
            padding: 28px 36px;
            text-align: center;
        }

        .footer-hint {
            font-size: 13px;
            color: #6b7280;
            margin: 0 0 16px;
        }

        .footer-links {
            margin-bottom: 16px;
        }

        .footer-links a {
            color: #667eea;
            text-decoration: none;
            font-size: 13px;
            font-weight: 500;
            margin: 0 10px;
        }

        .copyright {
            font-size: 12px;
            color: #9ca3af;
            margin: 0;
        }

        @media (max-width: 480px) {
            .body { padding: 32px 24px; }
            .header { padding: 36px 24px; }
            .code-value { font-size: 32px; letter-spacing: 6px; }
        }
    </style>
</head>
<body>
<div class="wrapper">
    <div class="card">
        <div class="header">
            <div class="header-content">
                <div class="brand">Joker-Box</div>
                <h1 class="header-title">邮箱验证码</h1>
                <p class="header-subtitle">请使用以下验证码完成身份验证</p>
            </div>
        </div>

        <div class="body">
            <p class="greeting">您好，欢迎登录 Joker-Box</p>
            <p class="instruction">请输入下方的 6 位验证码以继续操作</p>

            <div class="code-box">
                <span class="code-label">Verification Code</span>
                <span class="code-value">${code}</span>
            </div>

            <div class="timer">
                &#9201; 此验证码将在 <strong>5 分钟</strong> 后失效
            </div>

            <div class="warning">
                <p class="warning-text">&#9888; 请勿将验证码告知他人，包括客服人员。如非本人操作，请忽略此邮件。</p>
            </div>

            <div class="divider"></div>

            <p class="instruction">或点击下方按钮直接访问平台</p>
            <a href="http://localhost:5173" class="cta-button">立即访问 Joker-Box</a>
        </div>

        <div class="footer">
            <p class="footer-hint">如果按钮无法点击，请复制下方链接到浏览器中打开</p>
            <p style="font-size:12px;color:#9ca3af;word-break:break-all;margin:0 0 16px;">http://localhost:5173</p>
            <div class="footer-links">
                <a href="#">帮助中心</a>
                <a href="#">隐私政策</a>
                <a href="#">联系我们</a>
            </div>
            <p class="copyright">&#169; ${nowYear} Joker-Box. 保留所有权利</p>
        </div>
    </div>
</div>
</body>
</html>