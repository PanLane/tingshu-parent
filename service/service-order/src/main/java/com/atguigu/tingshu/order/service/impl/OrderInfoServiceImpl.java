package com.atguigu.tingshu.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderDerateService;
import com.atguigu.tingshu.order.service.OrderDetailService;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.user.client.VipServiceConfigFeignClient;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.account.AccountMinusVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.order.*;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private UserInfoFeignClient userInfoFeignClient;
    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;
    @Autowired
    private VipServiceConfigFeignClient vipServiceConfigFeignClient;
    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserAccountFeignClient userAccountFeignClient;
    @Autowired
    private KafkaTemplate kafkaTemplate;
    @Autowired
    private OrderDerateService orderDerateService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private RedissonClient redissonClient;


    @Override
    public OrderInfoVo trade(TradeVo tradeVo) {
        //接收数据
        Long itemId = tradeVo.getItemId();
        //创建对象、定义变量
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        BigDecimal originalAmount = new BigDecimal("0.00");//原始金额
        BigDecimal derateAmount = new BigDecimal("0.00");//减免总金额
        BigDecimal orderAmount = new BigDecimal("0.00");//订单总价
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();//订单明细列表
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();//订单减免明细列表

        switch (tradeVo.getItemType()) {
            case SystemConstant.ORDER_ITEM_TYPE_ALBUM: //购买专辑

                //判断用户是否已购买改专辑、如果已购买，抛异常
                Result<Boolean> paidAlbumResult = userInfoFeignClient.isPaidAlbum(itemId);
                Assert.notNull(paidAlbumResult, "用户是否购买专辑结果集为空");
                Boolean paidAlbum = paidAlbumResult.getData();
                Assert.notNull(paidAlbum, "用户是否购买专辑结果为空");
                if (paidAlbum) throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);

                //根据专辑id获取专辑信息
                Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(itemId);
                Assert.notNull(albumInfoResult, "专辑信息结果集为空");
                AlbumInfo albumInfo = albumInfoResult.getData();
                Assert.notNull(albumInfo, "专辑信息为空");

                //根据用户id获取用户信息
                Result<UserInfoVo> userInfoResult = userInfoFeignClient.getUserInfoById(albumInfo.getUserId());
                Assert.notNull(userInfoResult, "用户信息结果集为空");
                UserInfoVo userInfoVo = userInfoResult.getData();
                Assert.notNull(userInfoVo, "用户信息为空");

                //给变量赋值
                originalAmount = albumInfo.getPrice();//原始金额
                //判断用户是否是vip状态
                if (userInfoVo.getIsVip() == 1 && new Date().before(userInfoVo.getVipExpireTime())) {
                    //用户是vip状态，享受vip折扣
                    //判断是否打折
                    if (albumInfo.getVipDiscount().compareTo(new BigDecimal("-1")) != 0) {
                        //打折，减免总金额 = 专辑价格 * (10-折扣)/10
                        derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getVipDiscount()).divide(new BigDecimal("10"), 2, BigDecimal.ROUND_HALF_UP));
                        //订单总价 = 专辑价格 - 减免总金额
                        orderAmount = originalAmount.subtract(derateAmount);
                    } else {
                        //不打折，订单金额等于原始金额
                        orderAmount = originalAmount;
                    }
                } else {
                    //用户不是vip状态，享受普通折扣
                    //判断是否打折
                    if (albumInfo.getDiscount().compareTo(new BigDecimal("-1")) != 0) {
                        //打折，减免总金额 = 专辑价格 * (10-折扣)/10
                        derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getDiscount()).divide(new BigDecimal("10"), 2, BigDecimal.ROUND_HALF_UP));
                        //订单总价 = 专辑价格 - 减免总金额
                        orderAmount = originalAmount.subtract(derateAmount);
                    } else {
                        //不打折，订单金额等于原始金额
                        orderAmount = originalAmount;
                    }
                }
                //创建订单明细列表并赋值
                OrderDetailVo orderDetailVo = new OrderDetailVo();
                orderDetailVo.setItemId(itemId);
                orderDetailVo.setItemName(albumInfo.getAlbumTitle());
                orderDetailVo.setItemPrice(orderAmount);
                orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
                orderDetailVoList.add(orderDetailVo);//将订单明细列表加到集合中
                //如果减免总金额大于0，创建订单减免明细列表并赋值
                if (derateAmount.compareTo(new BigDecimal("0")) != 0) {
                    OrderDerateVo orderDerateVo = new OrderDerateVo();
                    orderDerateVo.setDerateAmount(derateAmount);
                    orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                    orderDerateVoList.add(orderDerateVo);//将订单减免明细添加到订单减免明细集合中
                }
                break;
            case SystemConstant.ORDER_ITEM_TYPE_TRACK: //购买声音
                //校验参数
                if (tradeVo.getTrackCount() <= 0) throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
                //根据声音id、声音数量，获取用户下单付费声音列表
                Result<List<TrackInfo>> paidTrackInfoListResult = trackInfoFeignClient.findPaidTrackInfoList(itemId, tradeVo.getTrackCount());
                Assert.notNull(paidTrackInfoListResult, "用户下单付费声音列表结果集为空");
                List<TrackInfo> paidTrackInfoList = paidTrackInfoListResult.getData();
                Assert.notNull(paidTrackInfoList, "用户下单付费声音列表为空");
                //调用专辑客户端，根据id获取专辑信息
                Result<AlbumInfo> albumInfoResult2 = albumInfoFeignClient.getAlbumInfo(paidTrackInfoList.get(0).getAlbumId());
                Assert.notNull(albumInfoResult2, "专辑信息结果集为空");
                AlbumInfo albumInfo2 = albumInfoResult2.getData();
                Assert.notNull(albumInfo2, "专辑信息为空");
                //为订单金额，原始金额赋值
                orderAmount = originalAmount = albumInfo2.getPrice().multiply(new BigDecimal(tradeVo.getTrackCount()));
                for (TrackInfo trackInfo : paidTrackInfoList) {
                    //创建订单详情对象
                    OrderDetailVo orderDetailVo1 = new OrderDetailVo();
                    orderDetailVo1.setItemUrl(trackInfo.getCoverUrl());
                    orderDetailVo1.setItemName(trackInfo.getTrackTitle());
                    orderDetailVo1.setItemId(trackInfo.getId());
                    orderDetailVo1.setItemPrice(albumInfo2.getPrice());
                    //将订单详情对象添加到订单详情列表中
                    orderDetailVoList.add(orderDetailVo1);
                }
                break;
            case SystemConstant.ORDER_ITEM_TYPE_VIP: //购买vip
                //通过itemId获取vip服务配置信息
                Result<VipServiceConfig> vipServiceConfigResult = vipServiceConfigFeignClient.getVipServiceConfig(itemId);
                Assert.notNull(vipServiceConfigResult, "vip服务配置结果集为空");
                VipServiceConfig vipServiceConfig = vipServiceConfigResult.getData();
                Assert.notNull(vipServiceConfig, "vip服务配置信息为空");
                //赋值
                originalAmount = vipServiceConfig.getPrice();
                orderAmount = vipServiceConfig.getDiscountPrice();
                derateAmount = originalAmount.subtract(orderAmount);
                //创建订单明细列表并赋值
                OrderDetailVo orderDetailVo2 = new OrderDetailVo();
                orderDetailVo2.setItemId(itemId);
                orderDetailVo2.setItemName(vipServiceConfig.getName());
                orderDetailVo2.setItemPrice(orderAmount);
                orderDetailVo2.setItemUrl(vipServiceConfig.getImageUrl());
                orderDetailVoList.add(orderDetailVo2);//将订单明细列表加到集合中
                //如果减免总金额大于0，创建订单减免明细列表并赋值
                if (derateAmount.compareTo(new BigDecimal("0")) != 0) {
                    OrderDerateVo orderDerateVo = new OrderDerateVo();
                    orderDerateVo.setDerateAmount(derateAmount);
                    orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
                    orderDerateVoList.add(orderDerateVo);//将订单减免明细添加到订单减免明细集合中
                }
                break;
            default:
                return orderInfoVo;
        }

        //赋值
        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        orderInfoVo.setTradeNo(tradeNo);//订单号
        orderInfoVo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);//支付方式
        orderInfoVo.setItemType(tradeVo.getItemType()); //付款项目类型
        orderInfoVo.setOriginalAmount(originalAmount); //原始金额
        orderInfoVo.setDerateAmount(derateAmount);//减免总金额
        orderInfoVo.setOrderAmount(orderAmount); //订单总金额
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);//订单明细列表
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);//订单减免明细列表
        orderInfoVo.setTimestamp(SignHelper.getTimestamp());//时间戳
        //签名  流程：order对象 -> string -> map -> treemap -> 遍历：value 与 | 进行拼接 -> 加盐 -> MD5将字符串进行加密 -> 得到签名
        orderInfoVo.setSign(SignHelper.getSign(JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class)));

        //将tradeNo存储到redis中，防止表单重复提交
        String key = "tradeNo:" + AuthContextHolder.getUserId() + ":" + tradeNo;
        redisTemplate.opsForValue().set(key, tradeNo,1,TimeUnit.HOURS);

        //返回数据
        return orderInfoVo;
    }

    @Override
    @GlobalTransactional
    public Map<String,Object> submitOrder(OrderInfoVo orderInfoVo) {

        //验证签名
        Map map = JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class);
        map.put("payWay", SystemConstant.ORDER_PAY_WAY_WEIXIN);
        SignHelper.checkSign(map);

        //获取用户id
        Long userId = AuthContextHolder.getUserId();
        //获取交易号
        String tradeNo = orderInfoVo.getTradeNo();

        //验证表单是否重复提交
        String key = "tradeNo:" + userId + ":" + tradeNo;
        Boolean deleted = redisTemplate.delete(key);
        if (!deleted) {
            //订单重复提交，抛异常
            throw new GuiguException(ResultCodeEnum.REPEAT_SUBMIT);
        }

        //生成订单号
        String orderNo = UUID.randomUUID().toString().replace("-", "");

        //判断支付类型
        switch (orderInfoVo.getPayWay()) {
            case SystemConstant.ORDER_PAY_ACCOUNT:
                try {
                    //余额支付
                    //调用账户微服务，检查并锁定账户余额
                    /*Result<AccountLockResultVo> accountLockResultVoResult = userAccountFeignClient.checkAndLock(new AccountLockVo(orderNo, userId, orderInfoVo.getOrderAmount(),orderInfoVo.getOrderDetailVoList().get(0).getItemName()));
                    Assert.notNull(accountLockResultVoResult, "锁定账户结果集为空");
                    if (!ResultCodeEnum.SUCCESS.getCode().equals(accountLockResultVoResult.getCode())) {
                        //账户余额锁定失败，抛异常
                        throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_ERROR);
                    }*/

                    //调用账户微服务，检查并扣减账户余额
                    Result<AccountLockResultVo> accountLockResultVoResult = userAccountFeignClient.checkAndMinus(new AccountMinusVo(orderNo, userId, orderInfoVo.getOrderAmount(), orderInfoVo.getOrderDetailVoList().get(0).getItemName()));
                    Assert.notNull(accountLockResultVoResult, "检查并扣减账户结果集为空");
                    if(!ResultCodeEnum.SUCCESS.getCode().equals(accountLockResultVoResult.getCode())){
                        throw new GuiguException(accountLockResultVoResult.getCode(), accountLockResultVoResult.getMessage());
                    }

                    //int i = 1/0;

                    //保存订单信息
                    saveOrder(orderInfoVo, userId, orderNo);

                    //订单保存成功，向kafka中发送消息，扣除用户余额
                    //kafkaTemplate.send(KafkaConstant.QUEUE_ACCOUNT_MINUS, orderNo);
                } catch (GuiguException e) {
                    /*log.error("---------------账户锁定失败!-----------");
                    e.printStackTrace();*/
                    throw e;
                } catch (Exception e) {
                    //订单保存失败，向kafka中发送消息，解锁账户
                    /*log.error("------------订单保存失败，解锁账户------------");
                    kafkaTemplate.send(KafkaConstant.QUEUE_ACCOUNT_UNLOCK, orderNo);
                    e.printStackTrace();*/
                    throw e;
                }
                break;
            case SystemConstant.ORDER_PAY_WAY_WEIXIN:
            case SystemConstant.ORDER_PAY_WAY_ALIPAY:
                //在线支付,调用saveOrder方法
                saveOrder(orderInfoVo, userId, orderNo);
                break;
            default:
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //返回订单号
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        return result;
    }

    @Override
    public IPage<OrderInfo> findUserPage(Page<OrderInfo> page,String orderStatus) {
        return orderInfoMapper.selectUserPage(page, AuthContextHolder.getUserId(),orderStatus);
    }

    @Override
    public void cancelOrder(Long orderId) {
        //根据订单id查询订单信息
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if(SystemConstant.ORDER_STATUS_UNPAID.equals(orderInfo.getOrderStatus())){
            //订单未支付，取消订单
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_CANCEL);
            orderInfoMapper.updateById(orderInfo);
        }
    }

    @Override
    public OrderInfo getOrderInfo(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.selectByOrderNo(orderNo);
        orderInfo.setPayWayName(SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfo.getPayWay())?"账户余额支付" : "微信支付");
        return orderInfo;
    }

    @Override
    public void paySuccess(String orderNo) {
        //根据订单号查询订单信息
        OrderInfo orderInfo = orderInfoMapper.selectByOrderNo(orderNo);
        if(SystemConstant.ORDER_STATUS_PAID.equals(orderInfo.getOrderStatus())){
            //订单状态已经更改过，直接返回
            return;
        }
        //更改订单状态
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
        orderInfoMapper.updateById(orderInfo);

        //发送异步消息，更新支付成功后的状态
        UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
        userPaidRecordVo.setUserId(orderInfo.getUserId());
        userPaidRecordVo.setItemType(orderInfo.getItemType());
        userPaidRecordVo.setOrderNo(orderNo);
        userPaidRecordVo.setItemIdList(orderInfo.getOrderDetailList().stream().map(OrderDetail::getItemId).collect(Collectors.toList()));
        kafkaTemplate.send(KafkaConstant.QUEUE_USER_PAY_RECORD, JSON.toJSONString(userPaidRecordVo));
    }

    private void saveOrder(OrderInfoVo orderInfoVo, Long userId, String orderNo) {
        //保存订单信息
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoVo, orderInfo);
        orderInfo.setUserId(userId);
        orderInfo.setOrderTitle(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
        orderInfo.setOrderNo(orderNo);
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
        orderInfoMapper.insert(orderInfo);

        //保存订单减免信息
        if(!CollectionUtils.isEmpty(orderInfoVo.getOrderDerateVoList())){
            List<OrderDerate> orderDerateList = orderInfoVo.getOrderDerateVoList().stream().map(orderDerateVo -> {
                OrderDerate orderDerate = new OrderDerate();
                BeanUtils.copyProperties(orderDerateVo, orderDerate);
                orderDerate.setOrderId(orderInfo.getId());
                return orderDerate;
            }).collect(Collectors.toList());
            orderDerateService.saveBatch(orderDerateList);
        }

        //保存订单明细信息
        if(!CollectionUtils.isEmpty(orderInfoVo.getOrderDetailVoList())){
            List<OrderDetail> orderDetailList = orderInfoVo.getOrderDetailVoList().stream().map(orderDetailVo -> {
                OrderDetail orderDetail = new OrderDetail();
                BeanUtils.copyProperties(orderDetailVo, orderDetail);
                orderDetail.setOrderId(orderInfo.getId());
                return orderDetail;
            }).collect(Collectors.toList());
            orderDetailService.saveBatch(orderDetailList);
        }

        //判断支付类型
        switch (orderInfoVo.getPayWay()) {
            case SystemConstant.ORDER_PAY_ACCOUNT -> {
                //账户支付，默认支付成功
                //更新账单状态为支付成功
                orderInfo.setOrderStatus(SystemConstant.PAYMENT_STATUS_PAID);
                orderInfoMapper.updateById(orderInfo);
                //向kafka发起请求，更新支付成功后的状态
                UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
                userPaidRecordVo.setUserId(userId);
                userPaidRecordVo.setOrderNo(orderNo);
                userPaidRecordVo.setItemType(orderInfoVo.getItemType());
                userPaidRecordVo.setItemIdList(orderInfoVo.getOrderDetailVoList().stream().map(OrderDetailVo::getItemId).collect(Collectors.toList()));
                //kafkaTemplate.send(KafkaConstant.QUEUE_USER_PAY_RECORD, JSON.toJSONString(userPaidRecordVo));
                //调用用户微服务更新支付成功后的状态
                Result<Void> payRecordResult = userInfoFeignClient.payRecord(userPaidRecordVo);
                Assert.notNull(payRecordResult, "更新用户状态结果集为空");
                if (!ResultCodeEnum.SUCCESS.getCode().equals(payRecordResult.getCode())) {
                    throw new GuiguException(payRecordResult.getCode(), payRecordResult.getMessage());
                }
            }
            case SystemConstant.ORDER_PAY_WAY_WEIXIN, SystemConstant.ORDER_PAY_WAY_ALIPAY ->
                    //向延迟队列中发送延迟消息，如果订单未支付则取消订单
                    sendDelayMessage(orderInfo.getId());
            default -> throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

    private void sendDelayMessage(Long orderId) {
        //向延迟队列中发送延迟消息，如果订单未支付则取消订单
        try{
            RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue(KafkaConstant.QUEUE_ORDER_CANCEL);
            RDelayedQueue<Long> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
            delayedQueue.offerAsync(orderId, KafkaConstant.DELAY_TIME, TimeUnit.SECONDS);
            //delayedQueue.offerAsync(orderId,20, TimeUnit.SECONDS); //测试时使用
            log.info("发送延迟消息成功,订单号：{},延迟时间：{}", orderId, KafkaConstant.DELAY_TIME);
        }catch (Exception e){
            log.error("发送延迟消息失败,订单号：{},延迟时间：{}", orderId, KafkaConstant.DELAY_TIME);
        }

    }
}
