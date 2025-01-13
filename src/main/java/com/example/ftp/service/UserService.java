package com.example.ftp.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@Service
public class UserService {

    private final XmlToDbHandler xmlToDbHandler;
    private final JsonToFtpHandler jsonToFtpHandler;
    private final DynamicDataSourceManager dynamicDataSourceManager;

    public UserService(XmlToDbHandler xmlToDbHandler, JsonToFtpHandler jsonToFtpHandler, DynamicDataSourceManager dynamicDataSourceManager) {
        this.xmlToDbHandler = xmlToDbHandler;
        this.jsonToFtpHandler = jsonToFtpHandler;
        this.dynamicDataSourceManager = dynamicDataSourceManager;
    }

    /**
     * 사용자 요청을 처리하는 메서드
     *
     * @param userInfo 사용자 정보 (이름, 전화번호, 이메일) 포함된 Map
     * @throws Exception 처리 중 오류 발생 시 예외 발생
     */
    public void processRequest(Map<String, String> userInfo) throws Exception {
        try {
            // 사용자 입력 데이터를 JSON 형식으로 변환
            JSONObject requestJson = new JSONObject();
            requestJson.put("NAME", userInfo.get("name"));
            requestJson.put("PHONE_NUMBER", userInfo.get("phone"));
            requestJson.put("E-MAIL", userInfo.get("email"));

            // 디버깅용 요청 JSON 출력
            System.out.println("Request JSON: " + requestJson.toString());

            // 서버로 요청을 전송하고 응답 받기
            JSONObject responseJson = sendRequestToServer(requestJson);

            // 필드 검증 로직 추가
            validateResponseField(responseJson, "DB_CONN_INFO");
            validateResponseField(responseJson, "XML_DATA");
            validateResponseField(responseJson, "JSON_DATA");
            validateResponseField(responseJson, "FTP_CONN_INFO");

            // DB 연결 정보 설정
            JSONObject dbConnInfo = responseJson.getJSONObject("DB_CONN_INFO");
            dynamicDataSourceManager.configureDataSource(
                    dbConnInfo.getString("HOST"),
                    dbConnInfo.getInt("PORT"),
                    dbConnInfo.getString("SID"),
                    dbConnInfo.getString("USER"),
                    dbConnInfo.getString("PASSWORD")
            );

            // XML 데이터 처리 및 DB 삽입
            JSONObject xmlDataJson = new JSONObject();
            xmlDataJson.put("XML_DATA", responseJson.getString("XML_DATA"));
            xmlToDbHandler.handleXmlData(xmlDataJson, userInfo.get("name"));

            // JSON 데이터 처리 및 FTP 업로드
            JSONObject ftpConnInfo = responseJson.getJSONObject("FTP_CONN_INFO");
            JSONObject jsonDataJson = new JSONObject();
            jsonDataJson.put("JSON_DATA", responseJson.getString("JSON_DATA"));
            jsonToFtpHandler.handleJsonData(jsonDataJson, ftpConnInfo, userInfo.get("name"));

        } catch (Exception e) {
            // 예외 발생 시 스택 트레이스 출력 및 메시지 전달
            e.printStackTrace();
            throw new RuntimeException("An error occurred during processing: " + e.getMessage());
        }
    }

    /**
     * 서버에 JSON 요청을 보내고 응답을 처리하는 메서드
     *
     * @param requestJson 요청 JSON 데이터
     * @return 서버 응답을 JSONObject로 반환
     * @throws Exception 요청 또는 응답 처리 중 오류 발생 시 예외 발생
     */
    private JSONObject sendRequestToServer(JSONObject requestJson) throws Exception {
        String url = "http://211.106.171.36:50000/RESTAdapter/RecruitingTest"; // 서버 URL
        HttpURLConnection connection = null;
        try {
            // HTTP 연결 설정
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);

            // 요청 데이터 전송
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestJson.toString().getBytes("UTF-8"));
            }

            // 응답 코드 확인
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            if (responseCode != 200) {
                throw new RuntimeException("Failed to send request: HTTP error code " + responseCode);
            }

            // 응답 데이터 읽기
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                System.out.println("Server Response: " + response.toString());
                return new JSONObject(response.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error occurred while communicating with server: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 응답 JSON에서 필드 존재 여부를 검증
     *
     * @param responseJson 서버에서 반환된 JSON 객체
     * @param fieldName    검증할 필드 이름
     * @throws RuntimeException 필드가 누락된 경우 예외 발생
     */
    private void validateResponseField(JSONObject responseJson, String fieldName) {
        if (!responseJson.has(fieldName)) {
            throw new RuntimeException("Field '" + fieldName + "' is missing in the response JSON.");
        }
    }
}
