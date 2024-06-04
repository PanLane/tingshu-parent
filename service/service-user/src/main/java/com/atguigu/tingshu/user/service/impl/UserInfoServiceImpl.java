package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
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
}
