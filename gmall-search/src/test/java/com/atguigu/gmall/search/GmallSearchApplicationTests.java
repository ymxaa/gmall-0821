package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.crud.UserRepository;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.pojo.User;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {


    @Autowired
    ElasticsearchRestTemplate restTemplate;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RestHighLevelClient restHighLevelClient;
    @Autowired
    GoodsRepository goodsRepository;

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void createIndex(){
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);

        Integer pageNum = 1;
        Integer pageSize = 100;

        do{
            //分页查询出spu
            PageParamVo pageParamVo = new PageParamVo(pageNum, pageSize, null);
            ResponseVo<List<SpuEntity>> listResponseVo = gmallPmsClient.querySpuByPageJson(pageParamVo);
            List<SpuEntity> spuEntities = listResponseVo.getData();
            if(CollectionUtils.isEmpty(spuEntities)){
                break;
            }
            // 遍历查询spu查询每个spu下的所有sku
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuResponseVo = gmallPmsClient.querySkuBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skuResponseVo.getData();
                if(!CollectionUtils.isEmpty(skuEntities)){
                    // 将sku集合转化为goods集合
                    List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        // sku相关信息
                        goods.setSkuId(skuEntity.getId());
                        goods.setPrice(skuEntity.getPrice().doubleValue());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubTitle(skuEntity.getSubtitle());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        // 创建时间
                        goods.setCreateTime(spuEntity.getCreateTime());
                        // 获取库存
                        ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = gmallWmsClient.queryWareSkuBySkuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
                        if(!CollectionUtils.isEmpty(wareSkuEntities)){
                            goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a,b) -> a+b).get());
                            goods.setStore(wareSkuEntities.stream().anyMatch(
                                    wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                        }
                        // 品牌
                        ResponseVo<BrandEntity> brandResponseVo = gmallPmsClient.queryBrandById(skuEntity.getBrandId());
                        BrandEntity brandEntity = brandResponseVo.getData();
                        if(brandEntity != null){
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }
                        // 分类
                        ResponseVo<CategoryEntity> categoryResponseVo = gmallPmsClient.queryCategoryById(skuEntity.getCategoryId());
                        CategoryEntity categoryEntity = categoryResponseVo.getData();
                        if(categoryEntity != null){
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }
                        // 检索参数
                        List<SearchAttrValue> attrValues = new ArrayList<>();

                        ResponseVo<List<SkuAttrValueEntity>> saleAttrValueResponseVo =
                                gmallPmsClient.querySearchAttrValuesByCidAndSkuId(skuEntity.getCategoryId(), skuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrValueResponseVo.getData();
                        if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
                            List<SearchAttrValue> skuToSearchAttrValues = skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValue);
                                return searchAttrValue;
                            }).collect(Collectors.toList());
                            attrValues.addAll(skuToSearchAttrValues);
                        }

                        ResponseVo<List<SpuAttrValueEntity>> baseAttrValueResponseVo =
                                gmallPmsClient.querySearchAttrValuesByCidAndSpuId(skuEntity.getCategoryId(), skuEntity.getSpuId());
                        List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrValueResponseVo.getData();
                        if(!CollectionUtils.isEmpty(spuAttrValueEntities)){
                            List<SearchAttrValue> spuToSearchAttrValues = spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValue);
                                return searchAttrValue;
                            }).collect(Collectors.toList());
                            attrValues.addAll(spuToSearchAttrValues);
                        }
                        goods.setSearchAttrs(attrValues);
                        return goods;
                    }).collect(Collectors.toList());
                    goodsRepository.saveAll(goodsList);
                }

            });
            //如果当前查询的spu等于100条则继续循环
            pageSize = spuEntities.size();
            pageNum++;
        }while (pageSize == 100);
    }

    //@Test
    void contextLoads() {
        this.restTemplate.createIndex(User.class);
        // 创建映射
        this.restTemplate.putMapping(User.class);
    }
    //@Test
    void test1() {
        userRepository.save(new User(3l,"ymx",24,"123456"));
        Optional<User> user = userRepository.findById(1l);
        System.out.println(user.get());
        List<User> byAgeBetween = userRepository.findByAgeBetween(20, 22);
        System.out.println(byAgeBetween);
        System.out.println(userRepository.findByAge(20,22));
    }
    //@Test
    void test2(){
        Iterable<User> search = userRepository.search(QueryBuilders.matchQuery("name", "ymx"));
        search.forEach(System.out::println);
    }
    //@Test
    void test3(){
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.matchQuery("name", "ymx"));
        queryBuilder.withSort(SortBuilders.fieldSort("age").order(SortOrder.DESC));
        queryBuilder.withPageable(PageRequest.of(1, 1));
        Page<User> users = userRepository.search(queryBuilder.build());
        System.out.println(users.getContent());
    }

    //@Test
    void test4() throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.should(QueryBuilders.matchQuery("name","ymx"));
        //boolQueryBuilder.filter(QueryBuilders.rangeQuery("age").gte(20).lte(22));

        //searchSourceBuilder.query(QueryBuilders.matchQuery("name", "ymx"));
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.sort("age",SortOrder.DESC);
        searchSourceBuilder.from(1);
        searchSourceBuilder.size(2);
        searchSourceBuilder.highlighter(new HighlightBuilder().field("name").preTags("<em>").postTags("</em>"));
        searchSourceBuilder.fetchSource(new String[]{"name","age"},null);
        searchSourceBuilder.aggregation(AggregationBuilders.terms("pwdAgg").field("password"));

        System.out.println(searchSourceBuilder);
        SearchRequest searchRequest = new SearchRequest(new String[]{"test"},searchSourceBuilder);
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(search);

        SearchHits hits = search.getHits();
        SearchHit[] hitsHits = hits.getHits();
        for (SearchHit hitsHit : hitsHits) {
            String sourceAsString = hitsHit.getSourceAsString();
            User user = MAPPER.readValue(sourceAsString, User.class);

            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField name = highlightFields.get("name");
            user.setName(name.getFragments()[0].string());

            System.out.println(user);
        }
        Map<String, Aggregation> aggregationMap = search.getAggregations().getAsMap();
        ParsedStringTerms pwaAgg = (ParsedStringTerms)aggregationMap.get("pwdAgg");
        pwaAgg.getBuckets().forEach(bucket -> {
            System.out.println(bucket.getKeyAsString());
            System.out.println(bucket.getDocCount());
        });

    }

}
