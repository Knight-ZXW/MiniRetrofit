package retrofit2;


import com.google.gson.Gson;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.POST;

public class Retrofit {

    private OkHttpClient mOkHttpClient;

    private Converter.Factory mConverterFactory;

    private CallAdapter.Factory mCallAdapterFactory;

    public Retrofit(OkHttpClient mOkHttpClient, Converter.Factory mConverterFactory, CallAdapter.Factory callAdapterFactory) {
        this.mOkHttpClient = mOkHttpClient;
        this.mConverterFactory = mConverterFactory;
        this.mCallAdapterFactory = callAdapterFactory;
    }

    @SuppressWarnings("unchecked")
    public <T> T createService(final Class<T> service) {
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method,
                                         Object[] args) throws Throwable {
                        //获取方法所有的注解
                        final Annotation[] annotations = method.getAnnotations();
                        final Type returnType = method.getGenericReturnType();
                        for (int i = 0; i < annotations.length; i++) {
                            if (annotations[i] instanceof GET) { //如果注解是GET类型
                                final GET annotation = (GET) annotations[i];
                                return parseGet(annotation.value(), method, args);
                            } else if (annotations[i] instanceof POST) {
                                final POST annotation = (POST) annotations[i];
                                return parsePost(annotation.value(), method, args);
                            }
                        }
                        return null;
                    }
                });
    }


    private Object parseGet(String url, Method method, Object args[]) {
        final Request request = new Request.Builder()
                .url(url)
                .get().build();

        final Call call = mOkHttpClient.newCall(request);


        return adaptCall(method, call);
    }


    private Object parsePost(String url, Method method, Object args[]) {
        final Type[] genericParameterTypes = method.getGenericParameterTypes();
        if (genericParameterTypes.length > 0) {
            final RequestBody requestBody = requestBodyConverter(genericParameterTypes[0]).convert(args[0]);
            final Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
            final Call call = mOkHttpClient.newCall(request);

            return adaptCall(method, call);

        }
        return null;
    }

    /**
     * 负责 任意Java类型到 RequestBody的转换
     * @param type
     * @param <T>
     * @return
     */
    public <T> Converter<T, RequestBody> requestBodyConverter(Type type) {
        return (Converter<T, RequestBody>) mConverterFactory.requestBodyConverter(type);
    }

    /**
     * 负责ResponseBody到Type类型的转换
     * @param type
     * @param <T>
     * @return
     */
    public <T> Converter<ResponseBody,T> responseBodyTConverter(Type type){
        return (Converter<ResponseBody, T>) mConverterFactory.responseBodyConverter(type);
    }


    /**
     * 获取方法的返回类型，并使用CallAdapter做类型转换
     * @param method
     * @param call
     * @return
     */
    private Object adaptCall(Method method, Call call) {
        final Type returnType = method.getGenericReturnType();
        if (Utils.getRawType(returnType) != Call.class) {
            final CallAdapter<?> callAdapter = mCallAdapterFactory.get(returnType,this);
            return callAdapter.adapt(call);
        } else {
            return call;
        }
    }


}
