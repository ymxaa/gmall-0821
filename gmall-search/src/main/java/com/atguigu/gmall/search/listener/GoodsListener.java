package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;
    @Autowired
    private GoodsRepository goodsRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "SEARCH_INSERT_QUEUE",durable = "true"),
            exchange = @Exchange(value = "PMS_ITEM_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        // 查询spu
        if(spuId == null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        ResponseVo<SpuEntity> spuEntityResponseVo = this.gmallPmsClient.querySpuById(spuId);
        SpuEntity spuEntity = spuEntityResponseVo.getData();
        if(spuEntity == null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        ResponseVo<List<SkuEntity>> skuResponseVo = gmallPmsClient.querySkuBySpuId(spuId);
        List<SkuEntity> skuEntities = skuResponseVo.getData();

        if(!CollectionUtils.isEmpty(skuEntities)){
            // 将sku集合转化为goods集合
            List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                Goods goods = new Goods();
                // sku相关信息
                goods.setSkuId(skuEntity.getId());
                goods.setPrice(skuEntity.getPrice().doubleValue());
                goods.setTitle(skuEntity.getTitle());
                goods.setSubTitle(skuEntity.getSubtitle());
                goods.setDefaultImage(skuEntity.getDefaultImage());
                // 创建时间
                goods.setCreateTime(spuEntity.getCreateTime());
                // 获取库存
                ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = gmallWmsClient.queryWareSkuBySkuId(skuEntity.getId());
                List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
                if(!CollectionUtils.isEmpty(wareSkuEntities)){
                    goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a,b) -> a+b).get());
                    goods.setStore(wareSkuEntities.stream().anyMatch(
                            wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                }
                // 品牌
                ResponseVo<BrandEntity> brandResponseVo = gmallPmsClient.queryBrandById(skuEntity.getBrandId());
                BrandEntity brandEntity = brandResponseVo.getData();
                if(brandEntity != null){
                    goods.setBrandId(brandEntity.getId());
                    goods.setBrandName(brandEntity.getName());
                    goods.setLogo(brandEntity.getLogo());
                }
                // 分类
                ResponseVo<CategoryEntity> categoryResponseVo = gmallPmsClient.queryCategoryById(skuEntity.getCategoryId());
                CategoryEntity categoryEntity = categoryResponseVo.getData();
                if(categoryEntity != null){
                    goods.setCategoryId(categoryEntity.getId());
                    goods.setCategoryName(categoryEntity.getName());
                }
                // 检索参数
                List<SearchAttrValue> attrValues = new ArrayList<>();

                ResponseVo<List<SkuAttrValueEntity>> saleAttrValueResponseVo =
                        gmallPmsClient.querySearchAttrValuesByCidAndSkuId(skuEntity.getCategoryId(), skuEntity.getId());
                List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrValueResponseVo.getData();
                if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    List<SearchAttrValue> skuToSearchAttrValues = skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        SearchAttrValue searchAttrValue = new SearchAttrValue();
                        BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValue);
                        return searchAttrValue;
                    }).collect(Collectors.toList());
                    attrValues.addAll(skuToSearchAttrValues);
                }

                ResponseVo<List<SpuAttrValueEntity>> baseAttrValueResponseVo =
                        gmallPmsClient.querySearchAttrValuesByCidAndSpuId(skuEntity.getCategoryId(), skuEntity.getSpuId());
                List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrValueResponseVo.getData();
                if(!CollectionUtils.isEmpty(spuAttrValueEntities)){
                    List<SearchAttrValue> spuToSearchAttrValues = spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        SearchAttrValue searchAttrValue = new SearchAttrValue();
                        BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValue);
                        return searchAttrValue;
                    }).collect(Collectors.toList());
                    attrValues.addAll(spuToSearchAttrValues);
                }
                goods.setSearchAttrs(attrValues);
                return goods;
            }).collect(Collectors.toList());
            goodsRepository.saveAll(goodsList);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
