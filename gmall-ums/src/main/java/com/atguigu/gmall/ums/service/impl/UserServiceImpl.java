package com.atguigu.gmall.ums.service.impl;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import org.springframework.util.CollectionUtils;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        switch (type){
            case 1: queryWrapper.eq("username", data); break;
            case 2: queryWrapper.eq("phone", data); break;
            case 3: queryWrapper.eq("email", data); break;
            default:
                return null;
        }
        int count = this.count(queryWrapper);
        return count == 0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        //TODO: 1、校验短信验证码 根据手机号查询redis中的验证码和code比较

        //2、生成盐salt
        String salt = StringUtils.substring(UUID.randomUUID().toString(), 0, 6);
        userEntity.setSalt(salt);
        //3、对密码加盐
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword() + salt));
        //4、新增用户
        userEntity.setLevelId(1l);
        userEntity.setNickname(userEntity.getUsername());
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setStatus(1);
        userEntity.setCreateTime(new Date());

        save(userEntity);
        //TODO: 5、删除redis短信验证码
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("username", loginName)
                .or().eq("phone", loginName)
                .or().eq("email", loginName);
        List<UserEntity> userEntities = this.list(queryWrapper);

        if(CollectionUtils.isEmpty(userEntities)){
            return null;
        }
        for (UserEntity userEntity : userEntities) {
            String s = DigestUtils.md5Hex(password+userEntity.getSalt());
            if (StringUtils.equals(s,userEntity.getPassword())) {
                return userEntity;
            }
        }
        return null;
    }

}