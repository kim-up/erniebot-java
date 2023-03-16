package com.kimup.erniebot.core;

import com.kimup.erniebot.api.ApiResponse;
import io.reactivex.Single;
import retrofit2.http.GET;

/**
 * @author kim-up
 * @date 2023/3/16 3:09 下午
 */
public interface ErnieBotApi {

    // TODO
    @GET("/")
    Single<ApiResponse> list();
}
