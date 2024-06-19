package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.vo.album.AlbumStatMqVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlbumStatMapper extends BaseMapper<AlbumStat> {

    /**
     * 根据专辑id获取专辑统计信息
     * @param albumId
     * @return
     */
    AlbumStatVo selectAlbumStatVo(Long albumId);

    /**
     * 根系专辑统计信息
     * @param albumStatMqVo
     * @return
     */
    int updateAlbumStat(AlbumStatMqVo albumStatMqVo);
}
