package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单
 * 
 * @author ymx
 * @email ymx@atguigu.com
 * @date 2021-02-27 21:48:16
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

	int updateStatus(@Param("orderToken") String orderToken,@Param("expectStatus") Integer expectStatus,@Param("targetStatus") Integer targetStatus);
}
