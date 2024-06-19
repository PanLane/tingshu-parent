package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrackStatMapper extends BaseMapper<TrackStat> {


    /**
     * 根系声音统计信息
     * @param trackStatMqVo
     * @return
     */
    int updateTrackStat(TrackStatMqVo trackStatMqVo);

    /**
     * 根据声音id查询声音统计信息
     * @param trackId
     * @return
     */
    TrackStatVo selectTrackStatVo(Long trackId);
}
