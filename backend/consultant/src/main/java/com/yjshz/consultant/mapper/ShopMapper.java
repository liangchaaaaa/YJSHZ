package com.yjshz.consultant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yjshz.consultant.pojo.Reservation;
import com.yjshz.consultant.pojo.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
    @Select("select * from tb_shop where name=#{shopName}")
    Shop findShop(String shopName);
}
