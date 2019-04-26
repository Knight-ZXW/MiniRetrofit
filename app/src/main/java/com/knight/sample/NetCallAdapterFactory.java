package com.knight.sample;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Created by zhuoxiuwu
 * on 2019/4/26
 * email nimdanoob@gmail.com
 */
public class NetCallAdapterFactory extends CallAdapter.Factory {

    /**
     * returnType参数 和 retroift参数 由底层框架传递给开发者
     * @param returnType
     * @param retrofit
     * @return
     */
    @Override
    public CallAdapter<?> get(final Type returnType, final Retrofit retrofit) {
        //判断返回类型是否是 NetCall
        if (getRawType(returnType) != NetCall.class) {
            return null;
        }
        //要求开发者方法的返回类型必须写成 NetCall<T> 或者NetCall<? extends Foo> 的形式,泛型内的类型就是Json数据对应的Class
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalStateException(
                    "NetCall return type must be parameterized as NetCall<Foo> or NetCall<? extends Foo>");
        }
        final Type innerType = getParameterUpperBound(0, (ParameterizedType) returnType);

        return new CallAdapter<NetCall>() {

            @Override
            public NetCall adapt(final Call call) {

                return new NetCall() {
                    @Override
                    public void execute(final NetCallback netCallback) {
                        call.enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                netCallback.onFailure(e);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                //由retrofit 提供 ResponseBody 到 某个Type Class的转换
                                final Object value = retrofit.responseBodyTConverter(innerType).convert(response.body());
                                netCallback.onSuccess(value);
                            }
                        });
                    }
                };
            }
        };
    }
}
