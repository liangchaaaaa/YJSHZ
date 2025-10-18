package com.yjshz.service;

import com.yjshz.dto.Result;
import com.yjshz.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    Result createVoucherOrder(Long voucherId);

    void createVoucherOrder1(VoucherOrder voucherOrder);
}
