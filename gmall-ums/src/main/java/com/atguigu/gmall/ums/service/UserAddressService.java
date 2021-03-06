package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;

import java.util.List;
import java.util.Map;

/**
 * 收货地址表
 *
 * @author ymx
 * @email ymx@atguigu.com
 * @date 2021-02-21 10:44:17
 */
public interface UserAddressService extends IService<UserAddressEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<UserAddressEntity> queryUserAddressByUserId(Long userId);
}

