package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;

import javax.swing.text.Document;
import javax.swing.text.Element;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String DETAIL_PAGE_BASE_URL = "https://www.qcc.com/firm/";
    private static final long REQUEST_DELAY = 10000; // 10 秒的延迟，单位为毫秒

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\\Z"); // 设置分隔符为文件结束符
        System.out.println("请输入 JSON 数据:");
        String jsonInput = scanner.next();

        List<String> companyKeynos = extractCompanyKeynos(jsonInput);
        for (String keyno : companyKeynos) {
            String detailPageUrl = generateDetailPageUrl(keyno);
            System.out.println("拼接后的详情页 URL: " + detailPageUrl);
            // 发送请求获取详情页内容
            sendRequestToDetailPage(detailPageUrl);
            try {
                // 添加 10 秒的延迟
                Thread.sleep(REQUEST_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        scanner.close();
    }


    public static List<String> extractCompanyKeynos(String jsonInput) {
        List<String> companyKeynos = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonInput);
            JsonNode keynosNode = rootNode.get("keynos");
            if (keynosNode != null && keynosNode.isArray()) {
                for (JsonNode item : keynosNode) {
                    if (item.isTextual()) {
                        companyKeynos.add(item.asText());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return companyKeynos;
    }


    public static String generateDetailPageUrl(String companyKeyno) {
        return DETAIL_PAGE_BASE_URL + companyKeyno + ".html";
    }


    public static void sendRequestToDetailPage(String url) {
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        // 设置请求头，可根据实际情况调整
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36");
        httpGet.setHeader("content-type", "application/json");
        // cookie 需要修改
        httpGet.setHeader("Cookie", "\n" +
                "qcc_did=2cfbe220-89f8-4f19-b830-8703a5e0755b; UM_distinctid=194ee44031f886-0d6aa827cc2ef5-26011b51-144000-194ee4403201443; acw_tc=0a472f8417391735466311576e0041b548c0ccc0d1aee5bac07f66d422f16f; QCCSESSID=44ec5f0af542caae399b643645; tfstk=gyzKZxvZrOXneE0c-wsiZJKDHTfgFzFFtJPXrYDHVReTGSJnVDXEele7QDDB-6oRwRw4t048aUh7N85EE6SgTWurPtX0eZVUTi6jecUpRNt__j9BFGmI_HEI0tXcoN0HMFU1nJqs5j7taAGINDg56cGqM4OBR8MsfjGDd4g7F13sZbOBO0iIfcGmCYgSP8N16blkVBHkOYL8DTJ61t_M7UYuWXnKeUc6yY4BockbOALWyPhdPxNIBUT-KgszG5FhpEPi8rexiJb6J-F7guiTyd6xUPF_RuNwp1gY6lqmvlBByA4nQmzIkQ_Q62HKczeNz6yb67ZmXPffu23Idu0UZIB36ye3TPFkNhgK-lnYJq_2dYqgMzh8zT7TH7N4VcUJpg5zorHWRKDxZH1O63-rAfzN40OvlbZmJfHcshxy4ccZ6xfO63-rAfltn1xM43oi_; CNZZDATA1254842228=1249216502-1739164222-https%253A%252F%252Fwww.baidu.com%252F%7C1739173865");

        try {

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("详情页请求状态码: " + statusCode);
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String responseBody = EntityUtils.toString(entity);
                    System.out.println("详情页响应内容长度: " + responseBody.length());
                    // 这里可以对详情页内容进行进一步处理
                    // 提取并拼接信息
                    extractAndPrintInfo(responseBody);
                }
            } else {
                System.err.println("详情页请求失败，状态码: " + statusCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void extractAndPrintInfo(String responseBody) {
        String title = extractInfoBetween(responseBody, "<title>", "</title>");
        if (title != null) {
            title = "title" + title ;
        } else {
            title = "\"title\":\"\"";
        }
        String operName = extractInfoBetween(responseBody, "\"operName\":\"", "\"");
        if (operName != null) {
            operName = "\"operName\":\"" + operName + "\"";
        } else {
            operName = "\"operName\":\"\"";
        }
        String contactNo = extractInfoBetween(responseBody, "\"contactNo\":\"", "\"");
        if (contactNo != null) {
            contactNo = "\"contactNo\":\"" + contactNo + "\"";
        } else {
            contactNo = "\"contactNo\":\"\"";
        }
        String address = extractInfoBetween(responseBody, "\"address\":\"", "\"");
        String creditCode = extractInfoBetween(responseBody, "\"creditCode\":\"", "\"");
        String djInfo = "{\"DJInfo\":{";
        if (address != null) {
            djInfo += "\"address\":\"" + address + "\"";
            if (creditCode != null) {
                djInfo += ",\"creditCode\":\"" + creditCode + "\"";
            }
        } else if (creditCode != null) {
            djInfo += "\"creditCode\":\"" + creditCode + "\"";
        }
        djInfo += "}}";
        String result = title + "," + operName + "," + contactNo + "," + djInfo;
        System.out.println(result);
    }
    private static String extractInfoBetween(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex != -1) {
            startIndex += start.length();
            int endIndex = source.indexOf(end, startIndex);
            if (endIndex != -1) {
                return source.substring(startIndex, endIndex);
            }
        }
        return null;
    }

}