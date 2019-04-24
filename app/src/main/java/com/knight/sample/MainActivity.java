package com.knight.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.knight.sample.entity.TodayGankResponse;
import com.knight.sample.entity.XianduResponse;

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
        resultTextView = findViewById(R.id.tv_result);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
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
            default:
                break;
        }
    }

    private void showHttpResult(final String msg){
        resultTextView.post(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(msg);
            }
        });
    }
}
