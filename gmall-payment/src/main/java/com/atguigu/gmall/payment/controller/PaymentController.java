package com.atguigu.gmall.payment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PaymentController {

    @GetMapping("pay.html/")
    public String toPay(@RequestParam("orderToken")String orderToken){

        return "pay";
    }
}
