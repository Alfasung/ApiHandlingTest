서비스(REST API) 연계 및 데이터 변환 처리
서비스 클라이언트 개발

**URL로 이름, 전화번호, 이메일 POST시


OUTPUT : 아래는 오브젝트 컬럼명
 - XML_DATA : base64 encoded EUC-KR XML String
 - JSON_DATA : base64 encoded UTF-8 JSON String
 - DB_CONN_INFO : DBMS 연결정보
 - FTP_CONN_INFO : FTP서버 연결정보

*XML_DATA를 핸들링 후 ORACLE DBMS에 “INSERT”
 - Header/Detail 구조를 join된 Single Table 구조로 변환

*JSON_DATA를 핸들링 후 FTP서버에 업로드
 - JSON 형식을 Flat(parameter-delimiter) 파일형태로 변환




오브젝트 필드정보(서비스)

Source DATA

          NAME
          PHONE_NUMBER
          E_MAIL

Response DATA

     "XML_DATA":{**}
     
     "JSON_DATA":{**}
     
     
     "DB_CONN_INFO": {
        "HOST": "*",
        "PORT": **,
        "SID": "**",
        "USER": "**",
        "PASSWORD": "**",
        "TABLENAME": "INSPIEN_XMLDATA_INFO"
     },
     "FTP_CONN_INFO": {
        "HOST": "**",
        "PORT": **,
        "USER": "**",
        "PASSWORD": "**",
        "FILE_PATH": "/"
     }




XML (DBMS)

Source DATA
 
     <PurchaseOrder><HEADER><ORDER_NUM>1000</ORDER_NUM><ORDER_ID>kwang001</ORDER_ID><ORDER_DATE>2021-08-26</ORDER_DATE><ORDER_PRICE>62000</ORDER_PRICE><ORDER_QTY>3</ORDER_QTY><RECEIVER_NAME>김광수</RECEIVER_NAME><RECEIVER_NO>010-5312-2345</RECEIVER_NO><ETA_DATE>2021-09-01</ETA_DATE><DESTINATION>서울시 서초구 방배동 801</DESTINATION><DESCIPTION>현관앞에 놓아주세요</DESCIPTION></HEADER>

Target DATA

     “INSERT” - Header/Detail 구조를 join된 Single Table 구조로 변환




JSON(FTP)

Source DATA

       "record": [
       		{
       			"Names": "Baxter Chang Özbey",
       			"Phone": "076 2957 1961",
       			"Email": "id.enim.Curabitur@Crasdictum.com",
       			"BirthDate": "1981/09/14",
       			"Company": "Sem Institute",
       			"PersonalNumber": "16550126 7313",
       			"OrganisationNumber": "978436-9705",
       			"Country": "Kenya",
       			"Region": "South Sumatra",
       			"City": "Palembang",
       			"Street": "9980 Lacus. Avenue",
       			"ZipCode": 86867,
       			"CreditCard": 4539184335316,
       			"GUID": "5DD3E3BF-A039-B909-326B-460396CD5CF6"
       		},

Target DATA
-parameter-delimiter : “^”
line separator : “\n”
필드 값 순서 유지
                                                        
       Baxter Chang Özbey^076 2957 1961^id.enim.Curabitur@Crasdictum.com^1981/09/14^Sem Institute^16550126 7313^978436-9705^Kenya^South Sumatra^Palembang^9980 Lacus. Avenue^86867^4539184335316^5DD3E3BF-A039-B909-326B-460396CD5CF6

