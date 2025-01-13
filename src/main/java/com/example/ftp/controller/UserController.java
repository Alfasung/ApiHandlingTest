package com.example.ftp.controller;

import com.example.ftp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// RESTful 컨트롤러 클래스, "/api/v1" 경로로 API 엔드포인트 제공
@RestController
@RequestMapping("/api/v1")
public class UserController {

    // UserService 주입
    @Autowired
    private UserService userService;

    /**
     * 사용자 정보를 처리하는 API 엔드포인트
     *
     * @param userInfo 사용자 정보가 포함된 JSON 객체 (이름, 전화번호, 이메일)
     * @return 처리 상태 메시지
     */
    @PostMapping("/process")
    public String processUser(@RequestBody Map<String, String> userInfo) {
        try {
            // UserService를 통해 사용자 정보 처리
            userService.processRequest(userInfo);
            return "Processing completed successfully.";
        } catch (Exception e) {
            // 예외 발생 시 스택 트레이스 출력 및 에러 메시지 반환
            e.printStackTrace();
            return "An error occurred: " + e.getMessage();
        }
    }
}
