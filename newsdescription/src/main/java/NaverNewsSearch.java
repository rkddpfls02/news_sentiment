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
        String clientId = "OE2sHIk6MRnEbwBG8PuV"; // 네이버 클라이언트 ID
        String clientSecret = "clvSRTarvD"; // 네이버 클라이언트 시크릿

        String inputFilePath = "C:\\Users\\Public\\title.xlsx"; // 입력 Excel 파일 경로
        String outputFilePath = "C:\\Users\\Public\\news_java.xlsx"; // 출력 Excel 파일 경로

        try (FileInputStream fis = new FileInputStream(inputFilePath);
             Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(outputFilePath)) {

            Sheet inputSheet = new XSSFWorkbook(fis).getSheetAt(0); // 입력 데이터 읽기
            Sheet outputSheet = workbook.createSheet("Results"); // 출력 데이터 쓰기

            // 헤더 작성
            Row headerRow = outputSheet.createRow(0);
            headerRow.createCell(0).setCellValue("title");
            headerRow.createCell(1).setCellValue("content");

            // 데이터 행 처리
            for (int i = 1; i <= inputSheet.getLastRowNum(); i++) {
                Row inputRow = inputSheet.getRow(i);
                if (inputRow == null) continue;

                Cell titleCell = inputRow.getCell(0); // title이 첫 번째 컬럼
                if (titleCell == null || titleCell.getCellType() != CellType.STRING) continue;

                String title = titleCell.getStringCellValue();
                String description = searchNaverNews(title, clientId, clientSecret);

                // 출력 데이터 작성
                Row outputRow = outputSheet.createRow(i);
                outputRow.createCell(0).setCellValue(title);
                outputRow.createCell(1).setCellValue(description);
            }

            // 결과 저장
            workbook.write(fos);
            System.out.println("처리가 완료되었습니다. 결과가 " + outputFilePath + "에 저장되었습니다.");

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
            throw new RuntimeException("검색어 인코딩 실패", e);
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
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                return parseDescription(readBody(con.getInputStream()));
            } else { // 오류 발생
                return "No matching news article found.";
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }

    private static HttpURLConnection connect(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException("연결 실패: " + apiUrl, e);
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
            throw new RuntimeException("API 응답 읽기 실패", e);
        }
    }

    private static String parseDescription(String jsonResponse) {
        // 정확도 순으로 첫 번째 item의 description 추출
        int itemStartIndex = jsonResponse.indexOf("\"items\":");
        if (itemStartIndex != -1) {
            int descriptionIndex = jsonResponse.indexOf("\"description\":", itemStartIndex);
            if (descriptionIndex != -1) {
                int start = jsonResponse.indexOf('"', descriptionIndex + 14) + 1;
                int end = jsonResponse.indexOf('"', start);
                return jsonResponse.substring(start, end).replaceAll("<.*?>", ""); // HTML 태그 제거
            }

        }
        return "No matching news article found.";
    }
}
