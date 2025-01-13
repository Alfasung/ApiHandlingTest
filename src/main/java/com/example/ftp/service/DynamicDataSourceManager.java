package com.example.ftp.service;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

// 동적으로 데이터 소스를 관리하는 컴포넌트 클래스
@Component
public class DynamicDataSourceManager {

    // 동적으로 생성된 데이터 소스
    private DataSource dataSource;

    /**
     * 데이터 소스를 동적으로 구성하는 메서드
     *
     * @param host     데이터베이스 호스트 주소
     * @param port     데이터베이스 포트 번호
     * @param sid      데이터베이스 SID (Oracle 전용)
     * @param username 데이터베이스 사용자 이름
     * @param password 데이터베이스 비밀번호
     */
    public void configureDataSource(String host, int port, String sid, String username, String password) {
        // JDBC URL을 생성
        String jdbcUrl = String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, sid);

        // DataSourceBuilder를 사용해 DataSource 생성
        this.dataSource = DataSourceBuilder.create()
                .driverClassName("oracle.jdbc.OracleDriver") // Oracle JDBC 드라이버 설정
                .url(jdbcUrl) // JDBC URL 설정
                .username(username) // 사용자 이름 설정
                .password(password) // 비밀번호 설정
                .build();

        // 데이터 소스 설정 완료 메시지 출력
        System.out.println("Dynamic DataSource configured: " + jdbcUrl);
    }

    /**
     * 현재 구성된 데이터 소스를 반환
     *
     * @return DataSource 객체
     * @throws IllegalStateException 데이터 소스가 설정되지 않은 경우 예외 발생
     */
    public DataSource getDataSource() {
        if (this.dataSource == null) {
            throw new IllegalStateException("DataSource has not been configured yet.");
        }
        return this.dataSource;
    }
}
