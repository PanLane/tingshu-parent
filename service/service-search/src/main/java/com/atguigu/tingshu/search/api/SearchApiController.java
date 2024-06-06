package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    @Operation(summary = "专辑上架")
    @GetMapping("/upperAlbum/{albumId}")
    public Result<Void> upperAlbum(@PathVariable Long albumId){
        searchService.upperAlbum(albumId);
        return Result.ok();
    }

    @Operation(summary = "专辑下架")
    @GetMapping("/lowerAlbum/{albumId}")
    public Result<Void> lowerAlbum(@PathVariable Long albumId){
        searchService.lowerAlbum(albumId);
        return Result.ok();
    }

    @Operation(summary = "批量上传")
    @GetMapping("/saveBatch")
    public Result<Void> saveBatch(){
        for (long i = 1; i <= 1500; i++) {
            searchService.upperAlbum(i);
        }
        return Result.ok();
    }
}

