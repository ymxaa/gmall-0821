package com.atguigu.gmall.wms;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class GmallWmsApplicationTests {

    @Resource
    private WareSkuMapper wareSkuMapper;
    @Test
    void contextLoads() {
        wareSkuMapper.unlock(1l,3);
    }

}
