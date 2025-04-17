package tmp;

import com.alibaba.fastjson2.JSONObject;
import com.cat.common.utils.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {


    public static void main(String[] args) throws IOException {

        String parentPath = "C:\\Users\\six6\\todo\\projects\\be\\数据集";

        File file = new File(parentPath);
        File[] files = file.listFiles();
        List<Map<String, String>> list = new ArrayList<>();
        if (files != null) {
            for (File folder : files) {
                File[] filesTxt = folder.listFiles(File::isFile);
                if (filesTxt != null) {
                    Map<String, String> map = new HashMap<>();
                    for (File txt : filesTxt) {
                        String txtStr = IOUtils.readTextByPath(txt.getPath());
                        switch (txt.getName()){
//                            case "input.txt":{
//                                map.put("input",txtStr);break;
//                            }
                            case "output.txt":{
                                map.put("output",txtStr);break;
                            }
                            case "input-all.txt":{
                                map.put("input",txtStr);break;
                            }
                            case "instruction.txt":{
                                map.put("instruction",txtStr);break;
                            }
                        }
                    }
                    list.add(map);
                }
            }
        }

        System.out.println(JSONObject.toJSONString(list));

    }


    public static void reqOllamaApi() throws IOException {
        String str = "下面我将给你一个段落 需要提取 合并资产负债表 所有指标值和单位\n" +
                "\n" +
                "返回样例：\n" +
                "{\n" +
                "    \"现金及存放中央银行款项\":[\n" +
                "        {\n" +
                "            \"值\":\"1,337,501\t\",\n" +
                "            \"单位\":\"人民币百万元\",\n" +
                "            \"日期\":\"2023年12月31日\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n" +
                "\n" +
                "\n" +
                "提取段落如下：\n" +
                "合并及银行资产负债表\n" +
                "2023年12月31日\n" +
                "（除特别注明外，金额单位均为人民币百万元）\n" +
                "合并 银行\n" +
                "2023年 2022年 2023年 2022年\n" +
                "附注 12月31日 12月31日 12月31日 12月31日\n" +
                "资产\n" +
                "现金及存放中央银行款项 八、1 1,337,501 1,263,951 1,336,884 1,263,786\n" +
                "存放同业款项 八、2 189,216 161,422 190,210 158,292\n" +
                "拆出资金 八、3 297,742 303,836 304,653 310,449\n" +
                "衍生金融资产 八、4 2,154 1,905 2,154 1,905\n" +
                "买入返售金融资产 八、5 409,526 229,870 405,983 229,819\n" +
                "发放贷款和垫款 八、6 7,915,245 6,977,710 7,855,535 6,931,162\n" +
                "金融投资\n" +
                "交易性金融资产 八、7 888,516 863,783 887,560 863,483\n" +
                "债权投资 八、8 3,988,210 3,669,598 3,981,244 3,667,138\n" +
                "其他债权投资 八、9 503,536 416,172 497,662 409,435\n" +
                "其他权益工具投资 八、10 7,326 9,346 7,326 9,346\n" +
                "长期股权投资 八、11 673 653 15,115 15,115\n" +
                "固定资产 八、13 44,139 40,184 44,022 40,066\n" +
                "在建工程 八、14 11,081 13,088 11,081 13,081\n" +
                "使用权资产 八、15 10,006 10,632 9,809 10,324\n" +
                "无形资产 八、16 7,809 7,251 7,455 7,012\n" +
                "递延所得税资产 八、17 62,508 63,955 61,656 62,722\n" +
                "其他资产 八、18 51,443 33,926 50,888 33,414\n" +
                "资产总计 15,726,631 14,067,282 15,669,237 14,026,549\n" +
                "PB 中国邮政储蓄银行股份有限公司 | 2023年年度报告 201\n" +
                "\n" +
                "合并及银行资产负债表\n" +
                "2023年12月31日\n" +
                "（除特别注明外，金额单位均为人民币百万元）\n" +
                "合并 银行\n" +
                "2023年 2022年 2023年 2022年\n" +
                "附注 12月31日 12月31日 12月31日 12月31日\n" +
                "负债\n" +
                "向中央银行借款 八、20 33,835 24,815 33,835 24,815\n" +
                "同业及其他金融机构存放款项 八、21 95,303 78,770 97,986 80,714\n" +
                "拆入资金 八、22 60,212 42,699 20,593 11,389\n" +
                "衍生金融负债 八、4 3,595 2,465 3,595 2,465\n" +
                "卖出回购金融资产款 八、23 273,364 183,646 273,364 183,646\n" +
                "吸收存款 八、24 13,955,963 12,714,485 13,946,123 12,712,659\n" +
                "应付职工薪酬 八、25 23,431 22,860 23,076 22,575\n" +
                "应交税费 八、26 4,167 7,240 3,999 6,596\n" +
                "应付债券 八、27 261,138 101,910 261,138 101,910\n" +
                "租赁负债 八、28 9,268 9,852 9,023 9,519\n" +
                "递延所得税负债 八、17 4 11 – –\n" +
                "其他负债 八、29 49,735 52,715 48,163 51,619\n" +
                "负债合计 14,770,015 13,241,468 14,720,895 13,207,907\n" +
                "股东权益\n" +
                "股本 八、30.1 99,161 92,384 99,161 92,384\n" +
                "其他权益工具\n" +
                "永续债 八、30.2 169,986 139,986 169,986 139,986\n" +
                "资本公积 八、31 162,682 124,479 162,693 124,490\n" +
                "其他综合收益 八、45 5,034 4,918 4,991 4,878\n" +
                "盈余公积 八、32 67,010 58,478 67,010 58,478\n" +
                "一般风险准备 八、33 201,696 178,784 198,910 176,246\n" +
                "未分配利润 八、34 249,304 225,196 245,591 222,180\n" +
                "归属于银行股东权益合计 954,873 824,225 948,342 818,642\n" +
                "少数股东权益 1,743 1,589 – –\n" +
                "股东权益合计 956,616 825,814 948,342 818,642\n" +
                "负债及股东权益总计 15,726,631 14,067,282 15,669,237 14,026,549\n" +
                "后附财务报表附注为本财务报表的组成部分。\n" +
                "刘建军 徐学明 邓萍\n" +
                "法定代表人 主管财务工作副行长 财务会计部负责人\n" +
                "202 中国邮政储蓄银行股份有限公司 | 2023年年度报告 PB\n" +
                "\n" +
                "概况 讨论与分析 公司治理 财务报告及其他";



        HttpPost httpPost = new HttpPost("http://localhost:11434/api/chat");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model","wangge:deepseek-r1-7b");
        jsonObject.put("stream",false);
        List<Map<String,String>> messages = new ArrayList<>();
        messages.add(new HashMap<>(){{
            put("role","user");
            put("content",str);
        }});
        jsonObject.put("messages",messages);
        // 将 options 设置为 Map 对象
//        Map<String, Object> options = new HashMap<>();
//        options.put("seed", 101);
//        options.put("temperature", 0);
//        jsonObject.put("options", options);
        String jsonString = jsonObject.toJSONString();
        StringEntity entity = new StringEntity(jsonString,"UTF-8");
        httpPost.setEntity(entity);

        CloseableHttpClient build = HttpClientBuilder.create().build();
        CloseableHttpResponse execute = build.execute(httpPost);
        HttpEntity entity1 = execute.getEntity();
        System.out.println(EntityUtils.toString(entity1));
    }


}
