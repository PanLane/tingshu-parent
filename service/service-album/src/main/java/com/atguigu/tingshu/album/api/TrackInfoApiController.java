package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album/trackInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoApiController {

	@Autowired
	private TrackInfoService trackInfoService;

	//http://127.0.0.1:8515/api/album/trackInfo/uploadTrack
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
		trackInfoQuery.setUserId(AuthContextHolder.getUserId());
		return Result.ok(trackInfoService.findAlbumTrackPage(page,trackInfoQuery));
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
}

