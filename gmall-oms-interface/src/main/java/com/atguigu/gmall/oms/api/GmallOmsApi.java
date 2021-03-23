package com.atguigu.gmall.oms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.vo.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.web.bind.annotation.*;

public interface GmallOmsApi {

    @PostMapping("oms/order/save/{userId}")
    @ResponseBody
    public ResponseVo saveOrder(@PathVariable("userId")Long userId, @RequestBody OrderSubmitVo submitVo);


    @GetMapping("oms/order/orderToken/{orderToken}")
    @ResponseBody
    public ResponseVo<OrderEntity> queryOrderByToken(@PathVariable("orderToken")String orderToken);
}
