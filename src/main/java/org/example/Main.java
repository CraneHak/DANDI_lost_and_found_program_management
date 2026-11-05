package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.example.auth.FirebaseProperties;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties(FirebaseProperties.class)
public class Main {
    public static void main(String[] args) {
        // 서버가 만드는 시각은 UTC로 저장한다 (EC2·RDS와 같음). 팀원 PC(한국 시간)에서 띄워도 같은 기준이 되게 고정.
        // 화면에는 ApiTime.utc()가 붙인 Z를 보고 브라우저가 한국 시간으로 바꿔 보여준다.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(Main.class, args);
    }
}
