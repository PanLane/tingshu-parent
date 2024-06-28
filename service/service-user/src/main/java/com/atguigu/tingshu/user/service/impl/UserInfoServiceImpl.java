package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.atguigu.tingshu.user.stragety.PaymentStrategy;
import com.atguigu.tingshu.user.stragety.impl.PaymentStrategyFactory;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

	@Autowired
	private UserInfoMapper userInfoMapper;
	@Autowired
	private WxMaService wxMaService;
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private KafkaTemplate<String,String> kafkaTemplate;
	@Autowired
	private UserPaidTrackMapper userPaidTrackMapper;
	@Autowired
	private UserPaidAlbumMapper userPaidAlbumMapper;
	@Autowired
	private TrackInfoFeignClient trackInfoFeignClient;
	@Autowired
	private UserPaidTrackService userPaidTrackService;
	@Autowired
	private UserVipServiceMapper userVipServiceMapper;
	@Autowired
	private VipServiceConfigMapper vipServiceConfigMapper;
	@Autowired
	private PaymentStrategyFactory paymentStrategyFactory;

	@Override
	@Transactional
	public Map<String, Object> wxLogin(String code) throws WxErrorException {
		//获取openid
		WxMaJscode2SessionResult result = wxMaService.jsCode2SessionInfo(code);
		String openid = result.getOpenid();

		//根据openid查找用户信息
		UserInfo userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getWxOpenId, openid));
		if(userInfo==null){
			//用户没有注册，进行注册
			userInfo = new UserInfo();
			userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
			userInfo.setNickname("听友："+System.currentTimeMillis());
			userInfo.setWxOpenId(openid);
			userInfoMapper.insert(userInfo);

			//初始化用户的账户信息(向kafka中发送一条消息)
			CompletableFuture completableFuture = kafkaTemplate.send(KafkaConstant.QUEUE_USER_REGISTER,userInfo.getId().toString());
			//确认消息发送成功
			completableFuture.whenComplete((o, throwable) -> {
				if(throwable!=null){
					log.error("向kafka发送消息失败："+throwable);
				}else {
					log.info("向kafka发送消息成功!");
				}
			});
		}

		//生成uuid
		String uuid = UUID.randomUUID().toString().replace("-", "");
		//在缓存中存储token
		redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX+uuid,userInfo,RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);

		//返回token
		Map<String,Object> map = new HashMap();
		map.put("token",RedisConstant.USER_LOGIN_KEY_PREFIX+uuid);
		return map;
	}

	@Override
	public UserInfoVo getUserInfo(Long userId) {
		UserInfo userInfo = userInfoMapper.selectById(userId);
		UserInfoVo userInfoVo = new UserInfoVo();
		BeanUtils.copyProperties(userInfo,userInfoVo);
		return userInfoVo;
	}

	@Override
	public void updateUser(UserInfoVo userInfoVo) {
		UserInfo userInfo = new UserInfo();
		BeanUtils.copyProperties(userInfoVo,userInfo);
		userInfoMapper.updateById(userInfo);
	}

	@Override
	public List<Long> getNotFreeTrackIdList(Long albumId, List<Long> mayNeedPaidTrackIdList) {
		//从用户登录上下文对象中获取用户id
		Long userId = AuthContextHolder.getUserId();
		Wrapper<UserPaidAlbum> albumWrapper = new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getUserId, userId).eq(UserPaidAlbum::getAlbumId, albumId);
		//根据专辑id和用户id查询用户已购买的专辑
		UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(albumWrapper);

		if(userPaidAlbum!=null){
			//用户已购买过该专辑，返回null
			return null;
		}

		//根据专辑id和用户id查询用户已购买的声音
		List<Long> userPaidTrackList = userPaidTrackMapper.selectPaidTrackIdtList(albumId,userId);
		//用可能需要购买的声音id集合减去用户已购买的声音id集合
		mayNeedPaidTrackIdList.removeAll(userPaidTrackList);
		//返回数据
		return mayNeedPaidTrackIdList;
	}

	@Override
	public Boolean isPaidAlbum(Long albumId) {
		return userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>()
				.eq(UserPaidAlbum::getAlbumId, albumId)
				.eq(UserPaidAlbum::getUserId, AuthContextHolder.getUserId())) != null;
	}

	@Override
	public List<Long> getPaidTrackIdList(Long albumId) {
		return userPaidTrackMapper.selectPaidTrackIdtList(albumId,AuthContextHolder.getUserId());
	}

	@Override
	@Transactional
	public void updateUserPaidRecord(UserPaidRecordVo userPaidRecordVo) throws Exception {

		//获取数据
		List<Long> itemIdList = userPaidRecordVo.getItemIdList();
		Long userId = userPaidRecordVo.getUserId();
		String orderNo = userPaidRecordVo.getOrderNo();

		//使用工厂加策略模式对以下代码进行优化
		/*switch (userPaidRecordVo.getItemType()){
			case SystemConstant.ORDER_ITEM_TYPE_ALBUM: //用户购买的是专辑
				//根据用户id，订单号获取用户已购买的专辑
				UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>()
						.eq(UserPaidAlbum::getUserId, userId)
						.eq(UserPaidAlbum::getOrderNo, orderNo));
				if(userPaidAlbum!=null){
					//已经更新过，直接返回
					return;
				}
				userPaidAlbum = new UserPaidAlbum();
				userPaidAlbum.setUserId(userId);
				userPaidAlbum.setAlbumId(itemIdList.get(0));
				userPaidAlbum.setOrderNo(orderNo);
				userPaidAlbumMapper.insert(userPaidAlbum);
				break;
			case SystemConstant.ORDER_ITEM_TYPE_TRACK: //用户购买的是声音
				//根据用户id，订单号获取用户已购买的声音数量
				int count = userPaidTrackMapper.selectPaidTrackCount();
				if(count>0){
					//已经更新过，直接返回
					return;
				}
				//调用声音微服务客户端，根据声音id获取专辑id
				Result<Long> albumIdResult= trackInfoFeignClient.getAlbumIdByTrackId(itemIdList.get(0));
				Assert.notNull(albumIdResult,"专辑id结果集为空");
				Long albumId = albumIdResult.getData();
				Assert.notNull(albumId,"专辑id为空");

				List<UserPaidTrack> userPaidTrackList = itemIdList.stream().map(itemId -> {
					UserPaidTrack userPaidTrack = new UserPaidTrack();
					userPaidTrack.setUserId(userId);
					userPaidTrack.setOrderNo(orderNo);
					userPaidTrack.setAlbumId(albumId);
					userPaidTrack.setTrackId(itemId);
					return userPaidTrack;
				}).collect(Collectors.toList());
				//批量保存
				userPaidTrackService.saveBatch(userPaidTrackList);
				break;
			case SystemConstant.ORDER_ITEM_TYPE_VIP: //用户购买的是VIP
				//根据用户id，订单号获取用户vip服务记录
				UserVipService userVipService = userVipServiceMapper.selectOne(new LambdaQueryWrapper<UserVipService>()
						.eq(UserVipService::getUserId, userId)
						.eq(UserVipService::getOrderNo, orderNo));
				if(userVipService!=null){
					//已经更新过，直接返回
					return;
				}

				userVipService = new UserVipService();

				//更新用户的过期时间
				//根据id获取vip服务配置信息
				VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(itemIdList.get(0));
				//根据id获取用户信息
				UserInfo userInfo = userInfoMapper.selectById(userId);
				//判断当前用户是否处于vip状态
				if(userInfo.getIsVip()==1 && userInfo.getVipExpireTime().after(new Date())){
					//设置vip服务记录的开始时间
					userVipService.setStartTime(userInfo.getVipExpireTime());
					//用户处于vip状态，续期
					userInfo.setVipExpireTime(new DateTime(userInfo.getVipExpireTime()).plusMonths(vipServiceConfig.getServiceMonth()).toDate());
					//设置vip服务记录的过期时间
					userVipService.setExpireTime(userInfo.getVipExpireTime());
				}else {
					//用户不是vip状态，设置vip过期时间
					userInfo.setVipExpireTime(new DateTime().plusMonths(vipServiceConfig.getServiceMonth()).toDate());
					//设置vip服务记录的开始时间和过期时间
					userVipService.setStartTime(new Date());
					userVipService.setExpireTime(userInfo.getVipExpireTime());
				}

				//更新vip服务记录和用户信息
				userInfo.setIsVip(1);
				userVipService.setOrderNo(orderNo);
				userVipService.setUserId(userId);
				userInfoMapper.updateById(userInfo);
				userVipServiceMapper.insert(userVipService);
				break;
			default:
				throw new GuiguException(ResultCodeEnum.DATA_ERROR);
		}*/
		PaymentStrategy strategy = paymentStrategyFactory.getStrategy(userPaidRecordVo.getItemType());
		strategy.processPayment(userId,orderNo,itemIdList);
	}

	@Override
	public void updateUserVipStatus() {
		userInfoMapper.updateUserVipStatus();
	}
}
