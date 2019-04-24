package com.knight.sample;

import java.io.IOException;

/**
 * 项目封装的统一网络请求的回调
 * @param <T>
 */
public interface NetCallback<T> {
    void onFailure(Exception e);

    void onSuccess(T data);
}
