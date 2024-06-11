package com.atguigu.tingshu.search.service;

import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SearchService {


    /**
     * 专辑上架
     * @param albumId
     */
    void upperAlbum(Long albumId);

    /**
     * 专辑下架
     * @param albumId
     */
    void lowerAlbum(Long albumId);

    /**
     * 检索
     * @param albumIndexQuery
     * @return
     */
    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) throws IOException;

    /**
     * 查询指定一级分类下热门排行专辑
     * @param category1Id
     * @return
     */
    List<Map<String, Object>> getHotAlbum(Long category1Id) throws IOException;

    /**
     * 关键字自动补全
     * @param keyword 关键字
     * @return
     */
    Set<String> completeSuggest(String keyword) throws IOException;
}
