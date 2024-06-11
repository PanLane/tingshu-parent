package com.atguigu.tingshu.search.service;


import com.atguigu.tingshu.model.search.SuggestIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SuggestIndexRepository extends ElasticsearchRepository<SuggestIndex,String> {
}
