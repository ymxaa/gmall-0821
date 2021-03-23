package com.atguigu.com.gmall.order.pojo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {

    private String orderToken; //防重
    private UserAddressEntity addressEntity; //收货地址
    private Integer payType;
    private String deliveryCompany;
    private Integer bounds;
    private List<OrderItemVo> items;
    private BigDecimal totalPrice; //验证总价
}
