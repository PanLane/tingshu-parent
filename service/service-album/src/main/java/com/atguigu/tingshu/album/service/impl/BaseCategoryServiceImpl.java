package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.base.BaseEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
	@Autowired
	private BaseAttributeMapper baseAttributeMapper;


	@Override
	public List<JSONObject> getBaseCategoryList() {

		List<JSONObject> list = new ArrayList<>();//结果

		//获取分类视图数据源
		List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);

		//获取一级分类数据源
		Map<Long, List<BaseCategoryView>> baseCategoryViews1 = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
		//遍历分组后的数据源
		baseCategoryViews1.forEach((k,v)->{

			//创建一级分类对象
			JSONObject category1 = new JSONObject();
			//将一级分类id、一级分类名称添加到一级分类对象中
			category1.put("categoryId",k);
			category1.put("categoryName",v.get(0).getCategory1Name());
			//创建一级分类对象的categoryChild
			List<JSONObject> categoryChild1 = new ArrayList<>();

			//获取二级分类数据源
			Map<Long, List<BaseCategoryView>> baseCategoryViews2 = v.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
			//遍历二级分类数据源
			baseCategoryViews2.forEach((k2,v2)->{
				//创建二级分类对象
				JSONObject category2 = new JSONObject();
				//将二级分类id、名称、categoryChild添加到二级分类对象中
				category2.put("categoryId",k2);
				category2.put("categoryName",v2.get(0).getCategory2Name());
				category2.put("categoryChild",v2.stream().map(e->{
					JSONObject category3 = new JSONObject();
					category3.put("categoryId",e.getCategory3Id());
					category3.put("categoryName",e.getCategory3Name());
					return category3;
				}));

				//将二级分类对象添加到一级分类对象的categoryChild中
				categoryChild1.add(category2);

			});

			//将categoryChild添加到一级分类对象中
			category1.put("categoryChild",categoryChild1);

			//将一级分类对象添加到list中
			list.add(category1);
		});

		//返回list
		return list;
	}

	@Override
	public List<BaseAttribute> findAttribute(Long categoryId) {
		return baseAttributeMapper.selectAttribute(categoryId);
	}

	@Override
	public BaseCategoryView getCategoryView(Long category3Id) {
		return baseCategoryViewMapper.selectOne(new LambdaQueryWrapper<BaseCategoryView>().eq(BaseCategoryView::getCategory3Id,category3Id));
	}

	@Override
	public JSONObject getBaseCategoryList(Long category1Id) {
		//获取分类视图数据源
		List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(new LambdaQueryWrapper<BaseCategoryView>().eq(BaseCategoryView::getCategory1Id,category1Id));

		//创建一级分类对象
		JSONObject jsonObject = new JSONObject();
		//封装数据
		jsonObject.put("categoryName",baseCategoryViews.get(0).getCategory1Name());
		jsonObject.put("categoryId",baseCategoryViews.get(0).getCategory1Id());
		ArrayList<JSONObject> categoryChild = new ArrayList<>();
		jsonObject.put("categoryChild",categoryChild);

		//根据二级分类id分组去重
		Map<Long, List<BaseCategoryView>> baseCategory2Map = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
		baseCategory2Map.forEach((k,v)->{

			//创建二级分类对象
			JSONObject jsonObject2 = new JSONObject();
			//封装数据
			jsonObject2.put("categoryName",v.get(0).getCategory2Name());
			jsonObject2.put("categoryId",k);
			jsonObject2.put("categoryChild",v.stream().map(e->{
				JSONObject jsonObject3 = new JSONObject();
				jsonObject3.put("categoryName",e.getCategory3Name());
				jsonObject3.put("categoryId",e.getCategory3Id());
				return jsonObject3;
			}).collect(Collectors.toList()));

			//将二级分类对象添加到一级分类对象的categoryChild中
			categoryChild.add(jsonObject2);
		});

		//返回数据
		return jsonObject;
	}
}
