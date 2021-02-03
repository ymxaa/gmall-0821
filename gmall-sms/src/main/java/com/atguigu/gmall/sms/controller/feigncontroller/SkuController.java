package com.atguigu.gmall.sms.controller.feigncontroller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("sms/sku")
@RestController
public class SkuController {

    @Autowired
    SkuBoundsService skuBoundsService;

    @PostMapping("save")
    public ResponseVo saveSales(@RequestBody SkuSaleVo skuSaleVo) {

        skuBoundsService.saveSales(skuSaleVo);

        return ResponseVo.ok();
    }
}
