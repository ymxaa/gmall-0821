package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;

@Data
public class ItemsGroupVo {
    private Long id;
    private String name;
    private List<AttrValueVo> attrValues;
}
