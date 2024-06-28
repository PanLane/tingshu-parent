package com.atguigu.tingshu.album.client;

import com.atguigu.tingshu.album.client.impl.TrackInfoDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-album", fallback = TrackInfoDegradeFeignClient.class,path = "api/album/trackInfo")
public interface TrackInfoFeignClient {

    @GetMapping("/findPaidTrackInfoList/{trackId}/{trackCount}")
    Result<List<TrackInfo>> findPaidTrackInfoList(@PathVariable Long trackId, @PathVariable Integer trackCount);

    @GetMapping("/getAlbumIdByTrackId/{trackId}")
    Result<Long> getAlbumIdByTrackId(@PathVariable Long trackId);
}