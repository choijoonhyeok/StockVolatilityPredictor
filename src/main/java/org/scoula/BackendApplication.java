package org.scoula;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

//TIP 코드를 <b>실행</b>하려면 <shortcut actionId="Run"/>을(를) 누르거나
// 에디터 여백에 있는 <icon src="AllIcons.Actions.Execute"/> 아이콘을 클릭하세요.
@SpringBootApplication
@MapperScan("org.scoula.**.mapper")
@EnableScheduling
public class BackendApplication {
    public static void main(String[] args) {
      //  SpringApplication.run(BackendApplication.class, args);
    }
}