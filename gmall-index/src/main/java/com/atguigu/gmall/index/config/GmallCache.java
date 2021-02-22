package com.atguigu.gmall.index.config;

import java.lang.annotation.*;

@Target({ElementType.METHOD}) //注解作用在方法上
@Retention(RetentionPolicy.RUNTIME) //运行时注解
// @Inherited //可继承
@Documented //加入文档
public @interface GmallCache {
    /**
     * 缓存的前缀
     * 将来的缓存的key: prefix + 方法参数
     * @return
     * */
    String prefix() default "";
    /**
     * 缓存的过期时间
     * 默认5分钟
     * @return
     * */
    int timeout() default 5;
    /**
     * 为了防止缓存的雪崩
     * 给缓存指定随机值范围
     * 默认5分钟
     * @return
     * */
    int random() default 5;
    /**
     * 为了防止缓存的击穿
     * 给分布式锁缓存指定前缀
     * @return
     * */
    String lock() default "lock:";
}
