package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.*;
import com.atguigu.gmall.oms.mapper.OrderItemMapper;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallUmsClient umsClient;

    @Resource
    private OrderItemMapper itemMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public void saveOrder(Long userId, OrderSubmitVo submitVo) {

        List<OrderItemVo> items = submitVo.getItems();
        if(CollectionUtils.isEmpty(items)){
            throw new OrderException("你没有购买的商品信息");
        }

        //新增订单表
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserId(userId);
        orderEntity.setOrderSn(submitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setPayAmount(submitVo.getTotalPrice());
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());

        orderEntity.setIntegration(1000);
        orderEntity.setGrowth(2000);
        UserAddressEntity addressEntity = submitVo.getAddressEntity();
        if(addressEntity != null) {
            orderEntity.setReceiverAddress(addressEntity.getAddress());
            orderEntity.setReceiverRegion(addressEntity.getRegion());
            orderEntity.setReceiverName(addressEntity.getName());
            orderEntity.setReceiverCity(addressEntity.getCity());
            orderEntity.setReceiverPhone(addressEntity.getPhone());
            orderEntity.setReceiverPostCode(addressEntity.getPostCode());
            orderEntity.setReceiverProvince(addressEntity.getProvince());
        }
        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(submitVo.getBounds());

        System.out.println("eeeeeeeeeeeeeee"+orderEntity);
        this.save(orderEntity);
        Long orderId = orderEntity.getId();

        //新增订单详情表
        items.forEach(item -> {
            OrderItemEntity orderItemEntity = new OrderItemEntity();
            orderItemEntity.setOrderId(orderEntity.getId());
            orderItemEntity.setOrderSn(orderEntity.getOrderSn());
            orderItemEntity.setSkuQuantity(item.getCount().intValue());
            //根据skuId查询sku相关信息
            SkuEntity skuEntity = pmsClient.querySkuById(item.getSkuId()).getData();

            orderItemEntity.setSkuId(item.getSkuId());
            orderItemEntity.setSkuName(skuEntity.getName());
            orderItemEntity.setSkuPic(skuEntity.getDefaultImage());
            orderItemEntity.setSkuPrice(skuEntity.getPrice());
            orderItemEntity.setCategoryId(skuEntity.getCategoryId());
            //查询sku销售属性
            List<SkuAttrValueEntity> skuAttrValueEntities = pmsClient.querySaleAttrValuesBySkuId(item.getSkuId()).getData();
            orderItemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrValueEntities));
            //查询品牌信息
            BrandEntity brandEntity = pmsClient.queryBrandById(skuEntity.getBrandId()).getData();
            orderItemEntity.setSpuBrand(brandEntity.getName());

            //查询spu
            SpuEntity spuEntity = pmsClient.querySpuById(skuEntity.getSpuId()).getData();
            orderItemEntity.setSpuId(spuEntity.getId());
            orderItemEntity.setSpuName(spuEntity.getName());
            //查询spu描述信息
            SpuDescEntity spuDescEntity = pmsClient.querySpuDescById(spuEntity.getId()).getData();
            orderItemEntity.setSkuPic(spuDescEntity.getDecript());
            //TODO:查询商品赠送的积分信息


            itemMapper.insert(orderItemEntity);
        });

    }

}