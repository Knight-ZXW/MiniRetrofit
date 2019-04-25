package com.knight.sample;

import com.knight.sample.entity.AddToGank;

import okhttp3.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Created by zhuoxiuwu
 * on 2019/4/25
 * email nimdanoob@gmail.com
 */
public interface NetRestService {

    @GET("http://gank.io/api/today")
    public Call todayGank();

    @POST("https://gank.io/api/add2gank")
    public Call add2Gank(AddToGank addToGank);
}
