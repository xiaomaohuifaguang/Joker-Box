package com.cat.common.utils.who;

import cn.hutool.extra.pinyin.PinyinUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/***
 * <TODO description class purpose>
 * @title WhoUtils
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/10 1:20
 **/
public class WhoUtils {

    public static final Random RANDOM = new Random();

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for(int i = 0 ; i < 1000 ; i++){
            int sex = RANDOM.nextInt(2);
            String randomName = getRandomName(sex);
            System.out.println("姓名："+ randomName);
            System.out.println("拼音："+ PinyinUtil.getPinyin(randomName));
            System.out.println("账号："+ PinyinUtil.getPinyin(randomName,""));
            System.out.println("性别："+(sex == 1 ? "男" : "女"));
            System.out.println("证件："+generateRandomIDCardNumber(sex));
            System.out.println("电话："+getRandomPhone());
            System.out.println("邮箱："+getRandomEmail());
            System.out.println("========================================================================");
        }
        System.out.println("耗时"+(System.currentTimeMillis()-start)+"ms");
    }

    /**
     * 获取随机手机号
     */
    public static Long getRandomPhone() {
        int phoneTwoRandomIndex = RANDOM.nextInt(4);
        String phoneTwoNum = "6379";
        return Long.parseLong("1" + phoneTwoNum.charAt(phoneTwoRandomIndex) + (100000000 + RANDOM.nextInt(899999999)));
    }

    /**
     * 获取随机邮箱
     */
    public static String getRandomEmail() {
        return getRandomPhone()+"@example.com";
    }

    public static String getRandomName(int sex) {
        sex = sex != 0 && sex != 1 ? RANDOM.nextInt(2) : sex;
        List<String> familyName = readCat("who/family_name.cat");
        List<String> names = readCat(sex == 1 ? "who/boy_name.cat" : "who/girl_name.cat");
        int doubleName = RANDOM.nextInt(2);
        String name = doubleName == 1 ? names.get(RANDOM.nextInt(names.size())) + names.get(RANDOM.nextInt(names.size())) : names.get(RANDOM.nextInt(names.size()));
        return familyName.get(RANDOM.nextInt(familyName.size())) + name;
    }

    public static List<String> readCat(String path){
        List<String> cats = new ArrayList<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(path);
            assert resource != null;
            String file = resource.getFile();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            // 逐行读取文件
            while ((line = reader.readLine()) != null) {
                // 处理每一行
                cats.add(line);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cats;
    }

    public static String generateRandomIDCardNumber(int sex) {
        sex = sex != 0 && sex != 1 ? RANDOM.nextInt(2) : sex;
        StringBuilder sb = new StringBuilder();

        // 行政区划代码（随机选择一个省份或城市）
        sb.append(PROVINCE_CODES.get(RANDOM.nextInt(34)));
        sb.append(String.format("%04d", RANDOM.nextInt(10000)));

        // 出生日期码（随机选择一个日期）
        sb.append(String.format("%04d%02d%02d",
                RANDOM.nextInt(124) + 1900, // 年份范围1900-2023
                RANDOM.nextInt(12) + 1,    // 月份
                RANDOM.nextInt(28) + 1));   // 日期

        // 顺序码（随机选择一个数字，奇数为男性，偶数为女性）
        sb.append(String.format("%03d", RANDOM.nextInt(500) * 2 + sex));

        // 校验码（这里简化处理，实际应使用校验算法）
        sb.append(calculateCheckDigit(sb.substring(0, 17)));

        return sb.toString();
    }

    // 校验码计算方法（简化版，实际应使用更复杂的算法）
    private static int calculateCheckDigit(String str) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += Integer.parseInt(str.substring(i, i + 1)) * (2 ^ (17 - i));
        }
        return (12 - (sum % 11)) % 11;
    }


    /**
     * 身份证前两位 省份代码
     */
    private static final List<String> PROVINCE_CODES = Arrays.asList(
            "11",
            "12",
            "13",
            "14",
            "15",
            "21",
            "22",
            "23",
            "31",
            "32",
            "33",
            "34",
            "35",
            "36",
            "37",
            "41",
            "42",
            "43",
            "44",
            "45",
            "46",
            "51",
            "52",
            "53",
            "54",
            "50",
            "61",
            "62",
            "63",
            "64",
            "65",
            "71",
            "81",
            "82"
    );


}
