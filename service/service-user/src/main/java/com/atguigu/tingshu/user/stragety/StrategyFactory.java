package com.atguigu.tingshu.user.stragety;

public interface StrategyFactory {

    PaymentStrategy getStrategy(String itemType);
}
