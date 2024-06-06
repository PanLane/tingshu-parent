package com.atguigu.tingshu.search.service;


import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AlbumIndexRepository extends ElasticsearchRepository<AlbumInfoIndex,Long> {
}
