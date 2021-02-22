package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.ItemsGroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Resource
    private AttrMapper attrMapper;
    @Resource
    private SpuAttrValueMapper spuAttrValueMapper;
    @Resource
    private SkuAttrValueMapper skuAttrValueMapper;
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<AttrGroupEntity> queryGroupsWithAttrsByCid(Long cid) {

        List<AttrGroupEntity> attrGroupEntities = list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));

        if(CollectionUtils.isEmpty(attrGroupEntities)){
            return null;
        }

        attrGroupEntities.forEach(group -> {
            QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("group_id",group.getId());
            queryWrapper.eq("type", 1);
            List<AttrEntity> attrEntities = attrMapper.selectList(queryWrapper);
            group.setAttrEntities(attrEntities);
        });

        return attrGroupEntities;
    }

    @Override
    public List<ItemsGroupVo> queryGroupWithAttrValuesBy(Long cid, Long spuId, Long skuId) {
        //根据分类id查询出所有的分组信息
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(groupEntities)) {
            return null;
        }
        return groupEntities.stream().map(attrGroupEntity -> {
            ItemsGroupVo itemsGroupVo = new ItemsGroupVo();
            //获取每个分组下的规格参数列表
            List<AttrEntity> attrEntities = attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            if(!CollectionUtils.isEmpty(attrEntities)) {
                List<AttrValueVo> attrValueVos = new ArrayList<>();
                //获取attrId的集合
                List<Long> ids = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
                //查询基本的规格参数及值
                List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().in("attr_id", ids).eq("spu_id", spuId));
                if(!CollectionUtils.isEmpty(spuAttrValueEntities)){
                    attrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity,attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }
                //查询销售的规格参数及值
                List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().in("attr_id", ids).eq("sku_id", skuId));
                if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    attrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity,attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }
                itemsGroupVo.setId(attrGroupEntity.getId());
                itemsGroupVo.setName(attrGroupEntity.getName());
                itemsGroupVo.setAttrValues(attrValueVos);
            }

            return itemsGroupVo;
        }).collect(Collectors.toList());

        //return null;
    }

}