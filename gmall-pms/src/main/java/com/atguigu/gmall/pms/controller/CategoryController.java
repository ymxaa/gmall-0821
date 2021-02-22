package com.atguigu.gmall.pms.controller;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 商品三级分类
 *
 * @author ymx
 * @email ymx@atguigu.com
 * @date 2021-01-18 20:26:56
 */
@Api(tags = "商品三级分类 管理")
@RestController
@RequestMapping("pms/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("all/{cid}")
    public ResponseVo<List<CategoryEntity>> query123CategoriesByCid3(@PathVariable("cid") Long cid){
        List<CategoryEntity> categoryEntities = categoryService.query123CategoriesByCid3(cid);
        return ResponseVo.ok(categoryEntities);
    }

    @GetMapping("parent/withsubs/{pid}")
    public ResponseVo<List<CategoryEntity>> queryLv2CategoriesByPid(@PathVariable("pid") Long pid){
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();
        QueryWrapper<CategoryEntity> querySubWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id",pid);
        List<CategoryEntity> categoryEntities = categoryService.list(queryWrapper);


        categoryEntities.forEach(categoryEntity -> {
            querySubWrapper.clear();
            querySubWrapper.eq("parent_id",categoryEntity.getId());
            List<CategoryEntity> list = categoryService.list(querySubWrapper);
            if(!CollectionUtils.isEmpty(list)) {
                categoryEntity.setSubs(list);
            }
        });

        return ResponseVo.ok(categoryEntities);
    }

    /**
     * 列表
     */
    @GetMapping("parent/{parentId}")
    @ApiOperation("分类树预览")
    public ResponseVo<List<CategoryEntity>> queryCategoriesById(@PathVariable("parentId") Long pid){
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();

        if(pid != -1){
            queryWrapper.eq("parent_id", pid);
        }
        List<CategoryEntity> categoryEntities = categoryService.list(queryWrapper);
        return ResponseVo.ok(categoryEntities);
    }
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryCategoryByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = categoryService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id){
		CategoryEntity category = categoryService.getById(id);

        return ResponseVo.ok(category);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody CategoryEntity category){
		categoryService.save(category);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody CategoryEntity category){
		categoryService.updateById(category);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		categoryService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
