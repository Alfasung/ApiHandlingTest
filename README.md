서비스(SOAP/REST API) 연계 및 데이터 변환 처리
서비스 클라이언트 개발

**URL로 이름, 전화번호, 이메일 입력시


     OUTPUT : 아래는 오브젝트 컬럼명
 - XML_DATA : base64 encoded EUC-KR XML String
 - JSON_DATA : base64 encoded UTF-8 JSON String
 - DB_CONN_INFO : DBMS 연결정보
 - FTP_CONN_INFO : FTP서버 연결정보

*XML_DATA를 핸들링 후 ORACLE DBMS에 “INSERT”
 - Header/Detail 구조를 join된 Single Table 구조로 변환
*
JSON_DATA를 핸들링 후 FTP서버에 업로드
 - JSON 형식을 Flat(parameter-delimiter) 파일형태로 변환




