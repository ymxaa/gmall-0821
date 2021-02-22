package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemsGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {
    //面包屑：分类
    private List<CategoryEntity> categories;
    //面包屑：品牌
    private Long brandId;
    private String brandName;
    //面包屑：spu
    private Long spuId;
    private String spuName;

    //中间的sku的信息
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private String defaultImage;
    private Integer weight;
    //图片列表
    private List<SkuImagesEntity> images;
    //营销信息
    private List<ItemSaleVo> sales;
    //库存信息
    private Boolean store = false;
    //跟当前sku相同的spu下的所有sku的销售属性列表
    private List<SaleAttrValueVo> saleAttrs;
    //当前sku的销售参数
    private Map<Long,String> saleAttr;
    //销售属性组合和skuId的映射关系
    private String skuJsons;
    //商品的海报信息
    private List<String> spuImages;
    //规格参数分组列表
    private List<ItemsGroupVo> groups;

}