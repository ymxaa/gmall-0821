package com.atguigu.gmall.wms.service;

import com.atguigu.gmall.wms.co.SkuLockVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import java.util.List;

/**
 * 商品库存
 *
 * @author ymx
 * @email ymx@atguigu.com
 * @date 2021-01-21 10:58:05
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos, String orderToken);
}

