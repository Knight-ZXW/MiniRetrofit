package com.knight.sample.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class GankEntity {
    //文章地址
    private String url;
    //文章概况描述
    private String desc;
    //作者
    private String who;
}
