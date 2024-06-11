package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Operation(summary = "检索")
    @PostMapping
    public Result<AlbumSearchResponseVo> search(@RequestBody AlbumIndexQuery albumIndexQuery) throws IOException {
        return Result.ok(searchService.search(albumIndexQuery));
    }

    @Operation(summary = "查询指定一级分类下热门排行专辑")
    @GetMapping("/channel/{category1Id}")
    public Result<List<Map<String,Object>>> getHotAlbum(@PathVariable Long category1Id) throws IOException {
        List<Map<String,Object>> list = searchService.getHotAlbum(category1Id);
        return Result.ok(list);
    }

    @Operation(summary = "关键字自动补全")
    @GetMapping("/completeSuggest/{keyword}")
    public Result<Set<String>> completeSuggest(@PathVariable String keyword) throws IOException {
        Set<String> set = searchService.completeSuggest(keyword);
        return Result.ok(set);
    }
}

