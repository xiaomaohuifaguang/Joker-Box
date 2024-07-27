package com.cat.auth.service.impl;

import com.cat.auth.config.redis.RedisService;
import com.cat.auth.service.MailService;
import com.cat.common.entity.CONSTANTS;
import com.cat.common.utils.UUIDUtils;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.util.HashMap;

/***
 * 邮件业务层实现
 * @title MailServiceImpl
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/8 0:19
 **/
@Service
public class MailServiceImpl implements MailService {

    @Value("${spring.mail.username}")
    private String username;
    @Resource
    private JavaMailSender javaMailSender;
    @Resource
    private FreeMarkerConfigurer freeMarkerConfigurer;
    @Resource
    private RedisService redisService;


    @Override
    public String sendCode(String email) throws IOException, TemplateException, MessagingException {
        String code = UUIDUtils.code(6);
        Template template;
        String htmlText;
        template = freeMarkerConfigurer.getConfiguration().getTemplate("code.ftl");
        HashMap<String, Object> map = new HashMap<>();
        map.put("code", code);
        htmlText = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom("Joker-Box<"+username+">");
        helper.setTo(email);
        helper.setSubject("验证码");
        helper.setText(htmlText, true);//不加参数默认是文本，加上true之后支持html格式文件、
        new Thread(()->{
            javaMailSender.send(helper.getMimeMessage());
        }).start();
        redisService.set(CONSTANTS.REDIS_PARENT_MAIL_CODE+email , code, 5 * 60);
        return code;
    }
}
