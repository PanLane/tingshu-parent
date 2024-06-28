package com.atguigu.tingshu.order.mapper;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 分页条件查询用户订单信息
     * @param page
     * @param userId
     * @param orderStatus
     * @return
     */
    IPage<OrderInfo> selectUserPage(Page<OrderInfo> page,@Param("userId") Long userId,@Param("orderStatus") String orderStatus);

    /**
     * 根据订单号查询订单信息
     * @param orderNo
     * @return
     */
    OrderInfo selectByOrderNo(String orderNo);
}
