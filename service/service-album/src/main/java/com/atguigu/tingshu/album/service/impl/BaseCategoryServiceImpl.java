package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.BaseCategory1Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategory2Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategory3Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategoryViewMapper;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.base.BaseEntity;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

	@Autowired
	private BaseCategory1Mapper baseCategory1Mapper;

	@Autowired
	private BaseCategory2Mapper baseCategory2Mapper;

	@Autowired
	private BaseCategory3Mapper baseCategory3Mapper;

	@Autowired
	private BaseCategoryViewMapper baseCategoryViewMapper;


	@Override
	public List<JSONObject> getBaseCategoryList() {

		List<JSONObject> list = new ArrayList<>();//结果

		//获取分类视图数据
		List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);

		//获取一级分类数据
		//将数据源进行分组
		Map<Long, List<BaseCategoryView>> category1 = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseEntity::getId));
		//遍历分组后的数据源
		category1.forEach((k,v)->{
			//
		});


		return list;
	}
}
