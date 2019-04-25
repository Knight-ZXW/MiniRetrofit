package com.knight.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.knight.sample.entity.AddToGank;
import com.knight.sample.entity.TodayGankResponse;
import com.knight.sample.entity.XianduResponse;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Gson gson;
    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RestService.init();
        gson = new Gson();
        findViewById(R.id.btn_test_1)
                .setOnClickListener(this);
        findViewById(R.id.btn_test_2)
                .setOnClickListener(this);
        findViewById(R.id.btn_test_3)
                .setOnClickListener(this);
        findViewById(R.id.btn_test_4)
                .setOnClickListener(this);
        resultTextView = findViewById(R.id.tv_result);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_test_1:
                RestService.todayGank(TodayGankResponse.class, new NetCallback<TodayGankResponse>() {
                    @Override
                    public void onFailure(Exception e) {

                    }

                    @Override
                    public void onSuccess(TodayGankResponse data) {
                        showHttpResult(data.toString());

                    }
                });
                break;
            case R.id.btn_test_2:
                RestService.xianduGank(10, 1, XianduResponse.class, new NetCallback<XianduResponse>() {
                    @Override
                    public void onFailure(Exception e) {

                    }

                    @Override
                    public void onSuccess(XianduResponse data) {
                        showHttpResult(data.toString());

                    }
                });
                break;
            case R.id.btn_test_3:
                getToDayGankByRetrofit();
                break;
            case R.id.btn_test_4:
                testPost();
                break;
            default:
                break;
        }
    }


    private Retrofit retrofit = new Retrofit(new OkHttpClient());
    private void getToDayGankByRetrofit() {
        retrofit.createService(NetRestService.class).todayGank().enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JsonReader jsonReader = gson.newJsonReader(response.body().charStream());
                TodayGankResponse todayGankResponse = gson.getAdapter(TodayGankResponse.class).read(jsonReader);
                showHttpResult(todayGankResponse.toString());
                Log.d("RetrofitTest","调用成功,结果为"+todayGankResponse.toString());
            }
        });
    }

    private void  testPost(){
        final AddToGank addToGank = new AddToGank();
        addToGank.setUrl("https://gank.io/api");
        addToGank.setWho(null);
        retrofit.createService(NetRestService.class)
                .add2Gank(addToGank).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("RetrofitTest","failed");
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String s = response.body().source().readUtf8();

                Log.d("RetrofitTest","post调用成功,结果为"+s);

            }
        });
    }

    private void showHttpResult(final String msg) {
        resultTextView.post(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(msg);
            }
        });
    }
}
