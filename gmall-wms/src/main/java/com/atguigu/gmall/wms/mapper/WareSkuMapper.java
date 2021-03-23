package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author ymx
 * @email ymx@atguigu.com
 * @date 2021-01-21 10:58:05
 */
@Mapper
public interface WareSkuMapper extends BaseMapper<WareSkuEntity> {

	List<WareSkuEntity> check(@Param("skuId") Long skuId,@Param("count") Integer count);

	Integer lock(@Param("id") Long id,@Param("count") Integer count);

	Integer unlock(@Param("id") Long id,@Param("count") Integer count);


}
