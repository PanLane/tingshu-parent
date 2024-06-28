package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeResponseVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {


    /**
     * 订单详情
     * @param tradeVo
     * @return
     */
    OrderInfoVo trade(TradeVo tradeVo);

    /**
     * 订单提交
     * @param orderInfoVo
     * @return
     */
    Map<String,Object> submitOrder(OrderInfoVo orderInfoVo);

    /**
     * 分页条件查询用户订单信息
     * @param page
     * @return
     */
    IPage<OrderInfo> findUserPage(Page<OrderInfo> page,String orderStatus);

    /**
     * 取消订单
     * @param orderId
     */
    void cancelOrder(Long orderId);

    /**
     * 根据订单号获取订单信息
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfo(String orderNo);

    /**
     * 更新支付成功后的状态
     * @param orderNo
     */
    void paySuccess(String orderNo);
}
