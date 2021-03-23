package com.atguigu.com.gmall.order.interceptor;



import com.atguigu.com.gmall.order.config.JwtProperties;
import com.atguigu.com.gmall.order.pojo.UserInfo;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

        //获取Token
        String userId = request.getHeader("userId");
        userInfo.setUserId(Long.valueOf(userId));

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
