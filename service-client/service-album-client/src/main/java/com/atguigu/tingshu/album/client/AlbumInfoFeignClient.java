package com.atguigu.tingshu.album.client;

import com.atguigu.tingshu.album.client.impl.AlbumInfoDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
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
@FeignClient(value = "service-album", fallback = AlbumInfoDegradeFeignClient.class,path = "/api/album/albumInfo")
public interface AlbumInfoFeignClient {


    @GetMapping("/getAlbumInfo/{albumId}")
    Result<AlbumInfo> getAlbumInfo(@PathVariable Long albumId);

    @GetMapping("/findAlbumAttributeValue/{albumId}")
    Result<List<AlbumAttributeValue>> findAlbumAttributeValue(@PathVariable Long albumId);
}