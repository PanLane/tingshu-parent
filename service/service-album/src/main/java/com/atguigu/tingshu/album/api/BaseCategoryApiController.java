package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album/category")
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryApiController {
	
	@Autowired
	private BaseCategoryService baseCategoryService;

	@Operation(summary = "获取分类数据")
	@GetMapping("/getBaseCategoryList")
	public Result<List<JSONObject>> getBaseCategoryList(){
		List<JSONObject> list = baseCategoryService.getBaseCategoryList();
		return Result.ok(list);
	}

	@Operation(summary = "根据一级分类id获取全部分类数据")
	@GetMapping("/getBaseCategoryList/{category1Id}")
	public Result<JSONObject> getBaseCategoryList(@PathVariable Long category1Id){
		JSONObject jsonObject = baseCategoryService.getBaseCategoryList(category1Id);
		return Result.ok(jsonObject);
	}

	//http://127.0.0.1:8515/api/album/category/findAttribute/2
	@Operation(summary = "根据一级分类id获取专辑属性")
	@GetMapping("/findAttribute/{categoryId}")
	public Result<List<BaseAttribute>> findAttribute(@PathVariable Long categoryId) {
		return Result.ok(baseCategoryService.findAttribute(categoryId));
	}

	@Operation(summary = "根据三级分类Id获取到分类信息")
	@GetMapping("/getCategoryView/{category3Id}")
	public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id) {
		return Result.ok(baseCategoryService.getCategoryView(category3Id));
	}

	@Operation(summary = "根据一级分类Id查询三级分类列表")
	@GetMapping("/findTopBaseCategory3/{category1Id}")
	public Result<List<BaseCategory3>> findTopBaseCategory3(@PathVariable Long category1Id){
		return Result.ok(baseCategoryService.findTopBaseCategory3(category1Id));
	}

	@Operation(summary = "查询所有一级分类信息")
	@GetMapping("/findAllCategory1")
	public Result<List<BaseCategory1>> findAllCategory1(){
		List<BaseCategory1> list = baseCategoryService.findAllCategory1();
		return Result.ok(list);
	}
}

