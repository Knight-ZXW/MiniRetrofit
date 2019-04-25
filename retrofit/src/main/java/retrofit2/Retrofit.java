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
import retrofit2.http.GET;
import retrofit2.http.POST;

public class Retrofit {

    private OkHttpClient mOkHttpClient;

    private Converter.Factory mConverterFactory;

    public Retrofit(OkHttpClient mOkHttpClient, Converter.Factory mConverterFactory) {
        this.mOkHttpClient = mOkHttpClient;
        this.mConverterFactory = mConverterFactory;
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


    private Call parseGet(String url, Method method, Object args[]) {
        final Request request = new Request.Builder()
                .url(url)
                .get().build();
        return mOkHttpClient.newCall(request);
    }


    private Call parsePost(String url, Method method, Object args[]) {
        final Type[] genericParameterTypes = method.getGenericParameterTypes();
        if (genericParameterTypes.length > 0) {
            final RequestBody requestBody = requestBodyConverter(genericParameterTypes[0]).convert(args[0]);
            final Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
            return mOkHttpClient.newCall(request);
        }
        return null;
    }


    public <T> Converter<T, RequestBody> requestBodyConverter(Type type) {
        return (Converter<T, RequestBody>) mConverterFactory.requestBodyConverter(type);
    }


}
