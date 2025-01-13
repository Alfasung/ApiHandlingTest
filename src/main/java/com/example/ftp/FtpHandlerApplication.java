package com.example.ftp;

import com.example.ftp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

// Spring Boot 애플리케이션의 진입점
@SpringBootApplication
public class FtpHandlerApplication implements CommandLineRunner {

    // UserService를 주입받아 사용
    @Autowired
    private UserService userService;

    // 애플리케이션의 main 메서드, SpringApplication.run()을 호출하여 애플리케이션 실행
    public static void main(String[] args) {
        SpringApplication.run(FtpHandlerApplication.class, args);
    }

    // CommandLineRunner 인터페이스의 run 메서드를 구현
    // 애플리케이션 실행 시 자동으로 실행됨
    @Override
    public void run(String... args) {
        // Scanner를 사용해 사용자 입력을 처리
        try (Scanner scanner = new Scanner(System.in)) {
            // 사용자로부터 이름 입력 받기
            System.out.println("이름을 입력하세요:");
            String name = scanner.nextLine();

            // 사용자로부터 전화번호 입력 받기
            System.out.println("전화번호를 입력하세요:");
            String phoneNumber = scanner.nextLine();

            // 사용자로부터 이메일 입력 받기
            System.out.println("이메일을 입력하세요:");
            String email = scanner.nextLine();

            // 입력받은 데이터를 Map에 저장
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("name", name); // 이름 저장
            userInfo.put("phone", phoneNumber); // 전화번호 저장
            userInfo.put("email", email); // 이메일 저장

            // UserService를 사용해 입력 데이터를 처리
            userService.processRequest(userInfo);

            // 처리 완료 메시지 출력
            System.out.println("프로그램 실행이 완료되었습니다.");
        } catch (Exception e) {
            // 예외 발생 시 스택 트레이스 출력 및 에러 메시지 표시
            e.printStackTrace();
            System.err.println("오류가 발생했습니다: " + e.getMessage());
        }
    }
}
