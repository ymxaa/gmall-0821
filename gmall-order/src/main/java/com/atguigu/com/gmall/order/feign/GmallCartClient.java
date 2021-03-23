package com.atguigu.com.gmall.order.feign;

import com.atguigu.gmall.cart.api.GamllCartApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("cart-service")
public interface GmallCartClient extends GamllCartApi {

}
