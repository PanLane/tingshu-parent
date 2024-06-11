package com.atguigu.tingshu.search.service;

import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.io.IOException;

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
}
