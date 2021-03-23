package com.atguigu.gmall.wms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;


@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){
        rabbitTemplate.setConfirmCallback((@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause) -> {
            if(!ack){
                log.error("消息没有到达交换机 原因："+cause);
            }
        });
        rabbitTemplate.setReturnCallback((Message message, int replyCode, String replyText, String exchange, String routingKey) -> {
            log.error("消息没有到达队列,交换机:{},路由键:{},消息内容:{}",exchange,routingKey,new String(message.getBody()));
        });
    }

    //定义延时队列定时解锁库存
    @Bean
    public Queue ttlQueue(){
        return QueueBuilder.durable("STOCK_TTL_QUEUE")
                .withArgument("x-message-ttl", 100000)
                .withArgument("x-dead-letter-exchange","ORDER_EXCHANGE")
                .withArgument("x-dead-letter-routing-key","order.disable")
                .build();
    }

    //绑定延时队列
    @Bean
    public Binding ttlBinding(){
        return new Binding("STOCK_TTL_QUEUE",Binding.DestinationType.QUEUE,"ORDER_EXCHANGE","stock.ttl",null);
    }


    //死信队列绑定到死信交换机
    @Bean
    public Binding deadBinding(){
        return new Binding("STOCK_UNLOCK_QUEUE",Binding.DestinationType.QUEUE,"ORDER_EXCHANGE","order.disable",null);
    }

}
