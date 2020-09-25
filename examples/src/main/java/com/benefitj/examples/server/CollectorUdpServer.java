package com.benefitj.examples.server;

import com.benefitj.core.DateFmtter;
import com.benefitj.core.DefaultThreadFactory;
import com.benefitj.core.EventLoop;
import com.benefitj.core.HexUtils;
import com.benefitj.netty.ByteBufCopy;
import com.benefitj.netty.adapter.BiConsumerChannelInboundHandler;
import com.benefitj.netty.server.UdpNettyServer;
import com.benefitj.netty.server.channel.NioDatagramServerChannel;
import com.benefitj.netty.server.device.DeviceStateChangeListener;
import com.benefitj.netty.server.udp.DefaultUdpDeviceManager;
import com.benefitj.netty.server.udp.OnlineDeviceExpireExecutor;
import com.benefitj.netty.server.udp.UdpDeviceManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * 采集器服务
 */
@Component
public class CollectorUdpServer extends UdpNettyServer {

  private static final Logger log = LoggerFactory.getLogger(CollectorUdpServer.class.getSimpleName());

  private static final EventLoop single = EventLoop.newSingle(true);

  private ByteBufCopy cache = new ByteBufCopy();

  private final UdpDeviceManager<CollectorDevice> clients;
  private final OnlineDeviceExpireExecutor executor;

  /**
   * 缓冲区大小
   */
  @Value("#{ @environment['collector.server.option.soRcvbufSize'] ?: 8 }")
  private Integer soRcvbufSize;
  /**
   * 过期时长
   */
  @Value("#{ @environment['collector.server.device.expire'] ?: 5000 }")
  private Integer expire;
  /**
   * 检查间隔
   */
  @Value("#{ @environment['collector.server.device.delay'] ?: 1000 }")
  private Integer delay;

  public CollectorUdpServer() {
    this.clients = DefaultUdpDeviceManager.newInstance();
    this.executor = new OnlineDeviceExpireExecutor(clients);
  }

  @Override
  public UdpNettyServer useDefaultConfig() {
    this.group(
        new NioEventLoopGroup(1, new DefaultThreadFactory("boss-", "-t-"))
        , new DefaultEventLoopGroup(new DefaultThreadFactory("worker-", "-t-")));

    // 接收数据的缓冲区大小，会直接影响到UDP是否被有效接收
    this.option(ChannelOption.SO_RCVBUF, (1024 << 10) * soRcvbufSize);

    clients.setDelay(delay);
    clients.setExpire(expire);

    clients.setStateChangeListener(new DeviceStateChangeListener<CollectorDevice>() {
      @Override
      public void onAddition(String id, CollectorDevice newDevice, @Nullable CollectorDevice oldDevice) {
        single.execute(() ->
            log.info("新客户端上线: {}, oldClient: {}", newDevice, oldDevice));
        if (oldDevice != null) {
          oldDevice.stopTimer();
        }
      }

      @Override
      public void onRemoval(String id, CollectorDevice device) {
        device.stopTimer();
        single.execute(() ->
            log.info("客户端下线: {}, duration: {}", device, DateFmtter.now() - device.getRecvTime()));
      }
    });

    this.handler(new ChannelInboundHandlerAdapter() {

      ScheduledFuture<?> future;

      @Override
      public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        executor.start();
        future = ctx.executor().scheduleAtFixedRate(() ->
            log.info("设备数量: {}, children channel: {}"
                , clients.size()
                , ((NioDatagramServerChannel) ctx.channel()).children().size()
            ), 1, 5, TimeUnit.SECONDS);
      }

      @Override
      public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        executor.stop();
        future.cancel(true);
      }
    });

    int printLevel = HexUtils.bytesToInt(HexUtils.hexToBytes("0100034A"));

    this.childHandler(new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        ch.pipeline()
            .addLast(new BiConsumerChannelInboundHandler<>(ByteBuf.class, (ctx, msg) -> {
              byte[] data = cache.copy(msg);

              String deviceId = CollectorHelper.getHexDeviceId(data);
              CollectorDevice client = clients.get(deviceId);
              if (client == null) {
                clients.put(deviceId, client = new CollectorDevice(deviceId, ctx.channel()));
                client.setDeviceSn(HexUtils.bytesToInt(HexUtils.hexToBytes(deviceId)));
                client.startTimer();
              }
              // 重置接收导数据的时间
              client.setRecvTimeNow();

              if (PacketType.isRealtime(CollectorHelper.getType(data))) {
                // 更新包序号
                int packetSn = CollectorHelper.getPacketSn(data);
                client.refresh(packetSn);
                // 反馈
                client.send(CollectorHelper.getRealtimeFeedback(data));

                if (client.getDeviceSn() < printLevel) {
                  long onlineTime = client.getOnlineTime();
                  long time = CollectorHelper.getTime(data, 9 + 4, 9 + 9);
                  single.execute(() ->
                      log.info("send: {}, deviceId: {}, packageSn: {}, time: {}, online: {}"
                          , ctx.channel().remoteAddress()
                          , deviceId
                          , packetSn
                          , DateFmtter.fmt(time)
                          , DateFmtter.fmt(onlineTime)
                      ));
                }
              } else {
                log.info("不是实时数据包: {}", CollectorHelper.getPacketType(data));
              }

            }));
      }
    });
    return super.useDefaultConfig();
  }

}
