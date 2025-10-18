package com.yjshz;

import com.yjshz.service.impl.ShopServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@SpringBootTest
@RunWith(SpringRunner.class)
public class YJSHZApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Test
    public void testSaveShop(){
        shopService.saveShop2Redis(1L,10L);

    }


}
