package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.fegin.GmallSmsClient;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescService spuDescService;
    @Autowired
    private SpuAttrValueService spuAttrValueService;
    @Autowired
    private SkuService skuService;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByCidAndPage(PageParamVo paramVo,Long cid) {

        QueryWrapper<SpuEntity> queryWrapper = new QueryWrapper<>();
        if(cid != 0) {
            queryWrapper.eq("category_id", cid);
        }
        String key = paramVo.getKey();
        if(!StringUtils.isBlank(key)){
            queryWrapper.and(t -> t.eq("id",key).or().like("name",key));
        }
        IPage<SpuEntity> page = page(paramVo.getPage(), queryWrapper);


        return new PageResultVo(page);


    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu) {

        // 1.先保存spu相关信息
        // 1.1 保存pms_spu
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        save(spu);
        Long spuId = spu.getId();
        // 1.2 保存pms_spu_desc
        List<String> spuImages = spu.getSpuImages();
        if(!CollectionUtils.isEmpty(spuImages)) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuImages, ","));
            spuDescService.save(spuDescEntity);
        }
        // 1.3 保存pms_spu_value
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        if(!CollectionUtils.isEmpty(baseAttrs)){

            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().map((spuAttrValueVo)->{
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrValueVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                return spuAttrValueEntity;
            }).collect(Collectors.toList());

            spuAttrValueService.saveBatch(spuAttrValueEntities);
        }
        // 2.再保存sku相关信息
        // 2.1 保存pms_sku
        List<SkuVo> skus = spu.getSkus();
        if(!CollectionUtils.isEmpty(skus)){
            skus.forEach(sku -> {
                sku.setSpuId(spuId);
                sku.setCategoryId(spu.getCategoryId());
                sku.setBrandId(spu.getBrandId());
                //设置默认图片
                List<String> images = sku.getImages();
                if(!CollectionUtils.isEmpty(images)){
                    sku.setDefaultImage(
                            StringUtils.isNotBlank(sku.getDefaultImage()) ?
                                    sku.getDefaultImage():images.get(0));
                }
                skuService.save(sku);
                Long skuId = sku.getId();
                // 2.2 保存pms_sku_images
                if(!CollectionUtils.isEmpty(images)){
                    List<SkuImagesEntity> skuImagesEntities = images.stream().map(image -> {
                        SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                        skuImagesEntity.setSkuId(skuId);
                        skuImagesEntity.setUrl(image);
                        skuImagesEntity.setDefaultStatus(sku.getDefaultImage().equals(image) ? 1 : 0);
                        return skuImagesEntity;
                    }).collect(Collectors.toList());
                    skuImagesService.saveBatch(skuImagesEntities);
                }
                // 2.3 保存pms_sku_value
                List<SkuAttrValueEntity> saleAttrs = sku.getSaleAttrs();
                if(!CollectionUtils.isEmpty(saleAttrs)){
                    saleAttrs.forEach(saleAttr -> saleAttr.setSkuId(skuId));
                }
                skuAttrValueService.saveBatch(saleAttrs);
                // 3.最后保存营销信息
                SkuSaleVo skuSaleVo = new SkuSaleVo();
                BeanUtils.copyProperties(sku,skuSaleVo);
                skuSaleVo.setSkuId(skuId);
                gmallSmsClient.saveSales(skuSaleVo);

            });
        }

        rabbitTemplate.convertAndSend("PMS_ITEM_EXCHANGE","item.insert",spuId);

    }

}