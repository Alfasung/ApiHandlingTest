package com.example.ftp.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

// JSON 데이터를 처리하고 FTP 서버에 업로드하는 서비스 클래스
@Service
public class JsonToFtpHandler {

    /**
     * JSON 데이터를 처리하여 FTP에 업로드하는 메서드
     *
     * @param jsonDataJson    JSON 데이터가 포함된 객체
     * @param ftpConnInfo     FTP 연결 정보가 포함된 객체
     * @param participantName 참여자 이름
     * @throws Exception JSON 처리 또는 FTP 업로드 중 오류 발생
     */
    public void handleJsonData(JSONObject jsonDataJson, JSONObject ftpConnInfo, String participantName) throws Exception {
        // JSON_DATA 키가 존재하는지 확인
        if (!jsonDataJson.has("JSON_DATA")) {
            throw new RuntimeException("JSON_DATA not found in the JSON object.");
        }

        // Base64로 인코딩된 JSON 데이터 디코딩
        String encodedJsonData = jsonDataJson.getString("JSON_DATA");
        String jsonData = decodeBase64(encodedJsonData);

        // JSON 데이터에서 배열 추출
        JSONArray jsonArray = extractJsonArray(jsonData);

        // JSON 데이터를 Flat 파일 형태로 변환
        String flatData = convertJsonToFlat(jsonArray);

        // 변환된 데이터를 출력
        System.out.println("Generated Flat Data:\n" + flatData);

        // 파일명 생성 (INSPIEN_JSON_참여자_타임스탬프 형식)
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = String.format("INSPIEN_JSON_[%s]_[%s].txt", participantName, timestamp);

        // FTP 업로드 URL 생성
        String ftpUrl = createFtpUrl(ftpConnInfo, fileName);

        // FTP로 업로드
        uploadToFTP(ftpUrl, flatData);
    }

    /**
     * Base64로 인코딩된 문자열을 디코딩
     *
     * @param encodedJsonData Base64로 인코딩된 데이터
     * @return 디코딩된 문자열
     */
    private String decodeBase64(String encodedJsonData) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedJsonData);
            return new String(decodedBytes, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Error decoding Base64 JSON data: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 문자열에서 배열 데이터를 추출
     *
     * @param jsonData JSON 형식의 데이터
     * @return JSONArray 객체
     */
    private JSONArray extractJsonArray(String jsonData) {
        if (jsonData.trim().startsWith("[")) {
            return new JSONArray(jsonData); // JSON 배열
        } else if (jsonData.trim().startsWith("{")) {
            JSONObject jsonObject = new JSONObject(jsonData);
            if (jsonObject.has("record")) {
                return jsonObject.getJSONArray("record"); // 객체에서 배열 추출
            } else {
                throw new RuntimeException("JSON does not contain a valid array: " + jsonData);
            }
        } else {
            throw new RuntimeException("Invalid JSON format: " + jsonData);
        }
    }

    /**
     * JSON 배열을 Flat 파일 형식으로 변환
     *
     * @param jsonArray JSON 배열
     * @return 변환된 Flat 데이터
     */
    private String convertJsonToFlat(JSONArray jsonArray) {
        StringBuilder flatData = new StringBuilder();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);

            // 각 필드를 ^ 구분자로 연결
            flatData.append(obj.optString("Names", "N/A")).append("^")
                    .append(obj.optString("Phone", "N/A")).append("^")
                    .append(obj.optString("Email", "N/A")).append("^")
                    .append(obj.optString("BirthDate", "N/A")).append("^")
                    .append(obj.optString("Company", "N/A")).append("^")
                    .append(obj.optString("ZipCode", "N/A")).append("\n");
        }

        return flatData.toString();
    }

    /**
     * FTP 업로드 URL을 생성
     *
     * @param ftpConnInfo FTP 연결 정보
     * @param fileName    업로드할 파일 이름
     * @return 생성된 FTP URL
     */
    private String createFtpUrl(JSONObject ftpConnInfo, String fileName) {
        try {
            return String.format("ftp://%s:%s@%s:%d%s/%s",
                    ftpConnInfo.getString("USER"),
                    ftpConnInfo.getString("PASSWORD"),
                    ftpConnInfo.getString("HOST"),
                    ftpConnInfo.getInt("PORT"),
                    ftpConnInfo.getString("FILE_PATH"),
                    fileName);
        } catch (Exception e) {
            throw new RuntimeException("Error creating FTP URL: " + e.getMessage(), e);
        }
    }

    /**
     * 변환된 데이터를 FTP 서버에 업로드
     *
     * @param ftpUrl  FTP 업로드 URL
     * @param flatData 업로드할 Flat 데이터
     * @throws Exception 업로드 중 오류 발생
     */
    private void uploadToFTP(String ftpUrl, String flatData) throws Exception {
        System.out.println("Uploading Flat Data to FTP:\n" + flatData);

        // FTP 연결 설정
        URL url = new URL(ftpUrl);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(flatData.getBytes("UTF-8")); // 데이터를 업로드
            os.flush();
        }

        System.out.println("File uploaded successfully to: " + ftpUrl);
    }
}
