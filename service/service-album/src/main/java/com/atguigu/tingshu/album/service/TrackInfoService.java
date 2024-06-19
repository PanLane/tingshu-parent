package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.*;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    /**
     * 声音上传
     */
    Map<String,String> uploadTrack(MultipartFile file);

    /**
     * 保存声音
     * @param trackInfoVo
     */
    void saveTrackInfo(TrackInfoVo trackInfoVo);

    /**
     * 分页条件查询声音列表
     * @param page
     * @param trackInfoQuery
     * @return
     */
    Page<TrackListVo> findUserTrackPage(Page<TrackInfoVo> page, TrackInfoQuery trackInfoQuery);

    /**
     * 删除声音
     * @param id
     */
    void removeTrackInfo(Long id);

    /**
     * 修改声音信息
     * @param id
     * @param trackInfoVo
     */
    void updateTrackInfo(Long id, TrackInfoVo trackInfoVo);

    /**
     * 查询专辑声音分页列表
     * @param page
     * @param albumId
     * @return
     */
    IPage<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> page, Long albumId);

    /**
     * 更新声音、专辑统计信息
     * @param trackStatMqVo
     */
    void updateTrackStat(TrackStatMqVo trackStatMqVo);

    /**
     * 获取声音统计信息
     * @param trackId
     * @return
     */
    TrackStatVo getTrackStatVo(Long trackId);

    /**
     * 获取用户声音分级购买支付列表
     * @param trackId
     * @return
     */
    List<Map<String,Object>> findUserTrackPaidList(Long trackId);

    /**
     * 根据声音id、声音数量，获取用户下单付费声音列表
     * @param trackId
     * @param trackCount
     * @return
     */
    List<TrackInfo> findPaidTrackInfoList(Long trackId, Integer trackCount);
}
