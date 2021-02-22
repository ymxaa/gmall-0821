package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author ymx
 * @email ymx@atguigu.com
 * @date 2021-02-21 10:44:17
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
