package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {
    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";

    public List<CategoryEntity> queryLvl1Categories() {
        ResponseVo<List<CategoryEntity>> categoryResponseVo = gmallPmsClient.queryCategoriesById(0l);
        return categoryResponseVo.getData();
    }

    @GmallCache(prefix = KEY_PREFIX,timeout = 43200,random = 7200,lock = KEY_PREFIX+"lock:")
    public List<CategoryEntity> queryLv2CategoriesByPid(Long pid) {

        ResponseVo<List<CategoryEntity>> categoryResponseVo = gmallPmsClient.queryLv2CategoriesByPid(pid);
        List<CategoryEntity> categoryEntities = categoryResponseVo.getData();
        return categoryEntities;

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
    }

}
