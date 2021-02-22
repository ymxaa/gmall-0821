package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemsGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private TemplateEngine templateEngine;

    public ItemVo loadData(Long skuId) {

        ItemVo itemVo = new ItemVo();
        //获取sku的相关信息
        CompletableFuture<SkuEntity> skuFuture = CompletableFuture.supplyAsync(() -> {
            SkuEntity skuEntity = gmallPmsClient.querySkuById(skuId).getData();
            if (skuEntity == null) {
                return null;
            }
            itemVo.setSkuId(skuId);
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            return skuEntity;
        }, threadPoolExecutor);
        //设置分类信息
        CompletableFuture<Void> categoryFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            List<CategoryEntity> categoryEntities = gmallPmsClient.query123CategoriesByCid3(skuEntity.getCategoryId()).getData();
            itemVo.setCategories(categoryEntities);
        }, threadPoolExecutor);
        //设置品牌信息
        CompletableFuture<Void> brandFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            BrandEntity brandEntity = gmallPmsClient.queryBrandById(skuEntity.getBrandId()).getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);
        //设置spu信息
        CompletableFuture<Void> spuFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            SpuEntity spuEntity = gmallPmsClient.querySpuById(skuEntity.getSpuId()).getData();
            if (skuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        //设置sku的图片列表
        CompletableFuture<Void> skuImagesFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> imagesEntities = gmallPmsClient.queryImagesBySkuId(skuId).getData();
            if (!CollectionUtils.isEmpty(imagesEntities)) {
                itemVo.setImages(imagesEntities);
            }
        }, threadPoolExecutor);
        //设置sku营销信息
        CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
            List<ItemSaleVo> saleVos = gmallSmsClient.querySalesBySkuId(skuId).getData();
            itemVo.setSales(saleVos);
        }, threadPoolExecutor);
        //设置sku库存信息
        CompletableFuture<Void> storeFuture = CompletableFuture.runAsync(() -> {
            List<WareSkuEntity> wareSkuEntities = gmallWmsClient.queryWareSkuBySkuId(skuId).getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(
                        wareSkuEntities.stream().anyMatch(
                                wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, threadPoolExecutor);
        //所有销售属性
        CompletableFuture<Void> saleAttrsFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            List<SaleAttrValueVo> saleAttrValueVos = gmallPmsClient.querySaleAttrsBySpuId(skuEntity.getSpuId()).getData();
            itemVo.setSaleAttrs(saleAttrValueVos);
        }, threadPoolExecutor);
        //当前sku销售属性
        CompletableFuture<Void> skuAttrValueFuture = CompletableFuture.runAsync(() -> {
            List<SkuAttrValueEntity> skuAttrValueEntities = gmallPmsClient.querySaleAttrValuesBySkuId(skuId).getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                Map<Long, String> map = skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue));
                itemVo.setSaleAttr(map);
            }
        }, threadPoolExecutor);
        //skuId和销售属性的映射关系
        CompletableFuture<Void> mappingFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            String json = gmallPmsClient.querySaleAttrsMappingSkuIdBySpuId(skuEntity.getSpuId()).getData();
            itemVo.setSkuJsons(json);
        }, threadPoolExecutor);
        //海报信息
        CompletableFuture<Void> descFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            SpuDescEntity spuDescEntity = gmallPmsClient.querySpuDescById(skuEntity.getSpuId()).getData();
            if (spuDescEntity != null && StringUtils.isBlank(spuDescEntity.getDecript())) {
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);
        //分组参数
        CompletableFuture<Void> groupFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            List<ItemsGroupVo> itemsGroupVos = gmallPmsClient.queryGroupWithAttrValuesBy(skuEntity.getCategoryId(), skuEntity.getSpuId(), skuId).getData();
            if (!CollectionUtils.isEmpty(itemsGroupVos)) {
                itemVo.setGroups(itemsGroupVos);
            }
        }, threadPoolExecutor);
        //等待所有子任务执行完成，才能返回
        CompletableFuture.allOf(categoryFuture,brandFuture,spuFuture,skuImagesFuture,salesFuture,storeFuture
        ,saleAttrsFuture,skuAttrValueFuture,mappingFuture,descFuture,groupFuture).join();

        return itemVo;
    }

    public void generateHtml(ItemVo itemVo){
        //初始化上下文对象，通过给该对象模板传递渲染所需目录
        Context context = new Context();
        context.setVariable("itemVo",itemVo);
        //初始化文件流
        try(PrintWriter printWriter = new PrintWriter("E:\\guli-gmall\\html\\" + itemVo.getSkuId() + ".html");) {
            templateEngine.process("item",context,printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
