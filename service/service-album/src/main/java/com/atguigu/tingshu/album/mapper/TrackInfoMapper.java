package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {


    /**
     * 分页条件查询声音列表
     * @param page
     * @param trackInfoQuery
     * @return
     */
    Page<TrackListVo> selectUserTrackPage(Page<TrackInfoVo> page, @Param("vo") TrackInfoQuery trackInfoQuery);

    /**
     * 更新声音信息序号
     * @param albumId 专辑id
     * @param id 声音id
     * @return
     */
    int updateOrderNum(@Param("albumId") Long albumId,@Param("id") Long id);

    /**
     * 根据专辑id查询AlbumTrackListVo列表
     * @param page
     * @param albumId
     * @return
     */
    Page<AlbumTrackListVo> selectAlbumTrackPage(Page<AlbumTrackListVo> page, Long albumId);

    /**
     * 根据albumId、orderNum获取可能需要购买声音id的集合
     * @param albumId
     * @param orderNum
     * @return
     */
    List<Long> selectMayNeedPaidTrackIdList(@Param("albumId") Long albumId,@Param("orderNum") Integer orderNum);
}
