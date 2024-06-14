package com.atguigu.tingshu.search.service;

import java.util.Map;

public interface ItemService {


    /**
     * 根据专辑id获取专辑详情
     * @return
     */
    Map<String,Object> getItem(Long albumId);
}
