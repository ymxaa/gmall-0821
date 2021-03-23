package com.atguigu.gmall.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GmallGatewayApplicationTests {

    @Test
    void contextLoads() {
        String s1 = "hello";
        String s2 = "he"+new String("llo");
    }

}
