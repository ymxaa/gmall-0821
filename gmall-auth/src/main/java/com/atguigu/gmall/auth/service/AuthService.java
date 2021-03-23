package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.fegin.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.security.auth.message.AuthException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@Service
@EnableConfigurationProperties({JwtProperties.class})
public class AuthService {

    @Resource
    JwtProperties jwtProperties;
    @Autowired
    private GmallUmsClient gmallUmsClient;

    public void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            //1、校验用户名和密码是否正确：调用远程接口
            UserEntity userEntity = gmallUmsClient.queryUser(loginName, password).getData();
            //2、判断用户信息是否为空
            if(userEntity == null){
                throw new UserException("用户名或密码错误！");
            }
            //3、组织载核
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId", userEntity.getId());
            map.put("userName",userEntity.getUsername());
            map.put("ip", IpUtil.getIpAddressAtService(request));
            //4、制作JWT
            String token = JwtUtils.generateToken(map, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
            //5、把JWT放入cookie中
            CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),token,jwtProperties.getExpire()*60);
            //6、为了方便展示用户的登录信息，需要写入unick
            CookieUtils.setCookie(request,response,jwtProperties.getUnick(),userEntity.getNickname(),jwtProperties.getExpire()*60);

        } catch (Exception e) {
            e.printStackTrace();
            throw new AuthException("登录异常！");
        }
    }
}
