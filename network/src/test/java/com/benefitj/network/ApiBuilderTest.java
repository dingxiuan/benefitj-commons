package com.benefitj.network;

import com.benefitj.core.EventLoop;
import com.benefitj.core.HexUtils;
import com.benefitj.core.IOUtils;
import com.benefitj.core.DUtils;
import com.benefitj.core.file.IWriter;
import io.reactivex.Observable;
import junit.framework.TestCase;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ApiBuilderTest extends TestCase {

  private Logger log = LoggerFactory.getLogger(getClass());

  private ServiceApi api;

  public void setUp() throws Exception {
    this.api = ApiBuilder.newBuilder(ServiceApi.class)
        .setBaseUrl(ServiceApi.BASE_URL)
        .addHttpLogging(HttpLoggingInterceptor.Level.NONE)
        .setUseDefault(true) // 启用默认的转换器和适配器
        .build();
  }

  @Test
  public void testRequest() {
//    api.getJS()
//        .subscribe(js -> log.info("js: \n{}", js));

//    // 写入文件
//    api.getBody()
//        .subscribe(body -> BodyUtils.transferTo(body, new File("D:/opt/tmp/super_load-eb15f1e5a8.js")));
    // 写入文件
//    api.getImg()
//        .subscribe(body -> BodyUtils.transferTo(body, new File("D:/opt/tmp/ew4nf5737jvn.jpg_760w.png")));
    api.getImg()
        .subscribe(body -> {
          final IWriter img = IWriter.newFileWriter(IOUtils.createFile("D:/opt/tmp/ew4nf5737jvn.jpg_760w.png"));
          BodyUtils.progressResponseBody(body
              , (buf, len) -> img.write(buf, 0, len)
              , (totalLength, progress, done) ->
                  log.info("总长度: {}, 已下载: {}, 进度: {}%， done[{}]"
                      , totalLength
                      , progress
                      , DUtils.fmt((progress * 100.f) / totalLength, "0.00")
                      , done
                  ));
        });
  }

  @Test
  public void testUploadFile() {
    long start = DUtils.now();
    File file = new File("D:/develop/tools/simulator.zip");
    final AtomicInteger index = new AtomicInteger();
    api.upload(BodyUtils.progressRequestBody(file, "files", (totalLength, progress, done) -> {
          if (index.incrementAndGet() % 50 == 0 || done) {
            log.info("总长度: {}, 已上传: {}, 进度: {}%， done[{}]"
                , totalLength
                , progress
                , DUtils.fmt((progress * 100.f) / totalLength, "0.00")
                , done
            );
          }
        }))
        .subscribe(SimpleObserver.create(result -> log.info("上传结果: {}", result)));
    log.info("耗时: {}", DUtils.diffNow(start));
  }

  @Test
  public void testDownload() {
    // 下载
    long start = DUtils.now();
    api.download("simulator.zip")
        .subscribe(SimpleObserver.create(response -> {
          if (!response.isSuccessful()) {
            log.info("请求失败, {}, {}", response.code(), response.message());
            return;
          }
          // 处理响应
          final AtomicInteger index = new AtomicInteger();
          BodyUtils.progressResponseBody(response.body()
              , new File("D:/opt/tmp/simulator2.zip") // 写入文件中
              , (totalLength, progress, done) -> {
                if (index.incrementAndGet() % 50 == 0 || done) {
                  log.info("总长度: {}, 已下载: {}, 进度: {}%， done[{}]"
                      , totalLength
                      , progress
                      , DUtils.fmt((progress * 100.f) / totalLength, "0.00")
                      , done
                  );
                }
              });
        }));
    log.info("耗时: {}", DUtils.diffNow(start));
  }

  @Test
  public void testDownloadFile() {
    long start = DUtils.now();
    HttpUtils http = new HttpUtils();
    okhttp3.Response response = http.get("https://downloads.gradle-dn.com/distributions/gradle-7.3.2-all.zip");
    final AtomicInteger index = new AtomicInteger();
    BodyUtils.progressResponseBody(response.body()
        , IOUtils.createFile("D:/opt/tmp/gradle-7.3.2-all.zip")
        , (totalLength, progress, done) -> {
          if (index.incrementAndGet() % 100 == 0 || done) {
            log.info("总长度: {}, 已下载: {}, 进度: {}%， done[{}]"
                , totalLength
                , progress
                , DUtils.fmt((progress * 100.f) / totalLength, "0.00")
                , done
            );
          }
        });
    log.info("耗时: {}", DUtils.diffNow(start));
  }

  @Test
  public void testWebSocket() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);

    String url = "ws://127.0.0.1:80/api/sockets/simple";
    WebSocket socket = HttpClientHolder.newWebSocket(url, new WebSocketListener() {
      @Override
      public void onOpen(@NotNull WebSocket webSocket, @NotNull okhttp3.Response response) {
        latch.countDown();
        log.info("onOpen, code: {}", response.code());
      }

      @Override
      public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        log.info("onMessage, text: {}", text);
      }

      @Override
      public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        log.info("onMessage, bytes: {}", HexUtils.bytesToHex(bytes.toByteArray()));
      }

      @Override
      public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable e, @Nullable okhttp3.Response response) {
        log.info("onFailure, error: {}", e.getMessage());
        latch.countDown();
      }

      @Override
      public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        log.info("onClosing, code: {}, reason: {}", code, reason);
      }

      @Override
      public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        log.info("onClosed, code: {}, reason: {}", code, reason);
        latch.countDown();
      }
    });

    latch.await();
    for (int i = 0; i < 20; i++) {
      socket.send("from okhttp websocket: " + i);
      EventLoop.sleepSecond(1);
    }
    socket.close(1000, "done");

  }

  public void tearDown() throws Exception {
  }


  interface ServiceApi {

    //    String BASE_URL = "https://dss0.bdstatic.com/";
//    String BASE_URL = "http://127.0.0.1:80/api/";
    String BASE_URL = "https://image.taoguba.com.cn/";

    @GET("5aV1bjqh_Q23odCf/static/superman/js/super_load-eb15f1e5a8.js")
    Observable<String> getJS();

    @GET("5aV1bjqh_Q23odCf/static/superman/js/super_load-eb15f1e5a8.js")
    Observable<ResponseBody> getBody();

    @GET("img/2021/12/14/ew4nf5737jvn.jpg_760w.png")
    Observable<ResponseBody> getImg();


    @POST("simple/upload")
    Observable<String> upload(@Body RequestBody body);

    @GET("simple/download")
    Observable<Response<ResponseBody>> download(@Query("filename") String filename);

  }
}