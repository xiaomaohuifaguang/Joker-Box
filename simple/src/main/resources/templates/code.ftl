<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>邮箱验证码 - Joker-Box</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600&display=swap');

        body {
            font-family: 'Poppins', Arial, sans-serif;
            background-color: #f5f7fa;
            color: #333;
            margin: 0;
            padding: 0;
            line-height: 1.6;
        }

        .container {
            margin: 40px auto;
            padding: 40px;
            max-width: 600px;
            background: linear-gradient(135deg, #ffffff 0%, #f9f9f9 100%);
            border-radius: 12px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
            text-align: center;
            border: 1px solid rgba(255, 255, 255, 0.3);
        }

        .logo {
            width: 120px;
            margin-bottom: 20px;
        }

        h1 {
            color: #333;
            font-weight: 600;
            margin: 20px 0;
            font-size: 28px;
        }

        h2 {
            color: #ff6b00;
            font-weight: 500;
            margin: 15px 0;
            font-size: 22px;
        }

        .code-container {
            margin: 30px 0;
            padding: 5px;
            background: linear-gradient(135deg, #ff6b00 0%, #ff9e00 100%);
            border-radius: 8px;
            display: inline-block;
            box-shadow: 0 4px 15px rgba(255, 107, 0, 0.3);
        }

        .code {
            font-size: 42px;
            font-weight: 600;
            letter-spacing: 5px;
            padding: 15px 30px;
            background-color: white;
            color: #ff6b00;
            border-radius: 6px;
            display: inline-block;
        }

        p {
            font-size: 16px;
            margin: 15px 0;
            color: #555;
        }

        .highlight {
            color: #ff6b00;
            font-weight: 500;
        }

        .break {
            height: 1px;
            background: linear-gradient(to right, transparent, rgba(0, 0, 0, 0.1), transparent);
            margin: 30px 0;
        }

        .btn {
            display: inline-block;
            margin-top: 25px;
            padding: 12px 30px;
            background: linear-gradient(135deg, #ff6b00 0%, #ff9e00 100%);
            color: white;
            text-decoration: none;
            border-radius: 30px;
            font-weight: 500;
            box-shadow: 0 4px 15px rgba(255, 107, 0, 0.3);
            transition: all 0.3s ease;
        }

        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(255, 107, 0, 0.4);
        }

        .footer {
            margin-top: 30px;
            font-size: 14px;
            color: #888;
        }

        .social-icons {
            margin: 20px 0;
        }

        .social-icon {
            display: inline-block;
            margin: 0 10px;
            width: 32px;
            height: 32px;
        }
    </style>
</head>
<body>
<div class="container">
<#--    <img src="https://yourdomain.com/logo.png" alt="Joker-Box Logo" class="logo">-->
    <h1>欢迎使用 Joker-Box</h1>
    <h2>您的验证码</h2>

    <div class="code-container">
        <div class="code">${code}</div>
    </div>

    <p>此验证码将在 <span class="highlight">5分钟</span> 后失效</p>
    <p>请勿将此验证码分享给他人！</p>

    <div class="break"></div>

    <p>点击下方按钮立即访问 Joker-Box</p>
    <a href="http://localhost:5173" class="btn">立即访问</a>

    <div class="social-icons">
<#--        <a href="#"><img src="https://yourdomain.com/icons/facebook.png" class="social-icon"></a>-->
<#--        <a href="#"><img src="https://yourdomain.com/icons/twitter.png" class="social-icon"></a>-->
<#--        <a href="#"><img src="https://yourdomain.com/icons/instagram.png" class="social-icon"></a>-->
    </div>

    <div class="footer">
        <p>如果您没有请求此验证码，请忽略此邮件</p>
        <p>© ${nowYear} Joker-Box. 保留所有权利</p>
    </div>
</div>
</body>
</html>