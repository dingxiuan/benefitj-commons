package com.benefitj.http;

import com.benefitj.core.SingletonSupplier;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

public class HttpHelper {

  static SingletonSupplier<HttpHelper> singleton = SingletonSupplier.of(HttpHelper::new);

  public static HttpHelper get() {
    return singleton.get();
  }

  private OkHttpClient client = new OkHttpClient.Builder()
      .connectTimeout(Duration.ofSeconds(3))
      .readTimeout(Duration.ofSeconds(300))
      .writeTimeout(Duration.ofSeconds(300))
      .build();

  public HttpHelper() {
  }

  public OkHttpClient getClient() {
    return client;
  }

  public HttpHelper setClient(OkHttpClient client) {
    this.client = client;
    return this;
  }

  /**
   * 发送GET请求
   *
   * @param url 请求地址
   * @return 返回响应
   */
  public Response get(String url) {
    return get(url, Collections.emptyMap());
  }

  /**
   * 发送GET请求
   *
   * @param url     请求地址
   * @param headers 请求头
   * @return 返回响应
   */
  public Response get(String url, Map<String, String> headers) {
    return request(new Request.Builder()
        .url(url)
        .headers(Headers.of(headers))
        .get()
        .build());
  }

  /**
   * 发送POST请求
   *
   * @param url 请求地址
   * @return 返回响应
   */
  public Response post(String url, File file) {
    return post(url, RequestBody.create(file, MediaType.parse("application/json")));
  }

  /**
   * 发送POST请求
   *
   * @param url  请求地址
   * @param body 请求体
   * @return 返回响应
   */
  public Response post(String url, RequestBody body) {
    return post(url, body, Collections.emptyMap());
  }

  /**
   * 发送POST请求
   *
   * @param url     请求地址
   * @param body    请求体
   * @param headers 请求头
   * @return 返回响应
   */
  public Response post(String url, RequestBody body, Map<String, String> headers) {
    return request(new Request.Builder()
        .url(url)
        .headers(Headers.of(headers))
        .post(body)
        .build());
  }

  /**
   * 发送请求
   *
   * @param request 请求
   * @return 返回响应
   */
  public Response request(Request request) {
    try {
      Call call = getClient().newCall(request);
      return call.execute();
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }


}