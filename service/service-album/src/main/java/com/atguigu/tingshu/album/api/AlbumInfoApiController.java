package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoApiController {

	@Autowired
	private AlbumInfoService albumInfoService;
	@Autowired
	private AlbumAttributeValueService albumAttributeValueService;

	//http://127.0.0.1:8515/api/album/albumInfo/saveAlbumInfo
	@Operation(summary = "保存专辑信息")
	@PostMapping("/saveAlbumInfo")
	public Result<Void> saveAlbumInfo(@Validated @RequestBody AlbumInfoVo albumInfoVo) {
		albumInfoService.saveAlbumInfo(albumInfoVo);
		return Result.ok();
	}

	//http://127.0.0.1:8515/api/album/albumInfo/findUserAlbumPage/1/10
	@TsLogin
	@Operation(summary = "分页条件查询专辑信息")
	@PostMapping("/findUserAlbumPage/{pageNum}/{pageSize}")
	public Result<Page<AlbumListVo>> findUserAlbumPage(@PathVariable Integer pageNum, @PathVariable Integer pageSize, @RequestBody AlbumInfoQuery albumInfoQuery){
		//albumInfoQuery.setUserId(AuthContextHolder.getUserId());
		albumInfoQuery.setUserId(1L);
		return Result.ok(albumInfoService.findUserAlbumPage(new Page<>(pageNum,pageSize), albumInfoQuery));
	}

	@Operation(summary = "删除专辑信息")
	@DeleteMapping("/removeAlbumInfo/{albumId}")
	public Result<Void> removeAlbumInfo(@PathVariable Long albumId){
		albumInfoService.removeAlbumInfo(albumId);
		return Result.ok();
	}

	@Operation(summary = "根据专辑id获取专辑信息")
	@GetMapping("/getAlbumInfo/{albumId}")
	public Result<AlbumInfo> getAlbumInfo(@PathVariable Long albumId){
		return Result.ok(albumInfoService.getAlbumInfo(albumId));
	}

	@Operation(summary = "修改专辑信息")
	@PutMapping("/updateAlbumInfo/{albumId}")
	public Result<Void> updateAlbumInfo(@PathVariable Long albumId, @RequestBody AlbumInfoVo albumInfoVo){
		albumInfoService.updateAlbumInfo(albumId,albumInfoVo);
		return Result.ok();
	}


	@Operation(summary = "获取用户专辑列表")
	@GetMapping("/findUserAllAlbumList")
	public Result<List<AlbumInfo>> findUserAllAlbumList(){
		return Result.ok(albumInfoService.findUserAllAlbumList());
	}

	@Operation(summary = "获取专辑属性值列表")
	@GetMapping("/findAlbumAttributeValue/{albumId}")
	public Result<List<AlbumAttributeValue>> findAlbumAttributeValue(@PathVariable Long albumId){
		return Result.ok(albumAttributeValueService.list(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId,albumId)));
	}

	@Operation(summary = "根据专辑id获取专辑统计信息")
	@GetMapping("/getAlbumStatVo/{albumId}")
	public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId){
		AlbumStatVo albumStatVo = albumInfoService.getAlbumStatVo(albumId);
		return Result.ok(albumStatVo);
	}
}

