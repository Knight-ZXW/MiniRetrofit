package retrofit2;

import java.lang.reflect.Type;

import javax.annotation.Nullable;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

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

        public Converter<ResponseBody, ?> responseBodyConverter(Type type){
            return null;
        }

    }
}
