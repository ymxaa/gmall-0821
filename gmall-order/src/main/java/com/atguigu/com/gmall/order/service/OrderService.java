package com.atguigu.com.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.com.gmall.order.feign.*;
import com.atguigu.com.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.com.gmall.order.pojo.OrderConfirmVo;
import com.atguigu.com.gmall.order.pojo.UserInfo;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.co.SkuLockVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "order:token:";

    public OrderConfirmVo confirm() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        //TODO: 异步编排改造远程调用

        //获取登录用户的id
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        //判断用户是否有选中购物车商品
        List<Cart> carts = cartClient.queryCheckedCarts(userId).getData();
        if(CollectionUtils.isEmpty(carts)){
            throw new OrderException("没有选中购物车记录");
        }

        //根据用户的id查询地址列表
        List<UserAddressEntity> addressEntities = umsClient.queryUserAddressByUserId(userId).getData();
        confirmVo.setAddresses(addressEntities);
        //设置订单商品属性
        List<OrderItemVo> orderItemVos = carts.stream().map(cart -> {
            OrderItemVo orderItemVo = new OrderItemVo();
            orderItemVo.setSkuId(cart.getSkuId());
            orderItemVo.setCount(cart.getCount());
            //查询sku的信息
            SkuEntity skuEntity = pmsClient.querySkuById(cart.getSkuId()).getData();
            orderItemVo.setTitle(skuEntity.getTitle());
            orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
            orderItemVo.setPrice(skuEntity.getPrice());
            orderItemVo.setWeight(skuEntity.getWeight());
            //查询销售属性
            List<SkuAttrValueEntity> skuAttrValueEntities = pmsClient.querySaleAttrValuesBySkuId(cart.getSkuId()).getData();
            orderItemVo.setSaleAttrs(skuAttrValueEntities);
            //查询营销信息
            List<ItemSaleVo> itemSaleVos = smsClient.querySalesBySkuId(cart.getSkuId()).getData();
            orderItemVo.setSales(itemSaleVos);
            //查询库存信息
            List<WareSkuEntity> wareSkuEntities = wmsClient.queryWareSkuBySkuId(cart.getSkuId()).getData();
            orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked()>0));

            return orderItemVo;
        }).collect(Collectors.toList());
        confirmVo.setOrderItems(orderItemVos);

        //设置用户的积分登录信息
        UserEntity userEntity = umsClient.queryUserById(userId).getData();
        confirmVo.setBounds(userEntity.getIntegration());

        //为了防重 生成唯一标识，保存到redis中一份
        String orderToken = IdWorker.getTimeId();
        redisTemplate.opsForValue().set(KEY_PREFIX+orderToken,orderToken,12, TimeUnit.HOURS);
        confirmVo.setOrderToken(orderToken);

        return confirmVo;
    }

    public void submit(OrderSubmitVo orderSubmitVo) {
        //1.防重：redis
        String orderToken = orderSubmitVo.getOrderToken();
        if(StringUtils.isBlank(orderToken)){
            throw new OrderException("非法提交！");
        }
        String script = "if (redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
        Boolean flag = redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX+orderToken), orderToken);
        if(!flag){
            throw new OrderException("请不要重复提交！");
        }
        //2.验证总价
        BigDecimal totalPrice = orderSubmitVo.getTotalPrice();//页面提交订单时的总价格
        List<OrderItemVo> items = orderSubmitVo.getItems();
        if(CollectionUtils.isEmpty(items)){
            throw new OrderException("你没有要购买的商品！");
        }
        BigDecimal currentTotalPrice = items.stream().map(item -> {
            SkuEntity skuEntity = pmsClient.querySkuById(item.getSkuId()).getData();
            if (skuEntity == null) {
                return new BigDecimal(0);
            }
            return skuEntity.getPrice().multiply(item.getCount());
        }).reduce((a, b) -> a.add(b)).get();
        if(currentTotalPrice.compareTo(totalPrice) != 0){
            throw new OrderException("页面已过期，请刷新后重试");
        }
        //3.验证库存并锁定库存
        List<SkuLockVo> lockVos = items.stream().map(item -> {
            SkuLockVo lockVo = new SkuLockVo();
            lockVo.setSkuId(item.getSkuId());
            lockVo.setCount(item.getCount().intValue());
            return lockVo;
        }).collect(Collectors.toList());
        List<SkuLockVo> skuLockVos = wmsClient.checkAndLock(lockVos, orderToken).getData();
        if(!CollectionUtils.isEmpty(skuLockVos)){
            throw new OrderException(JSON.toJSONString(skuLockVos));
        }

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        //4.下单
        try {
            this.omsClient.saveOrder(userId,orderSubmitVo);
            //如果订单创建成功，发送消息定时关单
            rabbitTemplate.convertAndSend("ORDER_EXCHANGE","order.close",orderToken);
        } catch (Exception e) {
            e.printStackTrace();
            //如果出现异常就解锁库存，标记订单是无效订单
            rabbitTemplate.convertAndSend("ORDER_EXCHANGE","order.disable",orderToken);
            throw new OrderException("创建订单时服务异常!");
        }

        //5.异步删除购物车中对应记录
        Map<String,Object> map = new HashMap<>();
        map.put("userId",userId);
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        map.put("skuIds",JSON.toJSONString(skuIds));
        rabbitTemplate.convertAndSend("ORDER_EXCHANGE","cart.delete",map);

    }
}
