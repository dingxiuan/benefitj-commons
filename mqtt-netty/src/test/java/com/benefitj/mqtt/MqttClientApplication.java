package com.benefitj.mqtt;

import com.benefitj.core.EventLoop;
import com.benefitj.netty.client.TcpNettyClient;
import com.benefitj.netty.handler.BiConsumerInboundHandler;
import com.benefitj.netty.log.Log4jNettyLogger;
import com.benefitj.netty.log.NettyLogger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * MQTT服务端
 */
@SpringBootApplication
public class MqttClientApplication {
  public static void main(String[] args) {
    NettyLogger.INSTANCE.setLogger(new Log4jNettyLogger());

    final Logger log = LoggerFactory.getLogger(MqttClientApplication.class);

    TcpNettyClient client = new TcpNettyClient();
    client.remoteAddress(new InetSocketAddress("127.0.0.1", 1883));
    client.handler(new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        ch.pipeline()
            .addLast(BiConsumerInboundHandler.newByteBufHandler((handler, ctx, msg) -> {
              log.info("响应: {}", new String(handler.copy(msg)));
            }));
      }
    });

    // 自动重连
    client.autoReconnect(true, 5, TimeUnit.SECONDS);

    // 注册启动时的监听
    client.addStartListeners(f -> {
      log.info("连接到远程服务: {}", f.isSuccess());
      if (f.isSuccess()) {
        ByteBuf data = Unpooled.wrappedBuffer("Hello World!".getBytes());
        client.getServeChannel().writeAndFlush(data);
      }
    });

    client.start();

    EventLoop.multi()
        .scheduleAtFixedRate(() -> {
          //log.info("client interrupted start ...");
          client.closeServeChannel();
          log.info("client interrupted end ...");
        }, 10, 10, TimeUnit.SECONDS);


    EventLoop.multi().schedule(() ->
        client.stop(future -> log.info("client stopped....")), 120, TimeUnit.SECONDS);

//    // 结束程序
//    EventLoop.single().schedule(() ->
//        System.exit(0), 10, TimeUnit.SECONDS);

  }

}
