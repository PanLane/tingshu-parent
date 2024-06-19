package com.atguigu.tingshu.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.core.util.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private KafkaTemplate kafkaTemplate;
	@Autowired
	private RedisTemplate redisTemplate;

	@Override
	public BigDecimal getTrackBreakSecond(Long trackId) {
		//获取用户id
		Long userId = AuthContextHolder.getUserId();
		if(userId == null) return new BigDecimal(0);//用户未登录，返回0
		Query query = new Query(Criteria.where("trackId").is(trackId));
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		return userListenProcess == null ? new BigDecimal(0) : userListenProcess.getBreakSecond();
	}

	@Override
	public void updateListenProcess(UserListenProcessVo userListenProcessVo) {
		//获取用户id
		Long userId = AuthContextHolder.getUserId();
		if(userId == null) return; //用户未登录，不做处理

		//获取数据
		Long albumId = userListenProcessVo.getAlbumId();
		Long trackId = userListenProcessVo.getTrackId();

		//从mongodb中获取用户播放进度信息
		Query query = new Query(Criteria.where("trackId").is(trackId));
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));

		if(userListenProcess == null) {
			//用户播放进度为null，新增
			userListenProcess = new UserListenProcess();
			userListenProcess.setId(UUID.randomUUID().toString().replace("-",""));
			userListenProcess.setUserId(userId);
			userListenProcess.setAlbumId(albumId);
			userListenProcess.setTrackId(trackId);
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setIsShow(1);
			userListenProcess.setUpdateTime(new Date());
			userListenProcess.setCreateTime(new Date());
		}else {
			//用户播放进度不为null，修改
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setUpdateTime(new Date());
		}
		mongoTemplate.save(userListenProcess, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));

		//判断用户是否在一天内已更新过播放量
		TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
		trackStatMqVo.setTrackId(trackId);
		trackStatMqVo.setAlbumId(albumId);
		trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replace("-",""));
		trackStatMqVo.setCount(1);
		trackStatMqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
		Boolean updated = redisTemplate.opsForValue().setIfAbsent(RedisConstant.ALBUM_STAT_ENDTIME+userId+"-"+trackId, trackStatMqVo, 1, TimeUnit.DAYS);
		if(Boolean.TRUE.equals(updated)){
			//没有更新过，向kafka中发送消息，专辑播放量+1，声音播放量+1
			kafkaTemplate.send(KafkaConstant.QUEUE_TRACK_STAT_UPDATE, JSON.toJSONString(trackStatMqVo));
		}
	}

	@Override
	public Map<String, Object> getLatelyTrack() {
		//获取用户id
		Long userId = AuthContextHolder.getUserId();
		//用户未登录，返回null
		if(userId==null) return null;
		//从mongodb中获取用户最近一次播放记录
		Query query = new Query().with(Sort.by(Sort.Direction.DESC,"updateTime"));
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		//封装并返回数据
		if(userListenProcess == null) return null;
		Map<String,Object> map = new HashMap<>();
		map.put("trackId",userListenProcess.getTrackId());
		map.put("albumId",userListenProcess.getAlbumId());
		return map;
	}
}
