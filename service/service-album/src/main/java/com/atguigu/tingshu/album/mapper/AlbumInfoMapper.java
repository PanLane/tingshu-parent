package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    /**
     * 分页条件查询用户专辑信息
     * @param page
     * @param albumInfoQuery
     * @return
     */
    Page<AlbumListVo> selectUserAlbumPage(Page<AlbumListVo> page,@Param("vo") AlbumInfoQuery albumInfoQuery);

    /**
     * 专辑包含声音总数+1
     * @param id 专辑id
     * @return
     */
    int incrIncludeTraceCount(Long id);

    /**
     * 专辑信息声音总数-1
     * @param id
     */
    int decrIncludeTraceCount(Long id);
}
