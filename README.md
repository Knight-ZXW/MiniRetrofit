
## 前文
> 本篇文章将采用循序渐进的编码方式，从零开始实现一个Retorift框架，在实现过程中不断提出问题并分析实现,最终开发出一个mini版的Retrofit框架


## 演示一个目前使用OkHttp的项目Demo
为了更好的演示实现过程，这里我创建了一个简单的Demo项目

这个Demo项目中主要包含3个部分
1. Json数据对应JavaEntity类
2. 项目中包装网络请求回调的Callback
3. 一个包含项目所有网络接口请求的管理类RestService
### JavaBean
```java
@Data
@ToString
public class BaseResponse<T> {
    private boolean error;
    private T results;
}
```

```java
package com.knight.sample.entity;

import java.util.List;
import java.util.Map;

public class XianduResponse extends BaseResponse<List<GankEntity>> {
}

```

### NetCallback 

```java
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
```

### NetWorkService
```java
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

```

在NetworkService类中我们目前定义了2个Http 请求 **todayGank** 和 **xianduGank** ，目前两个请求方式都是 **Get** 其中 **xianduGank** 需要传入 **count** 及 **page**参数分别表示每页数据的数据以及请求的页码，除此之外这两个网络请求都需要传入 一个**Class**对象表示响应的Json数据对应的Model，以便在内部使用Gson来解析，以及网络请求的异步回调 **NetCallback** 


我们不直接使用OkHttp提供的Callback 而是在内部简单的做了封装转换成项目自己的NetCallback，因为对项目的开发人员来说，更希望的是能够直接在Callback的success回调中直接得到响应的Json数据对应的JavaBean.

> [本次提交详细代码见](https://github.com/Knight-ZXW/MiniRetrofit/commit/8c5443b752bd85706b4290c0b54b35a13e58c4e2)
## 思考项目现状

上文模拟的代码只是一个简单的例子,可能会有更好的封装方式，但这并不是我们这篇文章想要讨论的重点。 我们回到示例中RestService类中的代码部分,卡拿下目前网络请求的写法；

因为我们项目中已经有了OKHttp这个网络库了，有关Http具体的连接及通信的脏话累活都可以交给他来处理,对于项目开发者，事实上我们只需要配置以下Http请求部分
- 请求的url 地址
- 请求的方式 (GET、POST、PUT...)
- 请求内容

假设我们已经具备了 Java注解 以及 动态代理的相关知识,知道以下信息

- 注解可以添加在方法上
- Retention为RUNTIME的注解可以在虚拟机运行时也获取到注解上的信息
- Java的动态代理可以运行时生成原接口类型的代理实现类并hook方法的调用

每一个网络接口调用请求的url地址和请求方式都是唯一的 ,那么对于一个简单的网络请求 我们能不能使用 **注解**  + **动态代理** 来简化这一过程，改为声明式的编程方式来实现网络调用，比如就像这样

```java
/**
 * Created by zhuoxiuwu
 * on 2019/4/25
 * email nimdanoob@gmail.com
 */
public interface NetRestService {

    @GET("http://gank.io/api/today")
    public Call todayGank();
}
```
我们在一个抽象接口类中添加了一个方法,在方法上添加了注解 **@GET** 表示这是一个Http GET请求的调用,注解中**GET**带的默认参数表示GET请求的地址。声明这个方法后，我们再通过Java动态代理技术在运行时解析这个方法上的注解的信息，内部通过调用**OKHttp**的相关方法生成一个 **Call**对象

有了大概思路了,我们接下来先简单的实现这样一个小例子来验证我们的想法是否可行

## 编码实现
### 3.1 简单实现一个支持GET、POST请求的Retrofit
新建一个注解类@GET
```java
package retrofit2.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Created by zhuoxiuwu
 * on 2019/4/25
 * email nimdanoob@gmail.com
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GET {
    //注解中 方法名写成value 这样的话，在使用注解传入参数时就不用带key了，它会作为一个默认的调用
    String value();
}
```

新建一个处理Http接口类的动态代理的类**Retrofit**，因为我们实际网络请求的调用是依赖OKHttp,所以我们要求构造函数传入**OkHttp**对象
目前**Retrofit** 类只有一个方法**public <T> T createService(final Class<T> service)** 它接收一个抽象类，并生成该抽象类的代理实现。

```
package retrofit2;



import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.http.GET;

public class Retrofit {

    private OkHttpClient mOkHttpClient;

    public Retrofit(OkHttpClient mOkHttpClient) {
        this.mOkHttpClient = mOkHttpClient;
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
                                final String url = annotation.value();
                                final Request request = new Request.Builder()
                                        .url(url)
                                        .get().build();
                                return mOkHttpClient.newCall(request);
                            }
                        }
                        return null;
                    }
                });
    }
}

```

目前我们主要的目标是为了验证这个方案的可行性，因此createService方法内部的逻辑很简单

1.获取方法上的所有注解
```java
 //获取方法所有的注解
                        final Annotation[] annotations = method.getAnnotations();
```
2.判断如果存在@GET注解则获取注解内的值作为请求的地址
```java
if (annotations[i] instanceof GET) { //如果注解是GET类型

    final GET annotation = (GET) annotations[i];
    final String url = annotation.value();
```
3.根据url构造GET请求的Request对象，并作为参数调用OkHttpClient的newCall方法生成Call对象作为该方法调用的返回值
```java
 final Request request = new Request.Builder()
                                        .url(url)
                                        .get().build();
                                return mOkHttpClient.newCall(request);
```


以上完成了一个对@GET注解申明的Http请求的动态代理封装，下面我们在自己的项目中验证一下
### 3.2在项目中验证
1.创建一个接口类,并添加一个方法，方法的返回类型为**Call**，方法是添加了@GET注解
```
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

```

2.在项目中添加测试方法并调用
```
private void getToDayGankByRetrofit() {
        final Retrofit retrofit = new Retrofit(new OkHttpClient());
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
```

运行之后,方法调用成功并得到了响应结果
```console
 D/RetrofitTest: 调用成功,结果为BaseResponse(error=false, results={Android=[GankEntity(url=https://github.com/iqiyi/Neptune, desc=适用于Android的灵活，强大且轻量级的插件框架...
```


通过简单的一个实现,我们成功验证了使用注解加动态代理的方式实现一个声明式的网络请求框架是可行的，那么后续我们需要继续完善这个项目，提供对更多请求方式 以及参数的支持

------------------------------------------
对于其他请求方式的支持，我们可以添加更多的表示请求方式的注解,当用户设置了不同的注解，在内部我们使用OKHttp调用相应的方法。Http的请求方式大概如下
- @DELETE
- @GET
- @HEAD
- @PATCH
- @POST
- @PUT
- @OPTIONS
> [本次提交见git](https://github.com/Knight-ZXW/MiniRetrofit/commit/406324fbaead0d233a0079ec384af6dee9cf6bba)
### 3.3继续实现POST注解
为了加深理解，我们继续简单的实现一个POST请求，并支持传入一个参数对象，作为POST请求的JSON数据

首先我们添加一个POST注解
```java

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface POST {
    String value();
}

```

```java
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

    public Retrofit(OkHttpClient mOkHttpClient) {
        this.mOkHttpClient = mOkHttpClient;
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

    private Gson gson = new Gson();
    private static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=UTF-8");

    private Call parsePost(String url, Method method, Object args[]) {
        final Type[] genericParameterTypes = method.getGenericParameterTypes();
        if (genericParameterTypes.length > 0) {
            final Class<?> clazz = Utils.getRawType(genericParameterTypes[0]);
            final String jsonBody = gson.toJson(args[0], clazz);
            final Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(MEDIA_TYPE, jsonBody))
                    .build();
            return mOkHttpClient.newCall(request);
        }
        return null;
    }


}

```

在 paresePost方法中我们首先通过Method的getGenericParameterTypes方法获取所有参数的Type类型,并且通过Type类获得参数的原始Class类型，之后就可以使用Gson转换成对应的Json对象了。
> [点击查看本次git提交的详细代码](https://github.com/Knight-ZXW/MiniRetrofit/commit/d62c3d5ebb306f139526634d1d02f99edebe42ee)

### 3.4 实现ConverterFactory 解耦Json转换
在上面的例子中，我们直接在框架Retrofit中使用了Gson库做Json转换，但作为一个框架来说 我们不希望直接强耦合一个第三方Json转换库，这部分更希望交由开发者根据具体情况自由选择；因此我们可以对这部分做下抽象封装，提取成一个负责Json转换的接口 由应用层传入具体的实现.

```
package retrofit2;

import java.lang.reflect.Type;

import javax.annotation.Nullable;

import okhttp3.RequestBody;

/**
 * Created by zhuoxiuwu
 * on 2019/4/25
 * email nimdanoob@gmail.com
 */
public interface Converter<F, T> {
    @Nullable
    T convert(F value);

    abstract class Factory {
        public @Nullable
        Converter<?, RequestBody> requestBodyConverter(Type type) {
            return null;
        }

    }
}

```

应用层需要传入一个ConverterFactory,该工厂类负责根据传入的Type类型，返回一个能够将该Type类型的对象转换成RequestBody的Converter

我们对Retrofit的构造函数以及paresePost方法做下修改，要求构造函数中传入一个ConverterFactory的实现，并在paresePost方法中使用这个ConverterFactory来做Java对象到ReqeustBody的转换
```java
public class Retrofit {

    private OkHttpClient mOkHttpClient;

    private Converter.Factory mConverterFactory;

    public Retrofit(OkHttpClient mOkHttpClient, Converter.Factory mConverterFactory) {
        this.mOkHttpClient = mOkHttpClient;
        this.mConverterFactory = mConverterFactory;
    }
    //..省略部分代码
    
    private Call parsePost(String url, Method method, Object args[]) {
        final Type[] genericParameterTypes = method.getGenericParameterTypes();
        if (genericParameterTypes.length > 0) {
            //直接调用得到RequestBody
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

```

在应用层，我们实现并传入一个Gson的ConvertFactory的实现

```java
package com.knight.sample;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Converter;

/**
 * Created by zhuoxiuwu
 * on 2019/4/25
 * email nimdanoob@gmail.com
 */
public class GsonConverterFactory extends Converter.Factory {
    public static GsonConverterFactory create() {
        return create(new Gson());
    }

    public static GsonConverterFactory create(Gson gson) {
        if (gson == null) throw new NullPointerException("gson == null");
        return new GsonConverterFactory(gson);
    }

    private final Gson gson;

    private GsonConverterFactory(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type) {
        //通过Type 转换成Gson的TypeAdapter
        //具体类型的json转换依赖于这个TypeAdapter
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new GsonRequestBodyConverter<>(gson, adapter);

    }


    final static class GsonRequestBodyConverter<T> implements Converter<T, RequestBody> {
        private static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=UTF-8");
        private static final Charset UTF_8 = Charset.forName("UTF-8");

        private final Gson gson;
        private final TypeAdapter<T> adapter;

        GsonRequestBodyConverter(Gson gson, TypeAdapter<T> adapter) {
            this.gson = gson;
            this.adapter = adapter;
        }

        @Override
        public RequestBody convert(T value) {
            Buffer buffer = new Buffer();
            Writer writer = new OutputStreamWriter(buffer.outputStream(), UTF_8);
            JsonWriter jsonWriter = null;
            try {
                jsonWriter = gson.newJsonWriter(writer);
                adapter.write(jsonWriter, value);
                jsonWriter.close();
                return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }
    }

}

```
> [点击查看本次git提交的详细代码](https://github.com/Knight-ZXW/MiniRetrofit/commit/59ee21c5b37dfe0f0deb9ca0376a729b2c9418a6)




### 3.5 实现CallAdapter 支持方法返回类型
继续回到Http请求的声明中，目前我们方法所支持的返回类型都是OKHttp的Call对象,而Call对象从使用上来说,目前还是有些繁琐,原生的Call对象返回的是ResponseBody还需要开发者自己处理并做转换。
```
public interface NetRestService {

    @GET("http://gank.io/api/today")
    public Call todayGank();
}
```

也许我们希望这个方法可以这样定义
```
public interface NetRestService {

    @GET("http://gank.io/api/today")
    public TodayGankResponse todayGank();
}
```


也许我们可以在框架内部通过判断方法的返回类型是不是Call对象,如果不是,就在框架内部直接同步调用网络请求得到响应的Json内容后直接转换成JavaBean对象作为方法的返回值，但是这个设想存在这样几个问题

1. 要实现直接返回Http结果则方法调用是同步调用,如果在主线程做IO请求肯定是不合理的

2. 如果内部IO异常了,或者JSON转换失败了方法返回的是什么呢？为null吗？

因此更合理的话，在应用我们希望的是返回一个包装的支持异步调用的类型;

比如我们的项目自己新增了一个支持异步调用的NetCall抽象接口
```
/**
 * Created by zhuoxiuwu
 * on 2019/4/26
 * email nimdanoob@gmail.com
 */
public interface NetCall<T> {
    public void execute(NetCallback<T> netCallback);
}

```

我们希望我们的方法可以这样申明
```
public interface NetRestService {

    @GET("http://gank.io/api/today")
    public NetCall<TodayGankResponse> todayGank();
}
```

这样的话在应用层我们调用的时候就可以像这样使用
```java
        retrofit.createService(NetRestService.class).todayGank()
                .execute(new NetCallback<TodayGankResponse>() {
                    @Override
                    public void onFailure(Exception e) {

                    }

                    @Override
                    public void onSuccess(TodayGankResponse data) {
                        Log.d("RetrofitTest","调用成功,结果为"+data.toString());
                        showHttpResult(data.toString());
                    }
                });
```


那么具体要怎么实现呢，这个功能相当于让Retrofit框架支持 对方法返回类型的自定义适配；和Converter接口一样的思路，我们在框架可以定义一个 CallAdapter接口,让应用层来具体实现并传入
```java
package retrofit2;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.Call;

/**
 * Created by zhuoxiuwu
 * on 2019/4/26
 * email nimdanoob@gmail.com
 */
public interface CallAdapter<T> {

    T adapt(Call call);


    abstract class Factory {

        public abstract CallAdapter<?> get(Type returnType,Retrofit retrofit);

        /**
         * 这是一个框架提供给开发者的util方法
         * 用于获取类型的泛型上的类型
         * 比如 Call<Response> 则 第0个泛型是Response.class
         */
        protected static Type getParameterUpperBound(int index, ParameterizedType type) {
            return Utils.getParameterUpperBound(index, type);
        }

        /**
         * 获取Type对应的Class
         * @param type
         * @return
         */
        protected static Class<?> getRawType(Type type) {
            return Utils.getRawType(type);
        }
    }
}

```

在应用层我们可以实现一个NetCallAdapter，支持Call对象到 NetCall对象的转换
```java
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
```

在**Retrofit**类中，我们添加了判断方法返回类型的逻辑，如果发现方法的返回类型不是Call类型，则使用CallAdapter做转换
```java
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
```

### 框架的后续实现及优化
到目前为止我们已经实现了一个简单的retrofit框架，也许代码不够精简，边界处理没有十分严谨，但已经初具雏形。我们可以继续思考现有项目的不足 添加更多的支持。

比如在网络请求方面目前只支持GET、POST，那么我们后续需要添加更多请求方式的支持

在Retrofit对象的构造上，目前我们的构造函数传入了3个对象，如果后续有更多的参数需要配置化，那么我们可以使用 Builder设计模式来构建Retrofit

在CallAdapter的设计上，我们目前只支持传入一个CallAdapterFactory，因此方法的返回类型除了原生的Call对象外 只支持应用开发者新增一个。实际上,这不太合理，因此这部分我们可以支持开发者传入一个列表，在内部我们迭代这个List<CallAdapter> ，遍历过程中调用**CallAdapter<?> get(Type returnType,Retrofit retrofit);**,如果这个Factory返回了null，则说明它对该类型不支持，则继续调用下个CallFactory 直到找到合适的
    
```

    abstract class Factory {
        //如果返回了null 则该Factory不支持该returnType的转换
        public abstract CallAdapter<?> get(Type returnType,Retrofit retrofit);
    }
```

在框架的性能优化上，目前我们每次调用 **createService(final Class<T> service)** 都是返回一个新的代理类，其实我们可以建立一个 Service类类型到该类型代理对象的Map缓存，如果发现缓存池有则直接复用该对象。更进一步的思考，我们是否可以以Method方法为纬度,每次调用一个抽象方法时我们解析该方法上的注解生成一个自己的**ServiceMethod**类对象并加到缓存池中，下次调用同样的方法时，我们就不需要解析注解了，而是直接使用内部的ServiceMethod
```
static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
            Retrofit retrofit, Method method){

        //...解析method并生成一个HttpServiceMethod对象
        return null;
    }

    //框架内部表示一个Http方法调用的类
    class HttpServiceMethod<ResponseT, ReturnT>{
        //arguments
        //解析方法及注解并把信息保存下来，同样的方法就不需要二次解析了


        //args为方法的参数，每次方法调用时只需要处理不同的参数
        //具体是POST 方法还是GET方法， callAdapter具体使用哪一个 都已经确定下来了
        final  ReturnT invoke(Object[] args) {
            return null;
        }
    }

```


以上提出的一些优化点，大家可以自己先思考实现并重新阅读写Retrofit源码来加深自己的理解。从整个思考流程及实现上来看Retrofit的实现并不复杂，但是从实现一个简单可用的网络封装库到实现一个拓展性强、职责分离的框架，中间的过程还是有很多细节的,如果你看完了这篇文章可以再抽1个小时左右的时间重新看下Retorift框架的源码相信从中还会有更多的收获.




