package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.TrackStatService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;
	@Autowired
	private VodConstantProperties vodConstantProperties;
	@Autowired
	private VodUploadClient vodUploadClient;
	@Autowired
	private VodService vodService;
	@Autowired
	private TrackStatService trackStatService;
	@Autowired
	private AlbumInfoMapper albumInfoMapper;
	@Autowired
	private UserInfoFeignClient userInfoFeignClient;

	@Override
	public Map<String,String> uploadTrack(MultipartFile file) {
		VodUploadRequest request = new VodUploadRequest();
		try {
			HashMap<String, String> map = new HashMap<>();
			String tempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
			request.setMediaFilePath(tempPath);
			VodUploadResponse response = vodUploadClient.upload(vodConstantProperties.getRegion(), request);
			log.info("Upload FileId = {}", response.getFileId());
			map.put("mediaFileId",response.getFileId());
			map.put("mediaUrl", response.getMediaUrl());
			return map;
		} catch (Exception e) {
			e.printStackTrace();
			// 业务方进行异常处理
			log.error("Upload Err", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	@Transactional
	public void saveTrackInfo(TrackInfoVo trackInfoVo)  {
		//保存声音信息
		TrackInfo trackInfo = new TrackInfo();
		BeanUtils.copyProperties(trackInfoVo,trackInfo);
		trackInfo.setUserId(AuthContextHolder.getUserId()==null?1l:AuthContextHolder.getUserId());//userId
		trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);//status
		TrackInfo orderNumObj = trackInfoMapper.selectOne(
				new LambdaQueryWrapper<TrackInfo>()
						.eq(TrackInfo::getAlbumId, trackInfoVo.getAlbumId())
						.orderByDesc(TrackInfo::getOrderNum)
						.select(TrackInfo::getOrderNum)
						.last("limit 1")
		);
		trackInfo.setOrderNum(orderNumObj==null?1:orderNumObj.getOrderNum()+1);//orderNum
		TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
		trackInfo.setMediaUrl(mediaInfo.getMediaUrl());
		trackInfo.setMediaType(mediaInfo.getType());
		trackInfo.setMediaSize(mediaInfo.getSize());
		trackInfo.setMediaDuration(mediaInfo.getDuration());
		trackInfoMapper.insert(trackInfo);

		//保存声音统计信息
		Long trackId = trackInfo.getId();
		ArrayList<TrackStat> list = new ArrayList<>();
		list.add(getTrackStat(trackId,SystemConstant.TRACK_STAT_PLAY));
		list.add(getTrackStat(trackId,SystemConstant.TRACK_STAT_COLLECT));
		list.add(getTrackStat(trackId,SystemConstant.TRACK_STAT_PRAISE));
		list.add(getTrackStat(trackId,SystemConstant.TRACK_STAT_COMMENT));
		trackStatService.saveBatch(list);

		//更新专辑声音总数
		albumInfoMapper.incrIncludeTraceCount(trackInfoVo.getAlbumId());
	}

	@Override
	public Page<TrackListVo> findUserTrackPage(Page<TrackInfoVo> page, TrackInfoQuery trackInfoQuery) {
		return trackInfoMapper.selectUserTrackPage(page,trackInfoQuery);
	}

	@Override
	@Transactional
	public void removeTrackInfo(Long id) {
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		//更新声音信息序号
		trackInfoMapper.updateOrderNum(trackInfo.getAlbumId(),trackInfo.getId());
		//删除声音信息
		trackInfoMapper.deleteById(id);
		//删除声音统计信息
		trackStatService.remove(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId,id));
		//专辑信息声音总数减一
		albumInfoMapper.decrIncludeTraceCount(trackInfo.getAlbumId());
		//删除云点播中的声音
		vodService.deleteMedia(trackInfo.getMediaFileId());
	}

	@Override
	public void updateTrackInfo(Long id, TrackInfoVo trackInfoVo) {
		//创建声音实体类
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		//判断是否修改了上传的声音
		if(!trackInfoVo.getMediaFileId().equals(trackInfo.getMediaFileId())){
			//修改了，更新媒体数据
			TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
			trackInfo.setMediaUrl(mediaInfo.getMediaUrl());
			trackInfo.setMediaType(mediaInfo.getType());
			trackInfo.setMediaSize(mediaInfo.getSize());
			trackInfo.setMediaDuration(mediaInfo.getDuration());
			//删除云点播中的声音
			vodService.deleteMedia(trackInfo.getMediaFileId());
		}
		//属性拷贝
		BeanUtils.copyProperties(trackInfoVo,trackInfo);
		//修改声音信息
		trackInfoMapper.updateById(trackInfo);
	}

	private TrackStat getTrackStat(Long trackId,String trackStatType) {
		TrackStat trackStat = new TrackStat();
		trackStat.setStatType(trackStatType);
		trackStat.setTrackId(trackId);
		trackStat.setStatNum(0);
		return trackStat;
	}

	@Override
	public IPage<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> page, Long albumId) {
		//声明用户信息变量
		UserInfoVo userInfoVo = null;

		//根据专辑id查询TrackListVo列表
		List<AlbumTrackListVo> albumTrackListVoList = trackInfoMapper.selectAlbumTrackPage(page,albumId).getRecords();

		//根据专辑id获取专辑信息
		AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
		//获取专辑付费类型
		String payType = albumInfo.getPayType();

		boolean isFree = true;//是否免费

		//判断专辑是否需要付费
		if(SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(payType)){//专辑支付类型为vip免费
			//从用户上下文中获取用户id
			Long userId = AuthContextHolder.getUserId();
			//如果用户id不为null，调用用户微服务，根据用户id获取用户信息
			if(userId != null){
				Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoById(userId);
				Assert.notNull(userInfoVoResult,"用户信息结果集为空");
				userInfoVo = userInfoVoResult.getData();
			}
			//专辑支付类型为会员免费，判断用户是否登录并且为会员（未过期）
			if(userInfoVo == null){
				//用户没登录，
				albumTrackListVoList.stream().filter(e->e.getOrderNum()>albumInfo.getTracksForFree()).forEach(e->e.setIsShowPaidMark(true));
				return page;
			}else if(!(userInfoVo.getIsVip() == 1 && userInfoVo.getVipExpireTime().after(new Date()))){
				//用户不是会员状态 将isFree设置为false
				isFree = false;
			}
		}else if(SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(payType)){
			//专辑支付类型为收费，将isFree设置为false
			isFree = false;
		}

		//统一处理收费结果
		if(!isFree){
			//获取需要可能需要收费的声音id集合
			List<Long> mayNeedPaidTrackIdList = albumTrackListVoList.stream().filter(trackListVo -> trackListVo.getOrderNum() > albumInfo.getTracksForFree()).map(AlbumTrackListVo::getTrackId).collect(Collectors.toList());
			//调用用户微服务，根据用户是否够买专辑或声音得到最终需要收费的专辑id集合
			Result<List<Long>> notFreeTrackIdListResult = userInfoFeignClient.getNotFreeTrackIdList(albumId,mayNeedPaidTrackIdList);
			Assert.notNull(notFreeTrackIdListResult,"需要收费的声音结果集为空");
			List<Long> notFreeTrackIdList = notFreeTrackIdListResult.getData();
			albumTrackListVoList.stream().filter(e->e.getOrderNum()>albumInfo.getTracksForFree())
					.forEach(e->e.setIsShowPaidMark(notFreeTrackIdList != null && notFreeTrackIdList.contains(e.getTrackId())));
		}

		//返回数据
		return page;
	}
}
