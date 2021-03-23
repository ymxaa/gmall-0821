package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.api.GmallSmsApi;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private GmallPmsApi pmsApi;
    @Autowired
    private GmallWmsApi wmsApi;
    @Autowired
    private GmallSmsApi smsApi;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartAsyncService cartAsyncService;

    @Resource
    private CartMapper cartMapper;

    private static final String KEY_PREFIX = "cart:info:";
    private static final String PRICE_PREFIX = "cart:price:";

    private String getUserId(){
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        if(userId != null){
            return userId.toString();
        }
        return userInfo.getUserKey();
    }

    public void saveCart(Cart cart) {
        //获取用户的登录信息
        String userId = getUserId();
        //获取当前用户的购物车
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        //判断该用户的购物车是否包含该商品
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)) {
            //包含更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson,Cart.class);
            cart.setCount(cart.getCount().add(count));
            hashOps.put(skuId,JSON.toJSONString(cart));

            cartMapper.update(cart, new UpdateWrapper<Cart>().eq("user_id",userId).eq("sku_id",skuId));
        }else {
            //不包含就新增一条记录
            cart.setUserId(userId);
            cart.setCheck(true);
            //查询sku相关信息
            SkuEntity skuEntity = pmsApi.querySkuById(cart.getSkuId()).getData();
            if(skuEntity == null){
                return;
            }
            cart.setTitle(skuEntity.getTitle());
            cart.setPrice(skuEntity.getPrice());
            cart.setDefaultImage(skuEntity.getDefaultImage());
            //查询库存
            List<WareSkuEntity> wareSkuEntities = wmsApi.queryWareSkuBySkuId(cart.getSkuId()).getData();
            if(!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
            //查询营销信息
            List<ItemSaleVo> itemSaleVos = smsApi.querySalesBySkuId(cart.getSkuId()).getData();
            if(!CollectionUtils.isEmpty(itemSaleVos)){
                cart.setSales(JSON.toJSONString(itemSaleVos));
            }
            //查询销售属性
            List<SkuAttrValueEntity> skuAttrValueEntities = pmsApi.querySaleAttrValuesBySkuId(cart.getSkuId()).getData();
            if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
                cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));
            }

            cartAsyncService.insertCart(userId,cart);
            //加入购物车价格缓存
            redisTemplate.opsForValue().set(PRICE_PREFIX+skuId,skuEntity.getPrice().toString());
            //cartMapper.insert(cart);
        }
        hashOps.put(skuId,JSON.toJSONString(cart));
    }

    public Cart queryCartBySkuId(Long skuId) {
        //获取登录信息
        String userId = getUserId();
        //获取当前用户的购物车
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if(hashOps.hasKey(skuId.toString())){
            String cartJson = hashOps.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson, Cart.class);
        }
        throw new CartException("该用户的购物车不包含该记录");
    }

    public List<Cart> queryCart() {



        //1、获取userKey
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        String unLoginKey = KEY_PREFIX + userKey;
        //2、根据userKey查询未登录的购物车
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(unLoginKey);
        List<Object> cartJsons = hashOps.values();
        List<Cart> unLoginCarts = null;
        if(!CollectionUtils.isEmpty(cartJsons)){
            unLoginCarts = cartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                //设置实时价格
                String newPrice = redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                if(StringUtils.isBlank(newPrice)){
                    SkuEntity skuEntity = pmsApi.querySkuById(cart.getSkuId()).getData();
                    redisTemplate.opsForValue().set(PRICE_PREFIX+cart.getSkuId(),skuEntity.getPrice().toString());
                }
                cart.setCurrentPrice(new BigDecimal(redisTemplate.opsForValue().get(PRICE_PREFIX+cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }
        //3、获取userId
        Long userId = userInfo.getUserId();
        //4、如果userId为空，则直接返回
        if(userId == null) {
            return unLoginCarts;
        }
        //5、把未登录的购物车合并到登录状态的购物车
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps1 = redisTemplate.boundHashOps(loginKey);
        if(!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                //如果用户的购物车包含了该记录就合并数量
                String skuId = cart.getSkuId().toString();
                if(hashOps1.hasKey(skuId)){
                    String cartJson = hashOps1.get(skuId).toString();
                    Cart loginCart = JSON.parseObject(cartJson, Cart.class);
                    loginCart.setCount(loginCart.getCount().add(cart.getCount()));

                    //写入redis 异步写入mysql
                    hashOps1.put(skuId,JSON.toJSONString(loginCart));
                    cartAsyncService.updateCart(userId.toString(),skuId,loginCart);
                    //cartMapper.update(loginCart, new UpdateWrapper<Cart>().eq("user_id",userId.toString()).eq("sku_id",skuId));
                }else {
                    //不包含就新增记录
                    cart.setUserId(userId.toString());

                    hashOps1.put(skuId, JSON.toJSONString(cart));
                    cartAsyncService.insertCart(userId.toString(),cart);
                    //cartMapper.insert(cart);
                }
            });
        }
        //6、把未登录的购物车删除
        redisTemplate.delete(unLoginKey);
        cartAsyncService.deleteCart(userId.toString(),userKey);
        //cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userKey));
        //7、返回登录状态的购物车
        List<Object> loginCartJson = hashOps1.values();
        if(!CollectionUtils.isEmpty(loginCartJson)){
            return loginCartJson.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);

                //设置实时价格
                String newPrice = redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                if(StringUtils.isBlank(newPrice)){
                    SkuEntity skuEntity = pmsApi.querySkuById(cart.getSkuId()).getData();
                    redisTemplate.opsForValue().set(PRICE_PREFIX+cart.getSkuId(),skuEntity.getPrice().toString());
                }
                cart.setCurrentPrice(new BigDecimal(redisTemplate.opsForValue().get(PRICE_PREFIX+cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }
        return null;
    }

    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        BigDecimal count = cart.getCount();

        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (!hashOps.hasKey(cart.getSkuId().toString())){
            throw new CartException("购物车不存在！");
        }
        String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
        cart = JSON.parseObject(cartJson,Cart.class);
        cart.setCount(count);

        hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        cartAsyncService.updateCart(userId,cart.getSkuId().toString(),cart);
    }

    public void deleteCart(Long skuId) {
        String userId = this.getUserId();

        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (!hashOps.hasKey(skuId.toString())){
            throw new CartException("删除的购物车不存在！");
        }
        hashOps.delete(skuId.toString());
        cartAsyncService.deleteCartBySkuId(userId,skuId);
    }

    public List<Cart> queryCheckedCarts(Long userId) {
        String key = KEY_PREFIX+userId;
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        List<Object> cartJsons = hashOps.values();
        if(CollectionUtils.isEmpty(cartJsons)){
            return null;
        }
        return cartJsons.stream()
                .map(cartJson -> JSON.parseObject(cartJson.toString(),Cart.class))
                .filter(Cart::getCheck)
                .collect(Collectors.toList());

    }
}
