package com.kimup.erniebot.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.kimup.erniebot.api.ApiError;
import com.kimup.erniebot.api.ApiHttpException;
import com.kimup.erniebot.core.AuthenticationInterceptor;
import com.kimup.erniebot.core.ErnieBotApi;
import io.reactivex.Single;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class ErnieBotService {

    private static final String BASE_URL = "https://api.baidu.com/";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final ObjectMapper errorMapper = defaultObjectMapper();

    private final ErnieBotApi api;

    /**
     * Creates a new ErnieBotService that wraps ErnieBotApi
     *
     * @param token ErnieBotApi token string
     */
    public ErnieBotService(final String token) {
        this(token, DEFAULT_TIMEOUT);
    }

    /**
     * Creates a new ErnieBotService that wraps ErnieBotApi
     *
     * @param token   ErnieBotApi token string
     * @param timeout http read timeout, Duration.ZERO means no timeout
     */
    public ErnieBotService(final String token, final Duration timeout) {
        this(buildApi(token, timeout));
    }

    /**
     * Creates a new ErnieBotService that wraps ErnieBotApi. Use this if you need more customization.
     *
     * @param api ErnieBotApi instance to use for all methods
     */
    public ErnieBotService(final ErnieBotApi api) {
        this.api = api;
    }

    /**
     * Calls the api, returns the response, and parses error messages if the request fails
     */
    public static <T> T execute(Single<T> apiCall) {
        try {
            return apiCall.blockingGet();
        } catch (HttpException e) {
            try {
                if (e.response() == null || e.response().errorBody() == null) {
                    throw e;
                }
                String errorBody = e.response().errorBody().string();

                ApiError error = errorMapper.readValue(errorBody, ApiError.class);
                throw new ApiHttpException(error, e, e.code());
            } catch (IOException ex) {
                // couldn't parse error
                throw e;
            }
        }
    }

    public static ErnieBotApi buildApi(String token, Duration timeout) {
        Objects.requireNonNull(token, "Token required");
        ObjectMapper mapper = defaultObjectMapper();
        OkHttpClient client = defaultClient(token, timeout);
        Retrofit retrofit = defaultRetrofit(client, mapper);

        return retrofit.create(ErnieBotApi.class);
    }

    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }

    public static OkHttpClient defaultClient(String token, Duration timeout) {
        return new OkHttpClient.Builder()
            .addInterceptor(new AuthenticationInterceptor(token))
            .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
            .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .build();
    }

    public static Retrofit defaultRetrofit(OkHttpClient client, ObjectMapper mapper) {
        return new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();
    }
}
