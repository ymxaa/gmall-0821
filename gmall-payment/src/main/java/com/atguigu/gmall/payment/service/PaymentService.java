package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.payment.feign.GmallOmsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    @Autowired
    private GmallOmsClient omsClient;
}
