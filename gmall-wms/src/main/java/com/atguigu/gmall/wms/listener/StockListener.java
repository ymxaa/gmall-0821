package com.atguigu.gmall.wms.listener;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.co.SkuLockVo;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Component
public class StockListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private WareSkuMapper wareSkuMapper;

    private static final String KEY_PREFIX = "stock:info:";


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK_UNLOCK_QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"order.disable"}
    ))
    public void unlockStock(String orderToken, Channel channel, Message message) throws IOException {

        if(StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }

        //获取锁定库存的缓存信息
        String skuLockJson = redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if(StringUtils.isBlank(skuLockJson)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        //反序列化解锁库存
        List<SkuLockVo> skuLockVos = JSON.parseArray(skuLockJson, SkuLockVo.class);
        skuLockVos.forEach(skuLockVo -> {
            wareSkuMapper.unlock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
        });

        // 解锁库存以后，删除锁定库存的缓存，防止重复解锁库存
        redisTemplate.delete(KEY_PREFIX+orderToken);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
}
