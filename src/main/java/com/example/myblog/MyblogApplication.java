package com.example.myblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@EnableJpaAuditing
@SpringBootApplication
public class MyblogApplication {

    public static void main(String[] args) {

        try {
            // JWT HS256 서명용 안전한 256비트 비밀키 생성 (자바 표준 라이브러리 사용)
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            String secret = Base64.getEncoder().encodeToString(secretKey.getEncoded());

            System.out.println("==================================================");
            System.out.println("Generated JWT Secret Key: " + secret);
            System.out.println("==================================================");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("키 생성 중 오류 발생: " + e.getMessage());
        }

        SpringApplication.run(MyblogApplication.class, args);
    }

}
