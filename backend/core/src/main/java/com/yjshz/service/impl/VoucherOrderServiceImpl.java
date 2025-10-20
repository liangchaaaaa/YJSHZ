package com.yjshz.service.impl;

import com.yjshz.dto.Result;
import com.yjshz.entity.VoucherOrder;
import com.yjshz.mapper.VoucherOrderMapper;
import com.yjshz.service.ISeckillVoucherService;
import com.yjshz.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yjshz.utils.RedisConstants;
import com.yjshz.utils.RedisIDWorker;
import com.yjshz.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private RedisIDWorker redisIDWorker;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private IVoucherOrderService proxy;

    private static final DefaultRedisScript<Long> SCEKILL_SCRIPT;

    static {
        SCEKILL_SCRIPT = new DefaultRedisScript<>();
        SCEKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SCEKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    private void handleVoucherOrder(VoucherOrder voucherOrder){
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("order:" + userId);
        boolean isLock = lock.tryLock();
        if( !isLock ){
            //获取锁失败
            log.error("不允许重复下单");
        }

        try {
           proxy.createVoucherOrder1(voucherOrder);
        } finally {
            lock.unlock();
        }
    }
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());

    }

    private  class VoucherOrderHandler implements Runnable{

        @Override
        public void run(){
            while( true ){
                try {
                    VoucherOrder voucherOrder = orderTasks.take();
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }
    @Override
    public Result seckillVoucher(Long voucherId) {

        Long userId = UserHolder.getUser().getId();
        //执行Lua脚本
        Long result = stringRedisTemplate.execute(SCEKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString());

        int r = result.intValue();
        if( r != 0){
            return Result.fail("获取优惠券失败");
        }
        long id = redisIDWorker.nextId("order");
        //保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIDWorker.nextId(RedisConstants.SECKILL_STOCK_KEY);
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        orderTasks.add(voucherOrder);
        //获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        //保存订单id
        return Result.ok(orderId);

    }

//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
//        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            return Result.fail("活动未开始");
//        }
//
//        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
//            return Result.fail("活动已结束");
//        }
//
//        if (seckillVoucher.getStock() > 1) {
//            return Result.fail("优惠券已经抢完");
//        }
//        Long userId = UserHolder.getUser().getId();
//
////        SimpleRedisLock simpleRedisLock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
//        //获取可重入的锁，指定锁名
//        RLock lock = redissonClient.getLock("order:" + userId);
//
//        boolean isLock = lock.tryLock();
//
//        if( !isLock ){
//            //获取锁失败
//            return  Result.fail("不允许重复下单");
//        }
//
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            simpleRedisLock.unlock();
//        }
//
//    }

    @Transactional
    public Result createVoucherOrder(Long voucherId){
        Long userId = UserHolder.getUser().getId();

        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();

        if (count > 0) {
            return Result.fail("该用户已经购买过一次!");
        }

        boolean success = seckillVoucherService.update()
                .setSql("Stock = Stock -1")
                .eq("voucher_id", voucherId).gt("stock", 0)
                .update();

        if (!success) {
            throw new RuntimeException("扣减库存失败");
        }

        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIDWorker.nextId(RedisConstants.SECKILL_STOCK_KEY);
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        save(voucherOrder);

        return Result.ok(orderId);

    }

    @Transactional
    public void createVoucherOrder1(VoucherOrder voucherOrder){
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();

        if (count > 0) {
            log.error("该用户已经购买过一次!");
        }

        boolean success = seckillVoucherService.update()
                .setSql("Stock = Stock -1")
                .eq("voucher_id", voucherId).gt("stock", 0)
                .update();

        if (!success) {
            log.error("扣减库存失败");
        }

        save(voucherOrder);

    }
}


