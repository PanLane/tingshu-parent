package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.netty.util.concurrent.CompleteFuture;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;
    @Autowired
    private UserInfoFeignClient userInfoFeignClient;
    @Autowired
    private CategoryFeignClient categoryFeignClient;
    @Autowired
    private Executor myExecutor;
    @Autowired
    private RedissonClient redissonClient;


    @Override
    public Map<String, Object> getItem(Long albumId) {

        //创建map，接收数据
        HashMap<String, Object> map = new HashMap<>();

        //判断专辑id是否在布隆过滤器里
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        if(!bloomFilter.contains(albumId)) return map; //不在直接返回

        //获取专辑信息
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            Assert.notNull(albumInfoResult, "专辑信息结果集为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑信息为空");
            map.put("albumInfo", albumInfo); //封装数据
            return albumInfo;
        }, myExecutor);


        //获取主播信息
        CompletableFuture<Void> announcerCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoById(albumInfo.getUserId());
            Assert.notNull(userInfoVoResult, "主播信息结果集为空");
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            Assert.notNull(userInfoVo, "主播信息为空");
            map.put("announcer", userInfoVo); //封装数据
        });

        //获取分类信息
        CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<BaseCategoryView> categoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
            Assert.notNull(categoryViewResult, "分类信息结果集为空");
            BaseCategoryView baseCategoryView = categoryViewResult.getData();
            Assert.notNull(baseCategoryView, "分类信息为空");
            map.put("baseCategoryView", baseCategoryView);//封装数据
        });

        //获取统计信息
        CompletableFuture<Void> statCompletableFuture = CompletableFuture.runAsync(() -> {
            Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStatVo(albumId);
            Assert.notNull(albumStatVoResult,"专辑统计信息结果集为空");
            AlbumStatVo albumStatVo = albumStatVoResult.getData();
            Assert.notNull(albumStatVo,"专辑统计信息为空");
            map.put("albumStatVo",albumStatVo);//赋值
        }, myExecutor);

        //等待异步编排完成
        CompletableFuture.allOf(albumInfoCompletableFuture,announcerCompletableFuture,categoryCompletableFuture,statCompletableFuture).join();

        //返回数据
        return map;
    }
}
