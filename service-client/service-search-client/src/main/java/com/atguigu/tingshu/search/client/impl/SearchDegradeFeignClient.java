package com.atguigu.tingshu.search.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.client.SearchFeignClient;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SearchDegradeFeignClient implements SearchFeignClient {


    @Override
    public Result<Void> updateLatelyAlbumRanking() throws IOException {
        return null;
    }
}
