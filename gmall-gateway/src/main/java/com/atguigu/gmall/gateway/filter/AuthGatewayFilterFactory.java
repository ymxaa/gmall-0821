package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@EnableConfigurationProperties({JwtProperties.class})
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    @Resource
    private JwtProperties jwtProperties;

    public AuthGatewayFilterFactory(){
        super(PathConfig.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("paths");
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    @Override
    public GatewayFilter apply(PathConfig config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            //1、判断当前请求路径是否在拦截名单中，不在直接放行
            List<String> paths = config.paths; //拦截名单
            String curPath = request.getURI().getPath();

            if(CollectionUtils.isEmpty(paths) || !paths.stream().allMatch(path -> StringUtils.startsWith(curPath,path))){
                return chain.filter(exchange);
            }
            //2、获取请求中的token,如果是异步：头信息，如果是同步：cookie
            String token = request.getHeaders().getFirst("token");
            if(StringUtils.isBlank(token)){
                MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                if(!CollectionUtils.isEmpty(cookies) && cookies.containsKey(jwtProperties.getCookieName())) {
                    token = cookies.getFirst(jwtProperties.getCookieName()).getValue();

                }
                //3、判断token信息是否为空，为空则重定向到登录页面
                if(StringUtils.isBlank(token)){
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl="+request.getURI());
                    return response.setComplete(); //拦截请求
                }
            }

            try {
                //4、使用公钥解析jwt，解析异常则重定向到登录页面
                Map<String, Object> infoFromToken = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
                //5、解析成功判断ip是否一致，否则重定向到登录页面
                String ip = infoFromToken.get("ip").toString(); //载荷中的ip地址
                String curIp = IpUtil.getIpAddressAtGateway(request);
                if(!StringUtils.equals(ip,curIp)){
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl="+request.getURI());
                    return response.setComplete(); //拦截请求
                }
                //6、把jwt中的登录信息传递给后续服务
                request.mutate().header("userId",infoFromToken.get("userId").toString()).build();
                exchange.mutate().request(request).build();
                //7、放行
                return chain.filter(exchange);
                
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl="+request.getURI());
                return response.setComplete(); //拦截请求
            }
        };
    }

    @Data
    public static class PathConfig{
        private List<String> paths;
        //private String value;
    }
}
