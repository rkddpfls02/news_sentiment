import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class NaverNewsSearch {

    public static void main(String[] args) {
        String clientId = "OE2sHIk6MRnEbwBG8PuV"; // ���̹� Ŭ���̾�Ʈ ID
        String clientSecret = "clvSRTarvD"; // ���̹� Ŭ���̾�Ʈ ��ũ��

        String inputFilePath = "C:\\Users\\Public\\title.xlsx"; // �Է� Excel ���� ���
        String outputFilePath = "C:\\Users\\Public\\news_java.xlsx"; // ��� Excel ���� ���

        try (FileInputStream fis = new FileInputStream(inputFilePath);
             Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(outputFilePath)) {

            Sheet inputSheet = new XSSFWorkbook(fis).getSheetAt(0); // �Է� ������ �б�
            Sheet outputSheet = workbook.createSheet("Results"); // ��� ������ ����

            // ��� �ۼ�
            Row headerRow = outputSheet.createRow(0);
            headerRow.createCell(0).setCellValue("title");
            headerRow.createCell(1).setCellValue("content");

            // ������ �� ó��
            for (int i = 1; i <= inputSheet.getLastRowNum(); i++) {
                Row inputRow = inputSheet.getRow(i);
                if (inputRow == null) continue;

                Cell titleCell = inputRow.getCell(0); // title�� ù ��° �÷�
                if (titleCell == null || titleCell.getCellType() != CellType.STRING) continue;

                String title = titleCell.getStringCellValue();
                String description = searchNaverNews(title, clientId, clientSecret);

                // ��� ������ �ۼ�
                Row outputRow = outputSheet.createRow(i);
                outputRow.createCell(0).setCellValue(title);
                outputRow.createCell(1).setCellValue(description);
            }

            // ��� ����
            workbook.write(fos);
            System.out.println("ó���� �Ϸ�Ǿ����ϴ�. ����� " + outputFilePath + "�� ����Ǿ����ϴ�.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String searchNaverNews(String query, String clientId, String clientSecret) {
        String apiURL = "https://openapi.naver.com/v1/search/news.json?query=";

        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            apiURL += encodedQuery;

            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("X-Naver-Client-Id", clientId);
            requestHeaders.put("X-Naver-Client-Secret", clientSecret);

            return get(apiURL, requestHeaders);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("�˻��� ���ڵ� ����", e);
        }
    }

    private static String get(String apiUrl, Map<String, String> requestHeaders) {
        HttpURLConnection con = connect(apiUrl);
        try {
            con.setRequestMethod("GET");
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // ���� ȣ��
                return parseDescription(readBody(con.getInputStream()));
            } else { // ���� �߻�
                return "No matching news article found.";
            }
        } catch (IOException e) {
            throw new RuntimeException("API ��û�� ���� ����", e);
        } finally {
            con.disconnect();
        }
    }

    private static HttpURLConnection connect(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException("���� ����: " + apiUrl, e);
        }
    }

    private static String readBody(InputStream body) {
        InputStreamReader streamReader = new InputStreamReader(body);

        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();

            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }

            return responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("API ���� �б� ����", e);
        }
    }

    private static String parseDescription(String jsonResponse) {
        // ��Ȯ�� ������ ù ��° item�� description ����
        int itemStartIndex = jsonResponse.indexOf("\"items\":");
        if (itemStartIndex != -1) {
            int descriptionIndex = jsonResponse.indexOf("\"description\":", itemStartIndex);
            if (descriptionIndex != -1) {
                int start = jsonResponse.indexOf('"', descriptionIndex + 14) + 1;
                int end = jsonResponse.indexOf('"', start);
                return jsonResponse.substring(start, end).replaceAll("<.*?>", ""); // HTML �±� ����
            }

        }
        return "No matching news article found.";
    }
}
