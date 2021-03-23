package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CartAsyncService {

    @Resource
    private CartMapper cartMapper;

    @Async
    public void updateCart(String userId, String skuId, Cart cart){

        cartMapper.update(cart,new UpdateWrapper<Cart>().eq("user_id",userId).eq("sku_id", skuId));
    }

    @Async
    public void insertCart(String userId, Cart cart){
        cartMapper.insert(cart);
    }

    @Async
    public void deleteCart(String userId,String userKey){
        cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userKey));
    }

    @Async
    public void deleteCartBySkuId(String userId, Long skuId) {
        cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
    }
}
