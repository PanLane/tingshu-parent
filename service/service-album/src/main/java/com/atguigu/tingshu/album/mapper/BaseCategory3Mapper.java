package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.BaseCategory3;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BaseCategory3Mapper extends BaseMapper<BaseCategory3> {


    /**
     * 根据一级分类Id查询三级分类列表
     * @param category1Id
     * @return
     */
    List<BaseCategory3> selectByCategory1Id(Long category1Id);
}
