package com.atguigu.com.gmall.order.pojo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {

    //收件人地址列表
    private List<UserAddressEntity> addresses;
    //送货清单
    private List<OrderItemVo> orderItems;
    //购买积分
    private Integer bounds;
    //防止订单重复提交
    private String orderToken;

}
