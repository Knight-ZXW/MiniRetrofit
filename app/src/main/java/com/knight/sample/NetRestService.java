package com.knight.sample;

import okhttp3.Call;
import retrofit2.http.GET;

/**
 * Created by zhuoxiuwu
 * on 2019/4/25
 * email nimdanoob@gmail.com
 */
public interface NetRestService {

    @GET("http://gank.io/api/today")
    public Call todayGank();
}
