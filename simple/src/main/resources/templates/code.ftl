<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>邮箱验证码</title>
    <link href="https://cdn.jsdelivr.net/npm/daisyui@4.12.10/dist/full.min.css" rel="stylesheet" type="text/css" />
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #262626;
            color: #E6E6E6;
        }
        .container {
            margin: 20px auto;
            padding: 20px 30px;
            max-width: 600px;
            background-color: #333333;
            border-radius: 3px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.2);
            text-align: center;
        }
        h1, h2, h3 {
            font-weight: normal;
            margin-top: 10px;
            margin-bottom: 20px;
        }
        h2 {
            color: #FF9900;
        }
        .code {
            font-size: 36px;
            margin-top: 30px;
            margin-bottom: 30px;
            padding: 10px;
            background-color: #E6E6E6;
            color: #333333;
            border-radius: 3px;
            box-shadow: 0 1px 2px rgba(0,0,0,0.1);
        }
        p {
            font-size: 16px;
            margin-top: 20px;
            margin-bottom: 10px;
        }
        .break {
            height: 1px;
            background-color: #FFFFFF;
            margin-top: 30px;
            margin-bottom: 30px;
        }
    </style>
</head>
<body>
<div class="container">
    <h2>欢迎使用Joker-Box网站！</h2>
    <p>您的验证码是：</p>
    <div class="code">${code}</div>
    <p>5分钟内有效</p>
    <p>请勿将此验证码分享给他人！</p>
    <div class="break"></div>
    <p>欢迎来到<a href="http://127.0.0.1/web">Joker-Box</a></p>
</div>
</body>
</html>