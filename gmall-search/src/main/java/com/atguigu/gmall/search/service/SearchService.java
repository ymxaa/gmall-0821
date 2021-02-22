package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVo search(SearchParamVo searchParamVo) {

        if(StringUtils.isBlank(searchParamVo.getKeyword())){
            return null;
        }
        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, builderDsl(searchParamVo));
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchResponseVo searchResponseVo = parseResult(searchResponse);
            //分页只有在搜索参数中有
            searchResponseVo.setPageNum(searchParamVo.getPageNum());
            searchResponseVo.setPageSize(searchParamVo.getPageSize());
            return searchResponseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    private SearchSourceBuilder builderDsl(SearchParamVo searchParamVo) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        String keyword = searchParamVo.getKeyword();
        if (StringUtils.isBlank(keyword)){
            return searchSourceBuilder;
        }
        //1、构建检索条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        searchSourceBuilder.query(boolQueryBuilder);
        //1.1 构建搜索条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));
        //1.2 构建过滤条件
        //1.2.1 构建品牌过滤
        List<Long> brandId = searchParamVo.getBrandId();
        if(!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }
        //1.2.2 构建分类过滤
        List<Long> categoryId = searchParamVo.getCategoryId();
        if(!CollectionUtils.isEmpty(categoryId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",categoryId));
        }
        //1.2.3 构建价格区间过滤
        Double priceFrom = searchParamVo.getPriceFrom(); //起始价格
        Double priceTo = searchParamVo.getPriceTo(); //终止价格
        if(priceFrom != null || priceTo != null){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if(priceFrom != null){
                rangeQuery.gte(priceFrom);
            }
            if(priceTo != null){
                rangeQuery.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQuery);
        }
        //1.2.4 构建是否有货过滤
        Boolean store = searchParamVo.getStore();
        if(store != null && store) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }
        //1.2.5 构建规格参数嵌套过滤
        // {"4:8G-12G","5:128G-256G-512G"}
        List<String> props = searchParamVo.getProps();
        if(!CollectionUtils.isEmpty(props)){ //每一个prop "4:8G-12G"
            props.forEach(prop -> {
                //用:分隔出attrId和attrValue
                String[] attr = StringUtils.split(prop, ":");
                if(attr != null && attr.length == 2){
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attr[0]));
                    //用-分隔attrValue
                    String[] attrValues = StringUtils.split(attr[1]);
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));

                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs",boolQuery, ScoreMode.None));
                }
            });
        }

        //2、排序
        Integer sort = searchParamVo.getSort();
        switch (sort){
            case 1: searchSourceBuilder.sort("price", SortOrder.DESC); break;
            case 2: searchSourceBuilder.sort("price", SortOrder.ASC); break;
            case 3: searchSourceBuilder.sort("sales", SortOrder.DESC); break;
            case 4: searchSourceBuilder.sort("createTime", SortOrder.DESC); break;
            default: searchSourceBuilder.sort("_score", SortOrder.DESC); break;
        }
        //3、构建分页条件
        Integer pageNum = searchParamVo.getPageNum();
        Integer pageSize = searchParamVo.getPageSize();
        searchSourceBuilder.from((pageNum-1)*pageSize);
        searchSourceBuilder.size(pageSize);
        //4、构建高亮
        searchSourceBuilder.highlighter(
                new HighlightBuilder()
                        .field("title")
                        .preTags("<font style = 'color:red;'>")
                        .postTags("</font>")
        );
        //5、构建聚合
        //5.1 构建品牌聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo"))
        );
        //5.2 构建分类聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        //5.3 构建规格参数聚合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));

        //6、构建结果集过滤
        searchSourceBuilder.fetchSource(new String[]{"skuId","defaultImage","price","title","subTitle"},null);

        return searchSourceBuilder;
    }

    private SearchResponseVo parseResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();

        //1、解析hits，获取总记录数和当前页的记录列表
        SearchHits hits = response.getHits();
        SearchHit[] hitsHits = hits.getHits();
        //总记录数
        responseVo.setTotal(hits.totalHits);
        //当前页数据
        List<Goods> goodsList = Stream.of(hitsHits).map(hitsHit -> {
            String sourceAsString = hitsHit.getSourceAsString();
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            //获取高亮标题，覆盖_source普通标题
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            goods.setTitle(highlightField.getFragments()[0].string());
            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        //2、解析aggregations，获取到品牌列表，分类列表、规格参数列表
        //把聚合结果集以map的形式解析：key是聚合的名称 value是聚合的内容
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        //2.1 获取品牌
        ParsedLongTerms brandIdAgg = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(buckets)){
            List<BrandEntity> brandEntities = buckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                brandEntity.setId(bucket.getKeyAsNumber().longValue());
                //根据获取当前桶的子聚合
                Map<String, Aggregation> brandAggregationMap = bucket.getAggregations().asMap();

                //每个品牌名称有且仅有一个桶
                ParsedStringTerms brandNameAgg = (ParsedStringTerms)brandAggregationMap.get("brandNameAgg");
                List<? extends Terms.Bucket> brandNameAggBuckets = brandNameAgg.getBuckets();
                brandEntity.setName(brandNameAggBuckets.get(0).getKeyAsString());
                //获取logo的子聚合有且仅有一个桶
                ParsedStringTerms logoAgg = (ParsedStringTerms)brandAggregationMap.get("logoAgg");
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());

                return brandEntity;
            }).collect(Collectors.toList());
            responseVo.setBrands(brandEntities);
        }
        //2.2 获取分类
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> buckets1 = categoryIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(buckets1)){
            //把每个桶转化为分类
            List<CategoryEntity> categoryEntities = buckets1.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(bucket.getKeyAsNumber().longValue());
                //获取分类名称的子聚合获取分类名称
                Map<String, Aggregation> categoryAggregationMap = bucket.getAggregations().asMap();
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms)categoryAggregationMap.get("categoryNameAgg");
                List<? extends Terms.Bucket> categoryNameAggBuckets = categoryNameAgg.getBuckets();
                categoryEntity.setName(categoryNameAggBuckets.get(0).getKeyAsString());
                return categoryEntity;
            }).collect(Collectors.toList());
            responseVo.setCategories(categoryEntities);
        }
        //2.3 获取规格参数
        // 获取到规格参数的嵌套聚合
        ParsedNested attrAgg = (ParsedNested)aggregationMap.get("attrAgg");
        // 获取嵌套聚合中attrId的聚合
        ParsedLongTerms attrIdAgg = (ParsedLongTerms)attrAgg.getAggregations().get("attrIdAgg");
        // 获取attrId聚合中的桶集合，获取所有的检索类型的规格参数
        List<? extends Terms.Bucket> buckets2 = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(buckets2)){
            List<SearchResponseAttrVo> searchResponseAttrVos = buckets2.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();

                //桶中的key就是attrId
                searchResponseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                //通过子聚合获取attrName和attrValues
                Map<String, Aggregation> subAggregationMap = bucket.getAggregations().asMap();
                //获取attrName的子聚合
                ParsedStringTerms attrNameAgg = (ParsedStringTerms)subAggregationMap.get("attrNameAgg");
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                //获取attrValue的子聚合
                ParsedStringTerms attrValueAgg = (ParsedStringTerms)subAggregationMap.get("attrValueAgg");
                List<? extends Terms.Bucket> buckets3 = attrValueAgg.getBuckets();
                if(!CollectionUtils.isEmpty(buckets3)) {
                    List<String> attrValues = buckets3.stream().map(bucket1 -> bucket1.getKeyAsString()).collect(Collectors.toList());
                    searchResponseAttrVo.setAttrValues(attrValues);
                }
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(searchResponseAttrVos);
        }

        return responseVo;
    }
}
