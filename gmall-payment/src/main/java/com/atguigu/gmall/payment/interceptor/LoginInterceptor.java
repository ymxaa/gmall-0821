package com.atguigu.gmall.payment.interceptor;


import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.payment.pojo.UserInfo;
import com.atguigu.gmall.payment.config.JwtProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@EnableConfigurationProperties({JwtProperties.class})
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Resource
    private JwtProperties jwtProperties;

    //获取ThreadLocal中的userInfo
    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfo userInfo = new UserInfo();

        //获取userKey,把解析后的内容传递给后续服务
        String userKey = CookieUtils.getCookieValue(request, jwtProperties.getUserKey());
        if(StringUtils.isBlank(userKey)){ //如果userKey为空则重新生成
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request,response,jwtProperties.getUserKey(),userKey,jwtProperties.getExpire());
        }
        userInfo.setUserKey(userKey);

        //获取Token
        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        if (!StringUtils.isBlank(token)) {
            Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
            userInfo.setUserId(Long.valueOf(map.get("userId").toString()));
        }
        THREAD_LOCAL.set(userInfo);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清空ThreadLocal，因为使用的是tomcat的线程池，线程无法结束
        THREAD_LOCAL.remove();
    }
}
