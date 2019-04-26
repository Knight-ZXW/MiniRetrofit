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

    /**
     * 从Call类型 转换成 T
     * @param call
     * @return
     */
    T adapt(Call call);


    abstract class Factory {

        public abstract CallAdapter<?> get(Type returnType,Retrofit retrofit);

        /**
         * 获取类型的泛型上的类型
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
