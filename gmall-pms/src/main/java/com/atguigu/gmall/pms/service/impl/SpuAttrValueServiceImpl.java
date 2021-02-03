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

import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("spuAttrValueService")
public class SpuAttrValueServiceImpl extends ServiceImpl<SpuAttrValueMapper, SpuAttrValueEntity> implements SpuAttrValueService {

    @Autowired
    private AttrService attrService;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SpuAttrValueEntity> querySearchAttrValuesByCidAndSpuId(Long cid, Long spuId) {
        // 根据cid查询检索类型的规格参数
        QueryWrapper<AttrEntity> attrQueryWrapper = new QueryWrapper<>();
        attrQueryWrapper.eq("category_id",cid);
        attrQueryWrapper.eq("search_type", 1);
        List<AttrEntity> attrEntities = attrService.list(attrQueryWrapper);

        if(CollectionUtils.isEmpty(attrEntities)){
            return null;
        }
        //获取检索规格参数的id集合
        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

        // 根据spuid和attrIds查询出基本类型的检索规格参数值
        QueryWrapper<SpuAttrValueEntity> spuAttrValueQueryWrapper = new QueryWrapper<>();
        spuAttrValueQueryWrapper.eq("spu_id", spuId);
        spuAttrValueQueryWrapper.in("attr_id",attrIds);
        List<SpuAttrValueEntity> spuAttrValueEntities = list(spuAttrValueQueryWrapper);

        return spuAttrValueEntities;

    }

}