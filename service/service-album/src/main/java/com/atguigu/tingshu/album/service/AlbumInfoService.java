package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {


    /**
     * 保存专辑信息
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo);

    /**
     * 分页查询用户专辑
     * @param page
     * @param albumInfoQuery
     * @return
     */
    Page<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> page, AlbumInfoQuery albumInfoQuery);

    /**
     * 删除专辑
     * @param albumId 专辑id
     */
    void removeAlbumInfo(Long albumId);

    /**
     * 专辑信息回显
     * @param albumId
     * @return
     */
    AlbumInfo getAlbumInfo(Long albumId);

    /**
     * 修改专辑信息
     * @param albumId
     * @param albumInfoVo
     */
    void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo);

    /**
     * 获取用户所有专辑列表
     */
    List<AlbumInfo> findUserAllAlbumList();
}
