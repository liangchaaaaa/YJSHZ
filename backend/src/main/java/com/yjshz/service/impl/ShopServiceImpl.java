package com.yjshz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yjshz.dto.Result;
import com.yjshz.entity.Shop;
import com.yjshz.mapper.ShopMapper;
import com.yjshz.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yjshz.utils.CacheClient;
import com.yjshz.utils.RedisConstants;
import com.yjshz.utils.RedisData;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.beans.Transient;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    @Override
    public Result queryById(Long id){
        //Shop shop = queryWithMutex(id);
        //Shop shop = queryWithLogincalExpire(id);
        Shop shop = cacheClient
                .queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if( shop == null){
            return Result.fail("查询失败");
        }

        return Result.ok(shop);
    }

    /* 互斥锁解决缓存击穿 */
    public Shop queryWithMutex(Long id){
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        if ( StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        if(shopJson != null) {
            return null;
        }

        //实现缓存重建：获取互斥锁、判断是否获取成功
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;

        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            if( !isLock ){ //获取锁失败
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //获取锁成功，再次检测Redis缓存是否存在
            shop = getById(id);
            if( shop == null){
                stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            /*释放互斥锁*/
            /*疑问：未获取锁的线程会释放锁？*/
            unlock(lockKey);
        }
        return shop;
    }

    /**
     * 解决缓存穿透
     * @param id
     * @return Shop
     */
    public Shop queryWithPassThrough(Long id){
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        if ( StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        if(shopJson != null) {
            return null;
        }

        Shop shop = getById(id);
        if( shop == null){
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop));
        return shop;
    }

    public Shop queryWithLogincalExpire(Long id){
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        if ( StrUtil.isBlank(shopJson)) {
            return null;
        }

        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject)redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        if(expireTime.isAfter(LocalDateTime.now())){ //没过期
            return shop;
        }

        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);

        if(isLock) {
            //开启独立线程，执行缓存重建
            CACHE_REBUILD_EXECUTOR.submit( () -> {
                try {
                    this.saveShop2Redis(id,20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlock(lockKey);
                }
            });
        }

        return shop;
    }

    @Transactional
    @Override
    public Result update(Shop shop){
        Long id = shop.getId();
        if( id == null){
            return Result.fail("店铺id不存在");
        }
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        updateById(shop);
        stringRedisTemplate.delete(key);
        return Result.ok();

    }

    public void saveShop2Redis(Long id,Long expireseconds) throws InterruptedException{
        Shop shop = getById(id);
        Thread.sleep(200); //模拟缓存重建的延迟
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireseconds));

        String key = RedisConstants.CACHE_SHOP_KEY + id;
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));

    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(flag);
    }

    private boolean unlock(String key){
        Boolean flag = stringRedisTemplate.delete(key);
        return  BooleanUtil.isTrue(flag);

    }



}
