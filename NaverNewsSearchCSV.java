package CSStudy;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class NaverNewsSearchCSV {
    static int cnt=1;

    public static void main(String[] args) {
        String clientId = "OE2sHIk6MRnEbwBG8PuV"; // 네이버 클라이언트 ID
        String clientSecret = "clvSRTarvD"; // 네이버 클라이언트 시크릿
        String inputFilePath = "C:\\Users\\Public\\title.csv"; // 입력 CSV 파일 경로
        String outputFilePath = "C:\\Users\\Public\\news_java.csv"; // 출력 CSV 파일 경로

        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath))) {

            // 헤더 작성
            bw.write("title,content");
            bw.newLine();

            String line;
            while ((line = br.readLine()) != null) {
                // 첫 번째 행이 헤더인 경우 스킵
                if (line.equalsIgnoreCase("title")) continue;

                String title = line.trim();
                if (title.isEmpty()) continue;

                // 뉴스 검색
                String description = searchNaverNews(title, clientId, clientSecret);

                // 결과 쓰기
                bw.write("\"" + title + "\",\"" + description + "\"");
                bw.newLine();
            }

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
            System.out.println(cnt++);
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

