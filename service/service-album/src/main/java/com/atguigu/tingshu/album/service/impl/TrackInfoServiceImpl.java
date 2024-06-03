package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.TrackStatService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.vod.v20180717.VodClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	public Page<TrackListVo> findAlbumTrackPage(Page<TrackInfoVo> page, TrackInfoQuery trackInfoQuery) {
		return trackInfoMapper.selectAlbumTrackPage(page,trackInfoQuery);
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
}
