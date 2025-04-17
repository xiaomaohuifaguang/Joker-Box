package com.cat.simple.service.impl;

import com.alibaba.fastjson2.JSON;
import com.cat.common.entity.mail.MailInfo;
import com.cat.simple.config.redis.RedisService;
import com.cat.simple.service.MailInfoService;
import com.cat.simple.service.MailService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Resource
    private MailInfoService mailInfoService;


    @Override
    public String sendCode(String email) throws IOException, TemplateException, MessagingException {

        String code = UUIDUtils.code(6);
        HashMap<String, Object> map = new HashMap<>();
        map.put("code", code);
        sendMail("code.ftl", map, email, "验证码" );

        redisService.set(CONSTANTS.REDIS_PARENT_MAIL_CODE+email , code, 5 * 60);
        return code;
    }

    @Override
    public void notification(String email, String nickname, String content) throws MessagingException, IOException, TemplateException {

        HashMap<String, Object> map = new HashMap<>();
        map.put("nickname", nickname);
        map.put("content", content);

        sendMail("notification.ftl", map, email, "通知" );

    }



    private void sendMail(String templateName,HashMap<String, Object> map, String to, String subject) throws IOException, TemplateException, MessagingException {

        Template template = freeMarkerConfigurer.getConfiguration().getTemplate(templateName);

        map.put("nowYear", LocalDate.now().getYear());
        String  htmlText = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom("Joker-Box<"+username+">");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlText, true);//不加参数默认是文本，加上true之后支持html格式文件
        new Thread(()->{
            javaMailSender.send(helper.getMimeMessage());

            MailInfo mailInfo = new MailInfo();
            mailInfo.setToMail(to);
            mailInfo.setSubject(subject);
            mailInfo.setContent(htmlText);
            mailInfo.setVariable(JSON.toJSONString(map));
            mailInfo.setSendTime(LocalDateTime.now());
            mailInfoService.add(mailInfo);


        }).start();

    }





}
