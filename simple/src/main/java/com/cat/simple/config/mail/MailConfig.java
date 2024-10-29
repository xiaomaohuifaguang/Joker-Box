package com.cat.simple.config.mail;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

//    @Bean
//    public JavaMailSender javaMailSender() {
//        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
//        mailSender.setHost("smtp.163.com");
//        mailSender.setPort(465);
//        mailSender.setUsername("xiaomaohuifaguang@163.com");
//        mailSender.setPassword("OCFCJALGZHBXHARC");
//        mailSender.setDefaultEncoding("UTF-8");
//        Properties props = new Properties();
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", "true");
//        props.put("mail.smtp.starttls.trust", "smtp.163.com");
//        props.put("mail.smtp.starttls.required", "true");
//        mailSender.setJavaMailProperties(props);
//        return mailSender;
//    }

}
