package com.knight.sample.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class BaseResponse<T> {
    private boolean error;
    private T results;
    public BaseResponse(){

    }
}
