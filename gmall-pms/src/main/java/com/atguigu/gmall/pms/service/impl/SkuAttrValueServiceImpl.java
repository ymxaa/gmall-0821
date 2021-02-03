package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.service.AttrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    public AttrService attrService;

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

}