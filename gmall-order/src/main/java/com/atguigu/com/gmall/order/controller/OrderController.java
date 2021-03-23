package com.atguigu.com.gmall.order.controller;

import com.atguigu.com.gmall.order.pojo.OrderConfirmVo;
import com.atguigu.com.gmall.order.service.OrderService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("confirm")
    public String confirm(Model model){
        OrderConfirmVo confirmVo = orderService.confirm();
        System.out.println("eeeeeeeeeeee"+confirmVo);
        model.addAttribute("confirmVo",confirmVo);
        return "trade";
    }
    @PostMapping("submit")
    @ResponseBody
    public ResponseVo<String> submit(@RequestBody OrderSubmitVo orderSubmitVo){
        orderService.submit(orderSubmitVo);
        return ResponseVo.ok(orderSubmitVo.getOrderToken());
    }

}
