package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.*;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album/trackInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoApiController {

	@Autowired
	private TrackInfoService trackInfoService;

	@Operation(summary = "声音上传")
	@PostMapping("/uploadTrack")
	public Result<Map<String,String>> uploadTrack(@RequestParam MultipartFile file) {
		return Result.ok(trackInfoService.uploadTrack(file));
	}

	@Operation(summary = "保存声音")
	@PostMapping("/saveTrackInfo")
	public Result<Void> saveTrackInfo(@RequestBody TrackInfoVo trackInfoVo) {
		trackInfoService.saveTrackInfo(trackInfoVo);
		return Result.ok();
	}

	@TsLogin
	@Operation(summary = "分页条件查询声音列表")
	@PostMapping("/findUserTrackPage/{pageNo}/{pageSize}")
	public Result<Page<TrackListVo>> findUserTrackPage(@PathVariable Integer pageNo,
														@PathVariable Integer pageSize,
														@RequestBody TrackInfoQuery trackInfoQuery) {
		Page<TrackInfoVo> page = new Page<>(pageNo, pageSize);
		//trackInfoQuery.setUserId(AuthContextHolder.getUserId());
		trackInfoQuery.setUserId(1L);
		return Result.ok(trackInfoService.findUserTrackPage(page,trackInfoQuery));
	}


	@Operation(summary = "删除声音")
	@DeleteMapping("/removeTrackInfo/{id}")
	public Result<Void> removeTrackInfo(@PathVariable Long id) {
		trackInfoService.removeTrackInfo(id);
		return Result.ok();
	}

	@Operation(summary = "根据id查询声音信息")
	@GetMapping("/getTrackInfo/{id}")
	public Result<TrackInfo> getTrackInfo(@PathVariable Long id) {
		return Result.ok(trackInfoService.getById(id));
	}

	@Operation(summary = "修改声音信息")
	@PutMapping("/updateTrackInfo/{id}")
	public Result<Void> updateTrackInfo(@PathVariable Long id, @RequestBody TrackInfoVo trackInfoVo) {
		trackInfoService.updateTrackInfo(id,trackInfoVo);
		return Result.ok();
	}

	@TsLogin(required = false)
	@Operation(summary = "查询专辑声音分页列表")
	@GetMapping("/findAlbumTrackPage/{albumId}/{pageNo}/{pageSize}")
	public Result<IPage<AlbumTrackListVo>> findAlbumTrackPage(@PathVariable Long albumId, @PathVariable Integer pageNo, @PathVariable Integer pageSize){
		Page<AlbumTrackListVo> page = new Page(pageNo, pageSize);
		return Result.ok(trackInfoService.findAlbumTrackPage(page,albumId));
	}

	@Operation(summary = "获取声音统计信息")
	@GetMapping("/getTrackStatVo/{trackId}")
	public Result<TrackStatVo> getTrackStatVo(@PathVariable Long trackId) {
		return Result.ok(trackInfoService.getTrackStatVo(trackId));
	}

	@TsLogin
	@Operation(summary = "获取用户声音分级购买支付列表")
	@GetMapping("/findUserTrackPaidList/{trackId}")
	public Result<List<Map<String,Object>>> findUserTrackPaidList(@PathVariable Long trackId) {
		return Result.ok(trackInfoService.findUserTrackPaidList(trackId));
	}

	@Operation(summary = "根据声音id、声音数量，获取用户下单付费声音列表")
	@GetMapping("/findPaidTrackInfoList/{trackId}/{trackCount}")
	public Result<List<TrackInfo>> findPaidTrackInfoList(@PathVariable Long trackId, @PathVariable Integer trackCount) {
		return Result.ok(trackInfoService.findPaidTrackInfoList(trackId,trackCount));
	}

	@Operation(summary = "根据声音id获取专辑id")
	@GetMapping("/getAlbumIdByTrackId/{trackId}")
	public Result<Long> getAlbumIdByTrackId(@PathVariable Long trackId) {
		TrackInfo trackInfo = trackInfoService.getById(trackId);
		return Result.ok(trackInfo.getAlbumId());
	}
}

