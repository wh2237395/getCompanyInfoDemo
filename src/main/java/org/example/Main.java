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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;

import javax.swing.text.Document;
import javax.swing.text.Element;
import java.io.*;
import java.util.*;

public class Main {
    private static final String DETAIL_PAGE_BASE_URL = "https://www.qcc.com/firm/";
    private static final long REQUEST_DELAY = 10000; // 10 秒的延迟，单位为毫秒
    private static String filePath = "C:\\Users\\WH\\Desktop\\商协会123.xlsx";

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
                "qcc_did=2cfbe220-89f8-4f19-b830-8703a5e0755b; UM_distinctid=194ee44031f886-0d6aa827cc2ef5-26011b51-144000-194ee4403201443; QCCSESSID=44ec5f0af542caae399b643645; acw_tc=1a0c380c17392379518828272e0136d31b5f6fef331f2452fa4c3d3ed4cbac; tfstk=gwFSsF1mY3x7rot0rgQqfA5s44cQAk1ZAegLSydyJbhJvHUt04PPqynQAoo0AvboYDnQYyePu15arzcn9XsN_1Jc_EbgNDKE9bcvWlPN815arzHF2Ach_7W1C7nSvXH-pjUxmVLJ9kdKHZgK83ppwkUAlm0H2LH-JEhx82gK9XELkZgnDDh7JigcF0U5P2Wu3X10xznX9BFRtYisPpRp9SgSFcU-cYM7G4MSXxfTaYP80yFzZxXvhX44CkwLXiJr2Rg_cAy5fQZ_q2UIJu1M5mF7RSMu3FR_lvi7H7HXJBUTr8kYdusWE0ezcA0xHe1iuloYr7ef-nU8b0G-kx5O5rHL37DgaidS6R4raJEO0Lo8C2IzLCo_8O9BlAAIlc7flpv34_WptSWCATM-oqPNlZtWKY3mlc7flpvneq04_Z_XVpf..; CNZZDATA1254842228=1249216502-1739164222-https%253A%252F%252Fwww.baidu.com%252F%7C1739237977");

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
                    // 提取并写入xslx
                    Map<String, String> infoMap = extractInfo(responseBody);
                    // 打印 map 中的内容
                    System.out.println("提取的信息如下：");
                    for (Map.Entry<String, String> entry : infoMap.entrySet()) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                    writeInfoToExcel(infoMap, filePath);
                }
            } else {
                System.err.println("详情页请求失败，状态码: " + statusCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    // 提取并处理信息
    private static Map<String, String> extractInfo(String responseBody) {
        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("协会名称", extractInfoBetween(responseBody, "<title>", " - 企查查"));
        infoMap.put("法定代表人", extractInfoBetween(responseBody, "\"operName\":\"", "\""));
        infoMap.put("联系电话", extractInfoBetween(responseBody, "\"contactNo\":\"", "\""));
        infoMap.put("地址", extractInfoBetween(responseBody, "\"address\":\"", "\""));
        infoMap.put("统一社会信用代码", extractInfoBetween(responseBody, "\"creditCode\":\"", "\""));
        return infoMap;
    }


    // 将信息写入 Excel 文件
    private static void writeInfoToExcel(Map<String, String> infoMap, String filePath) {
        Workbook workbook = null;
        Sheet sheet = null;
        try (FileInputStream fis = new FileInputStream(new File(filePath))) {
            workbook = new XSSFWorkbook(fis);
            sheet = workbook.getSheetAt(0);
        } catch (FileNotFoundException e) {
            // 文件不存在，创建新的工作簿和工作表
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("协会信息");
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"协会名称", "法定代表人", "联系电话", "地址", "统一社会信用代码"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 查找是否存在相同的协会名称记录
        boolean found = false;
        int lastRowNum = sheet.getLastRowNum();
        for (int i = 1; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            Cell nameCell = row.getCell(0);
            if (nameCell != null && infoMap.get("协会名称").equals(nameCell.getStringCellValue())) {
                // 存在相同的协会名称，更新记录
                found = true;
                for (int j = 1; j < 5; j++) {
                    Cell cell = row.getCell(j);
                    if (cell == null) {
                        cell = row.createCell(j);
                    }
                    String key = getKeyByIndex(j);
                    String value = infoMap.get(key);
                    if (value != null) {
                        cell.setCellValue(value);
                    }
                }
                break;
            }
        }
        if (!found) {
            // 不存在相同的协会名称，新增记录
            Row newRow = sheet.createRow(lastRowNum + 1);
            for (int i = 0; i < 5; i++) {
                Cell cell = newRow.createCell(i);
                String key = getKeyByIndex(i);
                String value = infoMap.get(key);
                if (value != null) {
                    cell.setCellValue(value);
                }
            }
        }
        // 保存文件
        try (FileOutputStream fos = new FileOutputStream(new File(filePath))) {
            workbook.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (workbook != null) {
                    workbook.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // 根据列索引获取对应的键
    private static String getKeyByIndex(int index) {
        switch (index) {
            case 0:
                return "协会名称";
            case 1:
                return "法定代表人";
            case 2:
                return "联系电话";
            case 3:
                return "地址";
            case 4:
                return "统一社会信用代码";
            default:
                return null;
        }
    }

}