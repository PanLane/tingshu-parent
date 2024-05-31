package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;
	@Autowired
	private AlbumAttributeValueMapper albumAttributeValueMapper;
	@Autowired
	private AlbumStatMapper albumStatMapper;

	@Override
	@Transactional
	public void saveAlbumInfo(AlbumInfoVo albumInfoVo) {
		/**
		 * album_info 专辑表
		 *
		 * 	a. 初始化userId 默认值1 为了后续能查到数据
		 *
		 * 	b. 并设置初始化状态为审核通过
		 *
		 * 	c. 如果**是付费的专辑**则设置前五集为免费试看
		 *
		 * album_attribute_value 专辑属性值表
		 *
		 * 	a. 设置专辑Id
		 *
		 * album_stat 专辑统计表
		 *
		 * 	a. 初始化统计项 【播放量，订阅量，购买量，评论数】为0
		 */

		/**********************保存专辑信息**********************/
		AlbumInfo albumInfo = new AlbumInfo();
		BeanUtils.copyProperties(albumInfoVo,albumInfo);
		//设置userId为默认值1
		albumInfo.setUserId(1L);
		//设置初始化审核通过
		albumInfo.setStatus("0301");
		//如果是付费的专辑则设置前五集为免费试看
		if(!albumInfoVo.getPayType().equals("0101")) albumInfo.setTracksForFree(5);
		System.out.println(albumInfoMapper.insert(albumInfo));

		/**********************保存专辑属性值关联信息**********************/
		albumInfoVo.getAlbumAttributeValueVoList().forEach(e->{
			AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
			albumAttributeValue.setAlbumId(albumInfo.getId());
			albumAttributeValue.setAttributeId(e.getAttributeId());
			albumAttributeValue.setValueId(e.getValueId());
			albumAttributeValueMapper.insert(albumAttributeValue);
		});

		/**********************初始化统计项**********************/
		//统计类型：0701-播放量 0702-收藏量 0703-点赞量 0704-评论数
		saveAlumStat(new AlbumStat(albumInfo.getId(),"0701",0));
		saveAlumStat(new AlbumStat(albumInfo.getId(),"0702",0));
		saveAlumStat(new AlbumStat(albumInfo.getId(),"0703",0));
		saveAlumStat(new AlbumStat(albumInfo.getId(),"0704",0));
	}

	private void saveAlumStat(AlbumStat albumStat){
		albumStatMapper.insert(albumStat);
	}

}
