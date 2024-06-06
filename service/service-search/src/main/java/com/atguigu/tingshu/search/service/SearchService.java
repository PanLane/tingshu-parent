package com.atguigu.tingshu.search.service;

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
}
