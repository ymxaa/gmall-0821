package com.atguigu.com.gmall.order.feign;

import com.atguigu.gmall.cart.api.GamllCartApi;
import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {

}
