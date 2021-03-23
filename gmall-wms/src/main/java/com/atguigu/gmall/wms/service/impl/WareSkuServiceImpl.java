package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.co.SkuLockVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Resource
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String LOCK_PREFIX = "stock:lock:";
    private static final String KEY_PREFIX = "stock:info:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos, String orderToken) {
        if(CollectionUtils.isEmpty(lockVos)){
            throw new OrderException("您没有要购买的商品");
        }
        //遍历所有商品，验证库存并锁库存，要具备原子性
        lockVos.forEach(lockVo -> {
            checkLock(lockVo);
        });
        //只要有一个商品锁定失败，所有锁定成功的商品要解锁库存
        if (lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())) {
            lockVos.stream().filter(SkuLockVo::getLock).forEach(lockVo -> {
                wareSkuMapper.unlock(lockVo.getWareSkuId(),lockVo.getCount());
            });
            // 响应锁定状态
            return lockVos;
        }
        //如果所有商品锁定成功，需要缓存锁定信息到redis，以方便解锁库存或者减少库存
        redisTemplate.opsForValue().set(KEY_PREFIX+orderToken, JSON.toJSONString(lockVos));
        //锁定成功之后定时解锁库存
        rabbitTemplate.convertAndSend("ORDER_EXCHANGE","stock.ttl",orderToken);

        return null;
    }

    private void checkLock(SkuLockVo lockVo){
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockVo.getSkuId());
        lock.lock();
        try {
            //验证库存 返回满足要求的库存列表
            List<WareSkuEntity> wareSkuEntities = wareSkuMapper.check(lockVo.getSkuId(), lockVo.getCount());
            //如果没有一个仓库满足要求，就验证库存失败
            if(CollectionUtils.isEmpty(wareSkuEntities)){
                lockVo.setLock(false);
                return;
            }
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
            //锁库存
            if (wareSkuMapper.lock(wareSkuEntity.getId(),lockVo.getCount()) == 1) {
                lockVo.setLock(true);
                lockVo.setWareSkuId(wareSkuEntity.getSkuId());
            }
        } finally {
            lock.unlock();
        }
    }

}