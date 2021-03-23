package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class JwtTest {


    // 别忘了创建rsa目录
    private static final String pubKeyPath = "E:\\guli-gmall\\rsa\\rsa.pub";
    private static final String priKeyPath = "E:\\guli-gmall\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MTM5ODM0MDJ9.VHLul-cng3CzJzM1Crc9FpAQSPDc0AgyQtW5mOWOkMBipdeigpG0KRDXx_994iXSRDvwydO7XhNmmjuitrN11BLJLCpiSpyN95QzkKBieOG9jXSP6nJbkWHjc57xYfQXbK6tg2A4Xu1BjWEbHEd1YwTjuwdNAlYjmy_LqsH7w-5IT2dNZSU6aATipIDtMdy7VVLmFnS2zEtGPih7-v5f20jKNbl4qhteS4AS3INaCIT41Lk0ifelWfWlt9VpiV_PwvUqJ2_uJxi1R81CgS9-457cF2HROQsSwiFuhz9A_9Lv_bKMAM9jlb2fTXMax9uU8mOrJNHv04_lHXrhKaNKJA";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }

}
