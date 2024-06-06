package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.search.service.AlbumIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    AlbumInfoFeignClient albumInfoFeignClient;
    @Autowired
    CategoryFeignClient categoryFeignClient;
    @Autowired
    UserInfoFeignClient userInfoFeignClient;
    @Autowired
    AlbumIndexRepository albumIndexRepository;
    @Autowired
    Executor myExecutor;

    @Override
    public void upperAlbum(Long albumId) {

        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //调用专辑信息客户端，获取专辑信息
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            //使用断言进行判空
            Assert.notNull(albumInfoResult, "专辑信息结果为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑信息数据为空");
            //赋值
            BeanUtils.copyProperties(albumInfo, albumInfoIndex);
            return albumInfo;
        }, myExecutor);

        CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAccept(albumInfo -> {
            //调用专辑分类客户端，获取分类信息
            Result<BaseCategoryView> categoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
            Assert.notNull(categoryViewResult, "分类信息结果为空");
            BaseCategoryView categoryView = categoryViewResult.getData();
            Assert.notNull(categoryView, "分类信息数据为空");
            //赋值
            albumInfoIndex.setCategory2Id(categoryView.getCategory2Id());
            albumInfoIndex.setCategory3Id(categoryView.getCategory3Id());
        });

        CompletableFuture<Void> userInfoCompletableFuture = albumInfoCompletableFuture.thenAccept(albumInfo -> {
            //调用用户微服务，获取主播名，这里需要传递userId，因为使用kafka和异步编排会使请求头失效！！！会导致无法获取请求头
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoById(albumInfo.getUserId());
            Assert.notNull(userInfoVoResult, "用户信息结果为空");
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            Assert.notNull(userInfoVo, "用户信息数据为空");
            //赋值
            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
        });

        CompletableFuture<Void> attrCompletableFuture = CompletableFuture.runAsync(() -> {
            //调用专辑微服务，获取专辑属性值列表
            Result<List<AlbumAttributeValue>> albumAttributeValueResult = albumInfoFeignClient.findAlbumAttributeValue(albumId);
            //使用断言进行判空
            Assert.notNull(albumAttributeValueResult, "专辑属性值结果为空");
            List<AlbumAttributeValue> attributeValueList = albumAttributeValueResult.getData();
            Assert.notNull(attributeValueList, "专辑属性值数据为空");
            //类型转换
            List<AttributeValueIndex> attributeValueIndexList = attributeValueList.stream().map(albumAttributeValue -> {
                AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                BeanUtils.copyProperties(albumAttributeValue, attributeValueIndex);
                return attributeValueIndex;
            }).collect(Collectors.toList());
            //赋值
            albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
        },myExecutor);

        //设置播放量、订阅量、购买量、评论数、热度
        Random random = new Random();
        albumInfoIndex.setPlayStatNum(random.nextInt(100000000));
        albumInfoIndex.setSubscribeStatNum(random.nextInt(10000000));
        albumInfoIndex.setBuyStatNum(random.nextInt(100000));
        albumInfoIndex.setCommentStatNum(random.nextInt(10000));
        albumInfoIndex.setHotScore(random.nextDouble() * 100);

        CompletableFuture.allOf(albumInfoCompletableFuture,categoryCompletableFuture, attrCompletableFuture,userInfoCompletableFuture).join();

        //上架
        albumIndexRepository.save(albumInfoIndex);
    }

    @Override
    public void lowerAlbum(Long albumId) {
        albumIndexRepository.deleteById(albumId);
    }
}
