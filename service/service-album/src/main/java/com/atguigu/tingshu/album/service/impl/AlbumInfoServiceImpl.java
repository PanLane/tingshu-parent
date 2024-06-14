package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.*;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	KafkaTemplate<String,String> kafkaTemplate;
	@Autowired
	private AlbumInfoMapper albumInfoMapper;
	@Autowired
	private AlbumStatMapper albumStatMapper;
	@Autowired
	private AlbumAttributeValueMapper albumAttributeValueMapper;
	@Autowired
	private AlbumAttributeValueService albumAttributeValueService;

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
		//设置userId
		albumInfo.setUserId(AuthContextHolder.getUserId()==null?1:AuthContextHolder.getUserId());
		//设置初始化审核通过
		albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
		//如果是付费的专辑则设置前五集为免费试看
		if(!albumInfoVo.getPayType().equals(SystemConstant.ALBUM_PAY_TYPE_FREE)) albumInfo.setTracksForFree(5);
		albumInfoMapper.insert(albumInfo);

		/**********************保存专辑属性值关联信息**********************/
		/*albumInfoVo.getAlbumAttributeValueVoList().forEach(e->{
			AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
			albumAttributeValue.setAlbumId(albumInfo.getId());
			albumAttributeValue.setAttributeId(e.getAttributeId());
			albumAttributeValue.setValueId(e.getValueId());
			albumAttributeValueMapper.insert(albumAttributeValue);
		});*/

		//优化
		List<AlbumAttributeValue> albumAttributeValueList = albumInfoVo.getAlbumAttributeValueVoList().stream().map(e -> {
			AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
			albumAttributeValue.setAlbumId(albumInfo.getId());
			albumAttributeValue.setAttributeId(e.getAttributeId());
			albumAttributeValue.setValueId(e.getValueId());
			return albumAttributeValue;
		}).collect(Collectors.toList());

		albumAttributeValueService.saveBatch(albumAttributeValueList);

		/**********************初始化统计项**********************/
		//统计类型：0701-播放量 0702-收藏量 0703-点赞量 0704-评论数
		saveAlumStat(new AlbumStat(albumInfo.getId(),"0401",0));
		saveAlumStat(new AlbumStat(albumInfo.getId(),"0402",0));
		saveAlumStat(new AlbumStat(albumInfo.getId(),"0403",0));
		saveAlumStat(new AlbumStat(albumInfo.getId(),"0404",0));

		//专辑设置为公开,上架专辑
		if("1".equals(albumInfoVo.getIsOpen())) kafkaTemplate.send(KafkaConstant.QUEUE_ALBUM_UPPER,albumInfo.getId().toString());
	}

	@Override
	public Page<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> page, AlbumInfoQuery albumInfoQuery) {
		return albumInfoMapper.selectUserAlbumPage(page,albumInfoQuery);
	}

	@Override
	@Transactional
	public void removeAlbumInfo(Long albumId) {
		//删除专辑信息
		albumInfoMapper.deleteById(albumId);
		//删除专辑统计
		albumStatMapper.delete(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId,albumId));
		//删除专辑属性值关联表
		albumAttributeValueMapper.delete(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId,albumId));
	}

	@Override
	public AlbumInfo getAlbumInfo(Long albumId) {
		//根据id获取专辑信息
		AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
		//根据id获取专辑属性值关联信息
		LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<AlbumAttributeValue>()
				.eq(AlbumAttributeValue::getAlbumId, albumId)
				.select(AlbumAttributeValue::getAttributeId,AlbumAttributeValue::getValueId);
		List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueMapper.selectList(wrapper);
		albumInfo.setAlbumAttributeValueVoList(albumAttributeValueList);
		return albumInfo;
	}

	@Override
	@Transactional
	public void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo) {
		//删除专辑属性值关联表
		albumAttributeValueMapper.delete(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId,albumId));
		//修改专辑信息
		AlbumInfo albumInfo = new AlbumInfo();
		BeanUtils.copyProperties(albumInfoVo,albumInfo);
		albumInfo.setId(albumId);
		//如果是付费的专辑则设置前五集为免费试看
		if(!albumInfoVo.getPayType().equals(SystemConstant.ALBUM_PAY_TYPE_FREE)) albumInfo.setTracksForFree(5);
		albumInfoMapper.updateById(albumInfo);
		//保存专辑属性值关联信息
		List<AlbumAttributeValue> albumAttributeValueList = albumInfoVo.getAlbumAttributeValueVoList().stream().map(e -> {
			AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
			albumAttributeValue.setAlbumId(albumInfo.getId());
			albumAttributeValue.setAttributeId(e.getAttributeId());
			albumAttributeValue.setValueId(e.getValueId());
			return albumAttributeValue;
		}).collect(Collectors.toList());
		if("1".equals(albumInfoVo.getIsOpen())){
			//专辑设置为公开,上架专辑
			kafkaTemplate.send(KafkaConstant.QUEUE_ALBUM_UPPER,albumId.toString());
		}else {
			//专辑设置为私有,下架专辑
			kafkaTemplate.send(KafkaConstant.QUEUE_ALBUM_LOWER,albumId.toString());
		}
		albumAttributeValueService.saveBatch(albumAttributeValueList);
	}

	@Override
	public List<AlbumInfo> findUserAllAlbumList() {
		Long userId = AuthContextHolder.getUserId() == null ? 1 : AuthContextHolder.getUserId();
		Wrapper<AlbumInfo> wrapper = new LambdaQueryWrapper<AlbumInfo>()
				.eq(AlbumInfo::getUserId,userId)
				.orderByDesc(AlbumInfo::getId).last("limit 20");
		return albumInfoMapper.selectList(wrapper);
	}

	@Override
	public AlbumStatVo getAlbumStatVo(Long albumId) {
		return albumStatMapper.selectAlbumStatVo(albumId);
	}

	private void saveAlumStat(AlbumStat albumStat){
		albumStatMapper.insert(albumStat);
	}

}
