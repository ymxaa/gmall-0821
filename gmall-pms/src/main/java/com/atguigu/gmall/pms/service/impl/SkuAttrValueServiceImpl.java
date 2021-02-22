package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.service.AttrService;
import com.atguigu.gmall.pms.service.SkuService;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    public AttrService attrService;
    @Autowired
    public SkuService skuService;
    @Autowired
    public SkuAttrValueService skuAttrValueService;
    @Resource
    private SkuAttrValueMapper skuAttrValueMapper;
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity> querySearchAttrValuesByCidAndSkuId(Long cid, Long skuId) {

        //根据cid查询出检索类型的规格参数
        QueryWrapper<AttrEntity> attrQueryWrapper = new QueryWrapper<>();
        attrQueryWrapper.eq("category_id",cid);
        attrQueryWrapper.eq("search_type", 1);
        List<AttrEntity> attrEntities = attrService.list(attrQueryWrapper);

        if(CollectionUtils.isEmpty(attrEntities)){
            return null;
        }
        //获取检索规格参数的id集合
        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

        //查询出销售类型的检索规格参数
        QueryWrapper<SkuAttrValueEntity> skuAttrValueQueryWrapper = new QueryWrapper<>();
        skuAttrValueQueryWrapper.eq("sku_id", skuId);
        skuAttrValueQueryWrapper.in("attr_id",attrIds);
        List<SkuAttrValueEntity> skuAttrValueEntities = list(skuAttrValueQueryWrapper);

        return skuAttrValueEntities;
    }

    @Override
    public List<SaleAttrValueVo> querySaleAttrsBySpuId(Long spuId) {
        // 查询出spu下所有的sku
        List<SkuEntity> skuEntities = skuService.list(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if(CollectionUtils.isEmpty(skuEntities)){
            return null;
        }
        //搜集所有的skuId
        List<Long> ids = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
        //查询sku对应的销售属性
        List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueService.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", ids).orderByAsc("attr_id"));
        if(CollectionUtils.isEmpty(skuAttrValueEntities)){
            return null;
        }
        //以attrId进行分组
        Map<Long, List<SkuAttrValueEntity>> map = skuAttrValueEntities.stream().collect(Collectors.groupingBy(t -> t.getAttrId()));
        //把map中的每个元素转换成SaleAttrValueVo
        List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
        map.forEach((attrId,attrValueEntities)-> {
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
            saleAttrValueVo.setAttrId(attrId);
            saleAttrValueVo.setAttrName(attrValueEntities.get(0).getAttrName());
            Set<String> set = attrValueEntities.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet());
            saleAttrValueVo.setAttrValues(set);
            saleAttrValueVos.add(saleAttrValueVo);
        });

        return saleAttrValueVos;
    }

    @Override
    public String querySaleAttrsMappingSkuIdBySpuId(Long spuId) {
        // 查询出spu下所有的sku
        List<SkuEntity> skuEntities = skuService.list(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if(CollectionUtils.isEmpty(skuEntities)){
            return null;
        }
        //搜集所有的skuId
        List<Long> ids = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());

        List<Map<String, Object>> maps = skuAttrValueMapper.querySaleAttrsMappingSkuId(ids);
        Map<String,Long> mappingMap = maps.stream().collect(Collectors.toMap((map -> map.get("attr_values").toString()), map -> (Long)map.get("sku_id")));

        return JSON.toJSONString(mappingMap);
    }

}