package com.atguigu.gmall.sms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author ymx
 * @email ymx@atguigu.com
 * @date 2021-01-18 21:11:32
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

