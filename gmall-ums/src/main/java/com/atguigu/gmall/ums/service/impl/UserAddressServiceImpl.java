package com.atguigu.gmall.ums.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserAddressMapper;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.service.UserAddressService;


@Service("userAddressService")
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddressEntity> implements UserAddressService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserAddressEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserAddressEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<UserAddressEntity> queryUserAddressByUserId(Long userId) {
        List<UserAddressEntity> userAddressEntities = this.list(new QueryWrapper<UserAddressEntity>().eq("user_id", userId));
        return userAddressEntities;
    }

}