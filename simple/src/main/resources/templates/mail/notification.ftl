<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>通知 - Joker-Box</title>
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

        .email-container {
            max-width: 600px;
            margin: 30px auto;
            background-color: #ffffff;
            border-radius: 12px;
            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
            overflow: hidden;
        }

        .header {
            background: linear-gradient(135deg, #ff6b00 0%, #ff9e00 100%);
            padding: 30px;
            text-align: center;
            color: white;
        }

        .logo {
            width: 120px;
            margin-bottom: 15px;
        }

        h1 {
            margin: 0;
            font-size: 24px;
            font-weight: 600;
        }

        .content {
            padding: 40px;
        }

        .greeting {
            font-size: 18px;
            margin-bottom: 25px;
            color: #555;
        }

        .message {
            font-size: 16px;
            line-height: 1.8;
            color: #444;
            margin-bottom: 30px;
            white-space: pre-line;
        }

        .bottom-container {
            text-align: center;
            margin: 30px 0;
        }

        .action-button {
            display: inline-block;
            padding: 12px 30px;
            background: linear-gradient(135deg, #ff6b00 0%, #ff9e00 100%);
            color: white;
            text-decoration: none;
            border-radius: 30px;
            font-weight: 500;
            box-shadow: 0 4px 15px rgba(255, 107, 0, 0.3);
            transition: all 0.3s ease;
        }

        .action-button:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(255, 107, 0, 0.4);
        }

        .footer {
            padding: 20px;
            text-align: center;
            background-color: #f9f9f9;
            color: #888;
            font-size: 14px;
        }

        .social-icons {
            margin: 20px 0;
        }

        .social-icon {
            display: inline-block;
            margin: 0 10px;
            width: 24px;
            height: 24px;
        }

        @media (max-width: 600px) {
            .content {
                padding: 30px 20px;
            }

            .header {
                padding: 20px;
            }

            h1 {
                font-size: 20px;
            }
        }
    </style>
</head>
<body>
<div class="email-container">
    <div class="header">
<#--        <img src="https://yourdomain.com/logo-white.png" alt="Joker-Box Logo" class="logo">-->
        <h1>通知</h1>
    </div>

    <div class="content">
        <div class="greeting">${nickname}，你好：</div>

        <div class="message">
            ${content}
        </div>
    </div>

    <div class="footer">
        <div class="social-icons">
<#--            <a href="#"><img src="https://yourdomain.com/icons/facebook.png" class="social-icon"></a>-->
<#--            <a href="#"><img src="https://yourdomain.com/icons/twitter.png" class="social-icon"></a>-->
<#--            <a href="#"><img src="https://yourdomain.com/icons/instagram.png" class="social-icon"></a>-->
        </div>
        <p>如果您有任何疑问，请联系我们：xiaomaohuifaguang@163.com</p>
        <p>© ${nowYear} Joker-Box. 保留所有权利</p>
    </div>
</div>
</body>
</html>