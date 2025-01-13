package com.example.ftp.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional(rollbackFor = Exception.class) // 트랜잭션 관리 추가
    public void handleXmlData(JSONObject xmlDataJson, String participantName) throws Exception {
        try {
            // Base64 디코딩
            String encodedXmlData = xmlDataJson.getString("XML_DATA");
            String decodedXmlData = decodeBase64(encodedXmlData);

            // XML 데이터 파싱 및 DB 삽입
            parseAndInsertXml(decodedXmlData, participantName);
        } catch (Exception e) {
            // 예외가 발생하면 트랜잭션을 롤백
            throw new RuntimeException("XML 데이터 처리 중 오류 발생: " + e.getMessage(), e);
        }
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
            throw new RuntimeException("Base64 XML 데이터를 디코딩하는 중 오류 발생: " + e.getMessage(), e);
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
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputSource inputSource = new InputSource(new StringReader(xmlData));
        inputSource.setEncoding("EUC-KR");
        Document document = builder.parse(inputSource);
        document.getDocumentElement().normalize();

        insertIntoDatabase(document, participantName);
    }

    /**
     * 파싱된 XML 데이터를 데이터베이스에 삽입
     *
     * @param document        XML Document 객체
     * @param participantName 참여자 이름
     * @throws Exception DB 삽입 중 오류 발생 시 예외 발생
     * ORDER_DATE, ETA_DATE가 DATE 타입이 아닌 문자열로 저장되어 있음 (TO_DATE 함수 사용할 필요 없음)
     */
    private void insertIntoDatabase(Document document, String participantName) throws Exception {
        DataSource dataSource = dataSourceManager.getDataSource();

        try (Connection connection = dataSource.getConnection()) {
            String insertQuery = "INSERT INTO INSPIEN_XMLDATA_INFO " +
                    "(ORDER_NUM, ITEM_SEQ, ORDER_ID, ORDER_DATE, ORDER_PRICE, ORDER_QTY, RECEIVER_NAME, RECEIVER_NO, ETA_DATE, DESTINATION, DESCIPTION, ITEM_NAME, ITEM_QTY, ITEM_COLOR, ITEM_PRICE, SENDER, CURRENT_DT) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                NodeList headers = document.getElementsByTagName("HEADER");
                NodeList details = document.getElementsByTagName("DETAIL");

                for (int i = 0; i < headers.getLength(); i++) {
                    Element header = (Element) headers.item(i);

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

                    for (int j = 0; j < details.getLength(); j++) {
                        Element detail = (Element) details.item(j);
                        if (getTextContent(detail, "ORDER_NUM").equals(orderNum)) {
                            String itemSeq = getTextContent(detail, "ITEM_SEQ");
                            String itemName = getTextContent(detail, "ITEM_NAME");
                            String itemQty = getTextContent(detail, "ITEM_QTY");
                            String itemColor = getTextContent(detail, "ITEM_COLOR");
                            String itemPrice = getTextContent(detail, "ITEM_PRICE");

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
                            statement.setString(16, participantName);

                            statement.addBatch();
                        }
                    }
                }

                int[] results = statement.executeBatch();
                System.out.println("삽입된 행 수: " + results.length);
            }
        } catch (Exception e) {
            // 데이터베이스 작업 중 오류 발생 시 예외 처리
            throw new RuntimeException("데이터베이스 삽입 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private String getTextContent(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }
}
