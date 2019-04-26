package com.knight.sample;

/**
 * Created by zhuoxiuwu
 * on 2019/4/26
 * email nimdanoob@gmail.com
 */
public interface NetCall<T> {
    public void execute(NetCallback<T> netCallback);
}
