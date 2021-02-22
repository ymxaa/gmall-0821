package com.atguigu.gmall.index.config.apect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.config.GmallCache;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter bloomFilter;

    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

         /*
        //手动实现了防止缓存的击穿、穿透和雪崩问题
        //先查询缓存，如果缓存中有就直接返回
        String s = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if(!StringUtils.isBlank(s)){
            return JSON.parseArray(s,CategoryEntity.class);
        }

        //为了防止缓存击穿，添加分布式锁
        RLock lock = redissonClient.getLock(KEY_PREFIX + "lock:" + pid);
        lock.lock();

        //再次查询缓存，因为在请求等待获取锁的过程中，可能有其他请求已把数据放入缓存
        String s1 = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if(!StringUtils.isBlank(s1)){
            lock.unlock();
            return JSON.parseArray(s1,CategoryEntity.class);
        }

        //再去查询缓存并放入缓存
        ResponseVo<List<CategoryEntity>> categoryResponseVo = gmallPmsClient.queryLv2CategoriesByPid(pid);
        List<CategoryEntity> categoryEntities = categoryResponseVo.getData();

        //为了防止缓存穿透，数据即使为Null也缓存
        if(CollectionUtils.isEmpty(categoryEntities)) {
            redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities),5,TimeUnit.MINUTES);
        }else {
            //为了防止缓存雪崩，给缓存添加随机值
            redisTemplate.opsForValue().set(KEY_PREFIX + pid,
                    JSON.toJSONString(categoryEntities),
                    30+ new Random().nextInt(10),TimeUnit.DAYS);
        }
        lock.unlock();
        return categoryEntities;
        */

        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache annotation = method.getAnnotation(GmallCache.class);
        List<Object> args = Arrays.asList(joinPoint.getArgs());

        int random = annotation.random();
        int timeout = annotation.timeout();
        String lockName = annotation.lock();
        String KEY_PREFIX = annotation.prefix();
        String pid = ((Long) args.get(0)).toString();
        Class returnType = signature.getReturnType();

        //添加布隆过滤器
        if(!bloomFilter.contains(KEY_PREFIX + pid)){
            return null;
        }
        //先查询缓存，如果缓存中有就直接返回
        String s = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if(!StringUtils.isBlank(s)){
            return JSON.parseObject(s, returnType);
        }

        //为了防止缓存击穿，添加分布式锁
        RLock lock = redissonClient.getLock(lockName + pid);
        lock.lock();

        //再次查询缓存，如果缓存中有就直接返回
        String s1 = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if(!StringUtils.isBlank(s1)){
            return JSON.parseObject(s1, returnType);
        }
        Object result;


        try {
            //执行目标方法
            result = joinPoint.proceed(joinPoint.getArgs());

            //为了防止缓存穿透，数据即使为Null也缓存
            if(result == null) {
                redisTemplate.opsForValue().set(
                        KEY_PREFIX + pid,
                        JSON.toJSONString(result),
                        5,TimeUnit.MINUTES
                );
            }else {
                //为了防止缓存雪崩，给缓存添加随机值
                redisTemplate.opsForValue().set(
                        KEY_PREFIX + pid,
                        JSON.toJSONString(result),
                        new Random().nextInt(random)+timeout,
                        TimeUnit.MINUTES
                );
            }
        } finally {
            lock.unlock();
        }

        return result;
    }
}
