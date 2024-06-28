package com.atguigu.tingshu.user.stragety;

import java.util.List;

public interface PaymentStrategy {
    void processPayment(Long userId, String orderNo, List<Long> itemIdList) throws Exception;
}
