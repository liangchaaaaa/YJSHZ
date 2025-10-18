package com.yjshz.consultant.service;

import com.yjshz.consultant.mapper.ShopMapper;
import com.yjshz.consultant.pojo.Shop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShopService {

    @Autowired
    private ShopMapper shopMapper;

    //1.查询商家信息
    public Shop findShop(String shopName) {
        return shopMapper.findShop(shopName);
    }

}
