package com.knight.sample;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RestService {
    private static OkHttpClient okHttpClient;
    public static void init() {
        okHttpClient = new OkHttpClient.Builder()
                .build();
    }

    public static<T>  void todayGank(Class<T> responseClazz,NetCallback<T> callback) {
        Request request = new Request.Builder().url("http://gank.io/api/today")
                .get()
                .build();
        okHttpClient.newCall(request).enqueue(new WrapperOkHttpCallback<>(responseClazz,callback));
    }

    public static<T>  void xianduGank(int count, int page,Class<T> responseClazz,NetCallback<T> callback) {
        Request request = new Request.Builder()
                .url("http://gank.io/api/xiandu/data/id/appinn/count/" + count + "/page/" + page)
                .get().build();
        okHttpClient.newCall(request).enqueue(new WrapperOkHttpCallback<>(responseClazz,callback));
    }

    static class WrapperOkHttpCallback<T> implements Callback {
        private static Gson gson = new Gson();
        private Class<T> clazz;
        private NetCallback<T> callback;

        public WrapperOkHttpCallback(Class<T> responseClazz, NetCallback<T> netCallback) {
            this.clazz = responseClazz;
            this.callback = netCallback;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            Log.e("WrapperOkHttpCallback", "onFailure");
            e.printStackTrace();
            callback.onFailure(e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            JsonReader jsonReader = gson.newJsonReader(response.body().charStream());
            T entity = gson.getAdapter(clazz).read(jsonReader);
            Log.d("response", entity.toString());
            callback.onSuccess(entity);

        }
    }

}
