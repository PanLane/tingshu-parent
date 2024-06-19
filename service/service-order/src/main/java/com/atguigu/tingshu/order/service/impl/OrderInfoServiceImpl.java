package com.atguigu.tingshu.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.user.client.VipServiceConfigFeignClient;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.order.*;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

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


    @Override
    public OrderInfoVo trade(TradeVo tradeVo) {
        //接收数据
        Long itemId = tradeVo.getItemId();
        //创建对象、定义变量
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        BigDecimal originalAmount = new BigDecimal("0");//原始金额
        BigDecimal derateAmount = new BigDecimal("0");//减免总金额
        BigDecimal orderAmount = new BigDecimal("0");//订单总价
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();//订单明细列表
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();//订单减免明细列表

        switch (tradeVo.getItemType()){
            case SystemConstant.ORDER_ITEM_TYPE_ALBUM: //购买专辑

                //判断用户是否已购买改专辑、如果已购买，抛异常
                Result<Boolean> paidAlbumResult = userInfoFeignClient.isPaidAlbum(itemId);
                Assert.notNull(paidAlbumResult,"用户是否购买专辑结果集为空");
                Boolean paidAlbum = paidAlbumResult.getData();
                Assert.notNull(paidAlbum,"用户是否购买专辑结果为空");
                if(paidAlbum) throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);

                //根据专辑id获取专辑信息
                Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(itemId);
                Assert.notNull(albumInfoResult,"专辑信息结果集为空");
                AlbumInfo albumInfo = albumInfoResult.getData();
                Assert.notNull(albumInfo,"专辑信息为空");

                //根据用户id获取用户信息
                Result<UserInfoVo> userInfoResult = userInfoFeignClient.getUserInfoById(albumInfo.getUserId());
                Assert.notNull(userInfoResult,"用户信息结果集为空");
                UserInfoVo userInfoVo = userInfoResult.getData();
                Assert.notNull(userInfoVo,"用户信息为空");

                //给变量赋值
                originalAmount = albumInfo.getPrice();//原始金额
                //判断用户是否是vip状态
                if(userInfoVo.getIsVip() == 1 && new Date().before(userInfoVo.getVipExpireTime())){
                    //用户是vip状态，享受vip折扣
                    //判断是否打折
                    if(albumInfo.getVipDiscount().compareTo(new BigDecimal("-1")) != 0){
                        //打折，减免总金额 = 专辑价格 * (10-折扣)/10
                        derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getVipDiscount()).divide(new BigDecimal("10"),2,BigDecimal.ROUND_HALF_UP));
                        //订单总价 = 专辑价格 - 减免总金额
                        orderAmount = originalAmount.subtract(derateAmount);
                    }else {
                        //不打折，订单金额等于原始金额
                        orderAmount = originalAmount;
                    }
                }else {
                    //用户不是vip状态，享受普通折扣
                    //判断是否打折
                    if(albumInfo.getDiscount().compareTo(new BigDecimal("-1")) != 0){
                        //打折，减免总金额 = 专辑价格 * (10-折扣)/10
                        derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getDiscount()).divide(new BigDecimal("10"),2,BigDecimal.ROUND_HALF_UP));
                        //订单总价 = 专辑价格 - 减免总金额
                        orderAmount = originalAmount.subtract(derateAmount);
                    }else {
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
                if(derateAmount.compareTo(new BigDecimal("0"))!=0){
                    OrderDerateVo orderDerateVo = new OrderDerateVo();
                    orderDerateVo.setDerateAmount(derateAmount);
                    orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                    orderDerateVoList.add(orderDerateVo);//将订单减免明细添加到订单减免明细集合中
                }
                break;
            case SystemConstant.ORDER_ITEM_TYPE_TRACK: //购买声音
                //校验参数
                if(tradeVo.getTrackCount()<=0) throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
                //根据声音id、声音数量，获取用户下单付费声音列表
                Result<List<TrackInfo>> paidTrackInfoListResult = trackInfoFeignClient.findPaidTrackInfoList(itemId, tradeVo.getTrackCount());
                Assert.notNull(paidTrackInfoListResult,"用户下单付费声音列表结果集为空");
                List<TrackInfo> paidTrackInfoList = paidTrackInfoListResult.getData();
                Assert.notNull(paidTrackInfoList,"用户下单付费声音列表为空");
                //调用专辑客户端，根据id获取专辑信息
                Result<AlbumInfo> albumInfoResult2 = albumInfoFeignClient.getAlbumInfo(paidTrackInfoList.get(0).getAlbumId());
                Assert.notNull(albumInfoResult2,"专辑信息结果集为空");
                AlbumInfo albumInfo2 = albumInfoResult2.getData();
                Assert.notNull(albumInfo2,"专辑信息为空");
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
                Assert.notNull(vipServiceConfigResult,"vip服务配置结果集为空");
                VipServiceConfig vipServiceConfig = vipServiceConfigResult.getData();
                Assert.notNull(vipServiceConfig,"vip服务配置信息为空");
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
                if(derateAmount.compareTo(new BigDecimal("0"))!=0){
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
        String key = "tradeNo:"+ AuthContextHolder.getUserId()+ ":" + tradeNo;
        redisTemplate.opsForValue().set(key,tradeNo);

        //返回数据
        return orderInfoVo;
    }
}
