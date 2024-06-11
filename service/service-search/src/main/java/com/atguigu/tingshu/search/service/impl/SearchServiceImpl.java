package com.atguigu.tingshu.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.PinYinUtils;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.base.BaseEntity;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.AlbumIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.search.service.SuggestIndexRepository;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    AlbumInfoFeignClient albumInfoFeignClient;
    @Autowired
    CategoryFeignClient categoryFeignClient;
    @Autowired
    UserInfoFeignClient userInfoFeignClient;
    @Autowired
    AlbumIndexRepository albumIndexRepository;
    @Autowired
    Executor myExecutor;
    @Autowired
    ElasticsearchClient elasticsearchClient;
    @Autowired
    SuggestIndexRepository suggestIndexRepository;

    @Override
    public void upperAlbum(Long albumId) {
        try {
            AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
            CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
                //调用专辑信息客户端，获取专辑信息
                Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
                //使用断言进行判空
                Assert.notNull(albumInfoResult, "专辑信息结果集为空");
                AlbumInfo albumInfo = albumInfoResult.getData();
                Assert.notNull(albumInfo, "专辑信息数据为空");
                //赋值
                BeanUtils.copyProperties(albumInfo, albumInfoIndex);
                return albumInfo;
            }, myExecutor);

            CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAccept(albumInfo -> {
                //调用专辑分类客户端，获取分类信息
                Result<BaseCategoryView> categoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
                Assert.notNull(categoryViewResult, "分类信息结果为空");
                BaseCategoryView categoryView = categoryViewResult.getData();
                Assert.notNull(categoryView, "分类信息数据为空");
                //赋值
                albumInfoIndex.setCategory1Id(categoryView.getCategory1Id());
                albumInfoIndex.setCategory2Id(categoryView.getCategory2Id());
            });

            CompletableFuture<Void> userInfoCompletableFuture = albumInfoCompletableFuture.thenAccept(albumInfo -> {
                //调用用户微服务，获取主播名，这里需要传递userId，因为使用kafka和异步编排会使请求头失效！！！会导致无法获取请求头
                Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoById(albumInfo.getUserId());
                Assert.notNull(userInfoVoResult, "用户信息结果为空");
                UserInfoVo userInfoVo = userInfoVoResult.getData();
                Assert.notNull(userInfoVo, "用户信息数据为空");
                //赋值
                albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
            });

            CompletableFuture<Void> attrCompletableFuture = CompletableFuture.runAsync(() -> {
                //调用专辑微服务，获取专辑属性值列表
                Result<List<AlbumAttributeValue>> albumAttributeValueResult = albumInfoFeignClient.findAlbumAttributeValue(albumId);
                //使用断言进行判空
                Assert.notNull(albumAttributeValueResult, "专辑属性值结果为空");
                List<AlbumAttributeValue> attributeValueList = albumAttributeValueResult.getData();
                Assert.notNull(attributeValueList, "专辑属性值数据为空");
                //类型转换
                List<AttributeValueIndex> attributeValueIndexList = attributeValueList.stream().map(albumAttributeValue -> {
                    AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                    BeanUtils.copyProperties(albumAttributeValue, attributeValueIndex);
                    return attributeValueIndex;
                }).collect(Collectors.toList());
                //赋值
                albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
            }, myExecutor);

            //设置播放量、订阅量、购买量、评论数、热度
            Random random = new Random();
            albumInfoIndex.setPlayStatNum(random.nextInt(100000000));
            albumInfoIndex.setSubscribeStatNum(random.nextInt(10000000));
            albumInfoIndex.setBuyStatNum(random.nextInt(100000));
            albumInfoIndex.setCommentStatNum(random.nextInt(10000));
            albumInfoIndex.setHotScore(random.nextDouble() * 100);

            CompletableFuture.allOf(albumInfoCompletableFuture, categoryCompletableFuture, attrCompletableFuture, userInfoCompletableFuture).join();

            //将专辑信息上架到专辑信息索引库
            albumIndexRepository.save(albumInfoIndex);
            //将标题信息上架到自动补全索引库，根据标题进行补全
            SuggestIndex titleSuggestIndex = new SuggestIndex();
            titleSuggestIndex.setId(UUID.randomUUID().toString().replace("-", ""));
            titleSuggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
            titleSuggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumTitle()}));
            titleSuggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumTitle())}));
            titleSuggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumTitle())}));
            suggestIndexRepository.save(titleSuggestIndex);
            //将内容信息上架到自动补全索引库，根据内容进行补全
            SuggestIndex introSuggestIndex = new SuggestIndex();
            titleSuggestIndex.setId(UUID.randomUUID().toString().replace("-", ""));
            titleSuggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
            titleSuggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumIntro()}));
            titleSuggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumIntro())}));
            titleSuggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumIntro())}));
            suggestIndexRepository.save(introSuggestIndex);
            //将作者信息上架到自动补全索引库，根据作者进行补全
            SuggestIndex authorSuggestIndex = new SuggestIndex();
            titleSuggestIndex.setId(UUID.randomUUID().toString().replace("-", ""));
            titleSuggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
            titleSuggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAnnouncerName()}));
            titleSuggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAnnouncerName())}));
            titleSuggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAnnouncerName())}));
            suggestIndexRepository.save(authorSuggestIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void lowerAlbum(Long albumId) {
        albumIndexRepository.deleteById(albumId);
    }

    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) throws IOException {
        //获取检索条件
        SearchRequest searchRequest = buildSearchRequest(albumIndexQuery);
        //获取检索结果
        SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
        //赋值
        AlbumSearchResponseVo albumSearchResponseVo = getAlbumSearchResponseVo(searchResponse);
        Integer pageSize = albumIndexQuery.getPageSize();
        Long total = albumSearchResponseVo.getTotal();
        albumSearchResponseVo.setPageNo(albumIndexQuery.getPageNo());
        albumSearchResponseVo.setPageSize(pageSize);
        albumSearchResponseVo.setTotalPages((total - 1) / pageSize + 1);
        //返回数据
        return albumSearchResponseVo;
    }

    @Override
    public List<Map<String, Object>> getHotAlbum(Long category1Id) throws IOException {
        //调用专辑微服务，根据一级分类id获取三级分类对象集合
        Result<List<BaseCategory3>> baseCategory3Result = categoryFeignClient.findTopBaseCategory3(category1Id);
        Assert.notNull(baseCategory3Result, "三级分类结果集为空");
        List<BaseCategory3> baseCategory3List = baseCategory3Result.getData();
        Assert.notNull(baseCategory3List, "三级分类数据为空");

        List<FieldValue> fieldValueList = baseCategory3List.stream().map(baseCategory3 -> FieldValue.of(baseCategory3.getId())).collect(Collectors.toList());

        //将baseCategory3List转为baseCategory3Map
        Map<Long, BaseCategory3> baseCategory3Map = baseCategory3List.stream().collect(Collectors.toMap(BaseCategory3::getId, v -> v));


        //通过es根据三级分类id查询albumInfoIndex
        SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(
                s -> s.index("albuminfo")
                        .query(q -> q.terms(t -> t.field("category3Id").terms(ts -> ts.value(fieldValueList))))
                        .aggregations("groupby_category3Id", f -> f.terms(t -> t.field("category3Id")).aggregations("topics_category3Id", fn -> fn.topHits(t -> t.size(6).sort(st -> st.field(fl -> fl.field("buyStatNum").order(SortOrder.Desc))))))
                , AlbumInfoIndex.class);

        //处理结果集
        Map<String, Aggregate> aggregations = response.aggregations();
        Aggregate groupby_category3Id = aggregations.get("groupby_category3Id");
        List<LongTermsBucket> array = groupby_category3Id.lterms().buckets().array();
        List<Map<String, Object>> result = array.stream().map(bucket -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("baseCategory3", baseCategory3Map.get(bucket.key()));
            List<AlbumInfoIndex> list = bucket.aggregations().get("topics_category3Id").topHits().hits().hits().stream()
                    .map(hit -> JSON.parseObject(hit.source().toString(), AlbumInfoIndex.class)).collect(Collectors.toList());
            map.put("list", list);
            return map;
        }).collect(Collectors.toList());

        //返回数据
        return result;
    }

    @Override
    public Set<String> completeSuggest(String keyword) throws IOException {

        HashSet<String> set = new HashSet<>();

        //创建ddl，从索引库中获取数据
        SearchResponse<SuggestIndex> response = elasticsearchClient.search(
                s -> s.index("suggestinfo")
                        .suggest(sg -> sg.suggesters("suggest_keyword", f -> f.prefix(keyword).completion(c -> c.field("keyword").size(10).skipDuplicates(true).fuzzy(fu -> fu.fuzziness("auto")))))
                        .suggest(sg -> sg.suggesters("suggest_keywordPinyin", f -> f.prefix(keyword).completion(c -> c.field("keywordPinyin").size(10).skipDuplicates(true).fuzzy(fu -> fu.fuzziness("auto")))))
                        .suggest(sg -> sg.suggesters("suggest_keywordSequence", f -> f.prefix(keyword).completion(c -> c.field("keywordSequence").size(10).skipDuplicates(true).fuzzy(fu -> fu.fuzziness("auto")))))
                , SuggestIndex.class);

        //处理结果集
        set.addAll(getSuggestSet(response,"suggest_keyword"));
        set.addAll(getSuggestSet(response,"suggest_keywordPinyin"));
        set.addAll(getSuggestSet(response,"suggest_keywordSequence"));

        //如果补全的关键字小于10条，通过标题进行分词匹配，补全10条为止
        if(set.size()<10){
            SearchResponse<SuggestIndex> response2 = elasticsearchClient.search(
                    s -> s.index("suggestinfo")
                            .query(q->q.match(m->m.field("title").query(keyword)))
                    , SuggestIndex.class);
            List<Hit<SuggestIndex>> hits = response2.hits().hits();
            if(hits!=null){
                for (Hit<SuggestIndex> hit : hits) {
                    if(hit.source()!=null && hit.source().getTitle()!=null) set.add(hit.source().getTitle());
                    if(set.size()>=10) break;
                }
            }
        }

        //返回数据
        return set;
    }

    /**
     * 根据结果集和关键字获取title的集合
     * @param response 结果集
     * @param keyword 关键字
     * @return
     */
    private List<String> getSuggestSet(SearchResponse<SuggestIndex> response, String keyword) {
        List<String> list = new ArrayList<>();//防止set.addAll时发生空指针异常
        List<Suggestion<SuggestIndex>> suggestions = response.suggest().get(keyword);
        if(suggestions!=null) suggestions.get(0).completion().options().stream().map(option -> option.source().getTitle()).collect(Collectors.toList());
        return list;
    }

    /**
     * 构建查询对象
     *
     * @return SearchRequest
     */
    private SearchRequest buildSearchRequest(AlbumIndexQuery albumIndexQuery) {
        SearchRequest.Builder builder = new SearchRequest.Builder().index("albuminfo");

        /*************如果分类不为空，按分类进行检索*************/
        //根据一级分类id检索
        if (albumIndexQuery.getCategory1Id() != null) {
            builder.query(q -> q.bool(b -> b.must(m -> m.match(mt -> mt.field("category1Id").query(albumIndexQuery.getCategory1Id())))));
        }
        //根据二级分类id检索
        if (albumIndexQuery.getCategory2Id() != null) {
            builder.query(q -> q.bool(b -> b.must(m -> m.match(mt -> mt.field("category2Id").query(albumIndexQuery.getCategory2Id())))));
        }
        //根据三级分类id检索
        if (albumIndexQuery.getCategory3Id() != null) {
            builder.query(q -> q.bool(b -> b.must(m -> m.match(mt -> mt.field("category3Id").query(albumIndexQuery.getCategory3Id())))));
        }
        /*************如果关键字和属性不为空，按关键字和属性进行检索*************/
        //根据属性进行检索
        List<String> attributeList = albumIndexQuery.getAttributeList();
        if (!CollectionUtils.isEmpty(attributeList)) {
            attributeList.forEach(attribute -> {
                String[] attr = attribute.split(":");
                if (attr.length == 2) {
                    String attrId = attr[0];
                    String valueId = attr[1];
                    builder.query(q -> q.bool(b -> b.filter(f -> f.nested(n -> n.path("attributeValueIndexList").query(q2 -> q2.bool(b2 -> b2.must(m2 -> m2.term(t -> t.field("attributeValueIndexList.attributeId")
                            .value(attrId))).filter(f2 -> f2.term(t -> t.field("attributeValueIndexList.valueId").value(valueId)))))))));
                }
            });
        }

        //根据关键字进行检索，设置高亮
        String keyword = albumIndexQuery.getKeyword();
        if (StringUtils.hasText(keyword)) {
            builder.query(q -> q.bool(b -> b.must(m -> m.multiMatch(v -> v.query(keyword).fields("albumTitle", "albumIntro")))));
            builder.highlight(h -> h.fields("albumTitle", fn -> fn.preTags("<span style='color:#f86442'>").postTags("</span>")));
        }

        /****************************排序，分页***************************/
        builder.from((albumIndexQuery.getPageNo() - 1) * albumIndexQuery.getPageSize()).size(albumIndexQuery.getPageSize());
        String order = albumIndexQuery.getOrder();
        if (order != null) {
            String[] split = order.split(":");
            if (split.length == 2) {
                String orderField = switch (split[0]) {
                    case "2" -> "playStatNum";
                    case "3" -> "createTime";
                    default -> "hotScore";
                };
                builder.sort(s -> s.field(f -> f.field(orderField).order(split[1].equals("desc") ? SortOrder.Desc : SortOrder.Asc)));
            }
        }

        SearchRequest searchRequest = builder.build();
        log.info("searchDSL：" + searchRequest);

        return searchRequest;
    }

    /**
     * 创建AlbumSearchResponseVo并赋值
     *
     * @param searchResponse 搜索结果
     * @return AlbumSearchResponseVo
     */
    private AlbumSearchResponseVo getAlbumSearchResponseVo(SearchResponse<AlbumInfoIndex> searchResponse) {
        AlbumSearchResponseVo albumSearchResponseVo = new AlbumSearchResponseVo();

        List<Hit<AlbumInfoIndex>> hits = searchResponse.hits().hits();

        if (!CollectionUtils.isEmpty(hits)) {
            albumSearchResponseVo.setList(hits.stream().map(e -> {
                AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
                if (e.source() != null) BeanUtils.copyProperties(e.source(), albumInfoIndexVo);
                //设置高亮
                if (e.highlight().get("albumTitle") != null)
                    albumInfoIndexVo.setAlbumTitle(e.highlight().get("albumTitle").get(0));
                return albumInfoIndexVo;
            }).collect(Collectors.toList()));
        }

        TotalHits total = searchResponse.hits().total();
        albumSearchResponseVo.setTotal(total == null ? 0 : total.value());

        return albumSearchResponseVo;
    }
}
