package com.example.ftp.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

/**
 * JSON 데이터를 처리하고 FTP 서버에 업로드하는 서비스 클래스.
 */
@Service
public class JsonToFtpHandler {

    /**
     * JSON 데이터를 처리하여 FTP 서버에 업로드하는 메서드.
     *
     * @param jsonDataJson    JSON 데이터가 포함된 객체 (Base64로 인코딩된 JSON 포함).
     * @param ftpConnInfo     FTP 연결 정보가 포함된 객체.
     * @param participantName 참여자 이름 (파일 이름에 사용).
     * @throws Exception 처리 및 업로드 중 오류가 발생한 경우 예외를 던짐.
     */
    public void handleJsonData(JSONObject jsonDataJson, JSONObject ftpConnInfo, String participantName) throws Exception {
        // JSON_DATA 키가 존재하는지 확인
        if (!jsonDataJson.has("JSON_DATA")) {
            throw new RuntimeException("JSON_DATA가 JSON 객체에 없습니다.");
        }

        // Base64로 인코딩된 JSON 데이터를 디코딩
        String encodedJsonData = jsonDataJson.getString("JSON_DATA");
        String jsonData = decodeBase64(encodedJsonData);

        // JSON 데이터에서 배열 형식 추출
        JSONArray jsonArray = extractJsonArray(jsonData);

        // JSON 데이터를 Flat 파일 형식으로 변환
        String flatData = convertJsonToFlat(jsonArray);

        // 변환된 데이터를 출력 (디버깅 용도)
        System.out.println("생성된 Flat 데이터:\n" + flatData);

        // 파일 이름 생성 (참여자 이름 및 타임스탬프 포함)
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = String.format("INSPIEN_JSON_[%s]_[%s].txt", participantName, timestamp);

        // FTP 업로드 URL 생성
        String ftpUrl = createFtpUrl(ftpConnInfo, fileName);

        // FTP로 업로드 시도
        try {
            uploadToFTP(ftpUrl, flatData);
        } catch (IOException e) {
            // 업로드 중 오류가 발생한 경우 예외 처리
            throw new RuntimeException("FTP 업로드 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * Base64로 인코딩된 문자열을 디코딩.
     *
     * @param encodedJsonData Base64로 인코딩된 JSON 데이터.
     * @return 디코딩된 JSON 문자열.
     */
    private String decodeBase64(String encodedJsonData) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedJsonData);
            return new String(decodedBytes, "UTF-8");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Base64 디코딩이 잘못되었습니다: " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Base64 데이터에 대한 지원되지 않는 인코딩: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 문자열에서 배열을 추출.
     *
     * @param jsonData JSON 형식의 문자열 데이터.
     * @return 추출된 JSONArray 객체.
     */
    private JSONArray extractJsonArray(String jsonData) {
        if (jsonData.trim().startsWith("[")) {
            return new JSONArray(jsonData); // JSON 배열 형식
        } else if (jsonData.trim().startsWith("{")) {
            JSONObject jsonObject = new JSONObject(jsonData);
            if (jsonObject.has("record")) {
                return jsonObject.getJSONArray("record"); // record 키에서 배열 추출
            } else {
                throw new RuntimeException("JSON에 유효한 배열이 포함되어 있지 않습니다: " + jsonData);
            }
        } else {
            throw new RuntimeException("JSON 형식이 잘못되었습니다: " + jsonData);
        }
    }

    /**
     * JSON 배열을 Flat 파일 형식으로 변환.
     *
     * @param jsonArray JSON 배열.
     * @return 변환된 Flat 데이터 문자열.
     */
    private String convertJsonToFlat(JSONArray jsonArray) {
        StringBuilder flatData = new StringBuilder();

        // 필드 순서를 명시적으로 정의
        String[] fieldOrder = {
                "Names", "Phone", "Email", "BirthDate", "Company",
                "PersonalNumber", "OrganisationNumber", "Country",
                "Region", "City", "Street", "ZipCode", "CreditCard", "GUID"
        };

        // 각 JSON 객체를 Flat 파일 형식으로 변환
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            for (int j = 0; j < fieldOrder.length; j++) {
                String key = fieldOrder[j];
                flatData.append(obj.optString(key, "N/A")); // 키가 없으면 "N/A"로 대체
                if (j < fieldOrder.length - 1) {
                    flatData.append("^");
                }
            }
            flatData.append("\n");
        }

        return flatData.toString();
    }

    /**
     * FTP 업로드 URL을 생성.
     *
     * @param ftpConnInfo FTP 연결 정보.
     * @param fileName    업로드할 파일 이름.
     * @return 생성된 FTP URL 문자열.
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
            throw new RuntimeException("FTP URL 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 변환된 데이터를 FTP 서버에 업로드.
     *
     * @param ftpUrl  FTP 업로드 URL.
     * @param flatData 업로드할 Flat 데이터.
     * @throws IOException FTP 업로드 중 오류가 발생한 경우 예외 발생.
     */
    private void uploadToFTP(String ftpUrl, String flatData) throws IOException {
        System.out.println("FTP로 업로드 중:\n" + flatData);

        // FTP 연결 설정
        URL url = new URL(ftpUrl);
        URLConnection connection = null;

        try {
            connection = url.openConnection();
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(flatData.getBytes("UTF-8")); // 데이터를 업로드
                os.flush();
            }
            System.out.println("FTP 업로드 성공: " + ftpUrl);
        } catch (FileNotFoundException e) {
            throw new IOException("FTP 서버에서 파일을 찾을 수 없음: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IOException("FTP 업로드 중 오류 발생: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("FTP 업로드 중 예상치 못한 오류 발생: " + e.getMessage(), e);
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
    }
}
