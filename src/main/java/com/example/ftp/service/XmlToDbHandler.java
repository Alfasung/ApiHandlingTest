package com.example.ftp.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Base64;
import java.sql.Connection;
import java.sql.PreparedStatement;

// XML 데이터를 처리하고 데이터베이스에 삽입하는 서비스 클래스
@Service
public class XmlToDbHandler {

    private final DynamicDataSourceManager dataSourceManager;

    // DynamicDataSourceManager를 의존성 주입받아 초기화
    public XmlToDbHandler(DynamicDataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    /**
     * Base64로 인코딩된 XML 데이터를 디코딩하고 DB에 삽입 처리
     *
     * @param xmlDataJson    Base64로 인코딩된 XML 데이터가 포함된 JSON 객체
     * @param participantName 참여자 이름
     * @throws Exception 데이터 처리 중 오류 발생 시 예외 발생
     */
    public void handleXmlData(JSONObject xmlDataJson, String participantName) throws Exception {
        // Base64 디코딩
        String encodedXmlData = xmlDataJson.getString("XML_DATA");
        String decodedXmlData = decodeBase64(encodedXmlData);

        // XML 데이터 파싱 및 DB 삽입
        parseAndInsertXml(decodedXmlData, participantName);
    }

    /**
     * Base64 문자열 디코딩
     *
     * @param base64EncodedXml Base64로 인코딩된 XML 문자열
     * @return 디코딩된 XML 문자열
     */
    private String decodeBase64(String base64EncodedXml) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedXml);
            return new String(decodedBytes, "EUC-KR");
        } catch (Exception e) {
            throw new RuntimeException("Error decoding Base64 XML data: " + e.getMessage(), e);
        }
    }

    /**
     * XML 데이터를 파싱하고 데이터베이스에 삽입
     *
     * @param xmlData         XML 데이터
     * @param participantName 참여자 이름
     * @throws Exception 파싱 또는 DB 작업 중 오류 발생 시 예외 발생
     */
    private void parseAndInsertXml(String xmlData, String participantName) throws Exception {
        // XML 파싱 설정
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // XML 데이터를 Document 객체로 변환
        InputSource inputSource = new InputSource(new StringReader(xmlData));
        inputSource.setEncoding("EUC-KR");
        Document document = builder.parse(inputSource);
        document.getDocumentElement().normalize();

        // 데이터베이스에 삽입
        insertIntoDatabase(document, participantName);
    }

    /**
     * 파싱된 XML 데이터를 데이터베이스에 삽입
     *
     * @param document        XML Document 객체
     * @param participantName 참여자 이름
     * @throws Exception DB 삽입 중 오류 발생 시 예외 발생
     */
    private void insertIntoDatabase(Document document, String participantName) throws Exception {
        // 동적 데이터 소스 가져오기
        DataSource dataSource = dataSourceManager.getDataSource();

        try (Connection connection = dataSource.getConnection()) {
            // SQL INSERT 쿼리
            String insertQuery = "INSERT INTO INSPIEN_XMLDATA_INFO " +
                    "(ORDER_NUM, ITEM_SEQ, ORDER_ID, ORDER_DATE, ORDER_PRICE, ORDER_QTY, RECEIVER_NAME, RECEIVER_NO, ETA_DATE, DESTINATION, DESCIPTION, ITEM_NAME, ITEM_QTY, ITEM_COLOR, ITEM_PRICE, SENDER, CURRENT_DT) " +
                    "VALUES (?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?, ?, ?, ?, ?, SYSDATE)";
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                // XML 데이터에서 HEADER와 DETAIL 태그 가져오기
                NodeList headers = document.getElementsByTagName("HEADER");
                NodeList details = document.getElementsByTagName("DETAIL");

                // 각 HEADER와 DETAIL 조합으로 데이터 삽입
                for (int i = 0; i < headers.getLength(); i++) {
                    Element header = (Element) headers.item(i);

                    // HEADER 태그의 값 추출
                    String orderNum = getTextContent(header, "ORDER_NUM");
                    String orderId = getTextContent(header, "ORDER_ID");
                    String orderDate = getTextContent(header, "ORDER_DATE");
                    String orderPrice = getTextContent(header, "ORDER_PRICE");
                    String orderQty = getTextContent(header, "ORDER_QTY");
                    String receiverName = getTextContent(header, "RECEIVER_NAME");
                    String receiverNo = getTextContent(header, "RECEIVER_NO");
                    String etaDate = getTextContent(header, "ETA_DATE");
                    String destination = getTextContent(header, "DESTINATION");
                    String description = getTextContent(header, "DESCIPTION");

                    // DETAIL 태그의 값 추출 및 매칭
                    for (int j = 0; j < details.getLength(); j++) {
                        Element detail = (Element) details.item(j);
                        if (getTextContent(detail, "ORDER_NUM").equals(orderNum)) {
                            String itemSeq = getTextContent(detail, "ITEM_SEQ");
                            String itemName = getTextContent(detail, "ITEM_NAME");
                            String itemQty = getTextContent(detail, "ITEM_QTY");
                            String itemColor = getTextContent(detail, "ITEM_COLOR");
                            String itemPrice = getTextContent(detail, "ITEM_PRICE");

                            // SQL 파라미터 설정
                            statement.setString(1, orderNum);
                            statement.setString(2, itemSeq);
                            statement.setString(3, orderId);
                            statement.setString(4, orderDate);
                            statement.setDouble(5, Double.parseDouble(orderPrice));
                            statement.setInt(6, Integer.parseInt(orderQty));
                            statement.setString(7, receiverName);
                            statement.setString(8, receiverNo);
                            statement.setString(9, etaDate);
                            statement.setString(10, destination);
                            statement.setString(11, description);
                            statement.setString(12, itemName);
                            statement.setInt(13, Integer.parseInt(itemQty));
                            statement.setString(14, itemColor);
                            statement.setDouble(15, Double.parseDouble(itemPrice));
                            statement.setString(16, participantName); // 참여자 이름

                            statement.addBatch(); // Batch 처리
                        }
                    }
                }

                // Batch 실행
                int[] results = statement.executeBatch();
                System.out.println("Inserted rows: " + results.length);
            }
        }
    }

    /**
     * XML 태그에서 텍스트 콘텐츠를 추출
     *
     * @param element XML Element 객체
     * @param tagName 태그 이름
     * @return 태그의 텍스트 콘텐츠 (없을 경우 null 반환)
     */
    private String getTextContent(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }
}
