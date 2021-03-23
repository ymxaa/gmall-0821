package com.atguigu.gmall.oms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class OrderListener {

    @Resource
    private OrderMapper orderMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "OMS_DISABLE_QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"order.disable"}
    ))
    public void disableOrder(String orderToken, Channel channel, Message message) throws IOException {

        if(StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }

        // 更新订单的状态为无效订单
        orderMapper.updateStatus(orderToken, 0, 5);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
    @RabbitListener(queues = "ORDER_DEAD_QUEUE")
    public void closeOrder(String orderToken, Channel channel, Message message) throws IOException {

        if(StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }

        // 更新订单的状态为关闭状态，关单成功的情况下需要给wms发送消息解锁库存
        if(orderMapper.updateStatus(orderToken,0,4) == 1){
            rabbitTemplate.convertAndSend("ORDER_EXCHANGE","order.disable",orderToken);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
}
