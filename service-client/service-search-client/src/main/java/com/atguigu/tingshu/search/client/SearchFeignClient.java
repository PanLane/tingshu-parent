package com.atguigu.tingshu.search.client;

import com.atguigu.tingshu.search.client.impl.SearchDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-search", fallback = SearchDegradeFeignClient.class,path = "api/search/albumInfo")
public interface SearchFeignClient {

    @GetMapping("/updateLatelyAlbumRanking")
    Result<Void> updateLatelyAlbumRanking() throws IOException;

}