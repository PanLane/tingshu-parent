package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
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
    Page<TrackListVo> findAlbumTrackPage(Page<TrackInfoVo> page, TrackInfoQuery trackInfoQuery);

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
}
