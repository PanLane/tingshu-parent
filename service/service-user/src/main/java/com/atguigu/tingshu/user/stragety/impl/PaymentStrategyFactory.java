package com.atguigu.tingshu.user.stragety.impl;

import com.atguigu.tingshu.user.stragety.PaymentStrategy;
import com.atguigu.tingshu.user.stragety.StrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaymentStrategyFactory implements StrategyFactory {

    @Autowired
    Map<String,PaymentStrategy> map;

    @Override
    public PaymentStrategy getStrategy(String itemType) {
        return map.get(itemType);
    }
}
