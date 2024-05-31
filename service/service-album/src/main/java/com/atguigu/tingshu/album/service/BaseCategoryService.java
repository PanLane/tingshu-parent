package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {


    /**
     * 获取分类数据
     * @return
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 根据一级分类id获取专辑属性
     * @param categoryId 一级分类id
     * @return
     */
    List<BaseAttribute> findAttribute(Long categoryId);
}
