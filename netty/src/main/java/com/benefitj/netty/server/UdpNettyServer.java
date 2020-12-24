package com.benefitj.netty.server;

import com.benefitj.netty.server.channel.NioDatagramServerChannel;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.unix.UnixChannelOption;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * UDP服务
 */
public class UdpNettyServer extends AbstractNettyServer<UdpNettyServer> {

  private long readerTimeout = 60;
  private long writeTimeout = 0;
  private TimeUnit timeoutUnit = TimeUnit.SECONDS;

  public UdpNettyServer() {
    super();
  }

  @Override
  public UdpNettyServer useDefaultConfig() {
    if (useLinuxNativeEpoll()) {
      this.executeWhileNull(bossGroup(), () ->
          this.group(new NioEventLoopGroup(1), new DefaultEventLoopGroup()));
      this.executeWhileNull(channelFactory(), () -> this.channel(NioDatagramServerChannel.class));
      this.option(UnixChannelOption.SO_REUSEPORT, false);
    } else {
      this.executeWhileNull(bossGroup(), () ->
          this.group(new NioEventLoopGroup(1), new DefaultEventLoopGroup()));
      this.executeWhileNull(channelFactory(), () -> this.channel(NioDatagramServerChannel.class));
    }
    this.option(ChannelOption.SO_BROADCAST, true);
    this.option(ChannelOption.SO_REUSEADDR, false);
    Map<ChannelOption<?>, Object> options = new HashMap<>(options());
    // 默认4MB，数据量较大，缓冲区较小会导致丢包
    options.putIfAbsent(ChannelOption.SO_RCVBUF, (1024 << 10) * 4);
    options.putIfAbsent(ChannelOption.SO_SNDBUF, (1024 << 10) * 4);
    this.options(options);

    return self();
  }

  /**
   * 设置接收的缓冲区大小
   */
  public UdpNettyServer soRcvbufSize(Integer soRcvbufSize) {
    this.option(ChannelOption.SO_RCVBUF, (1024 << 10) * soRcvbufSize);
    return self();
  }

  /**
   * 设置发送的缓冲区大小
   */
  public UdpNettyServer soSndbufSize(Integer soSndbufSize) {
    this.option(ChannelOption.SO_SNDBUF, (1024 << 10) * soSndbufSize);
    return self();
  }


  @Override
  protected ChannelFuture startOnly(ServerBootstrap bootstrap) {
    return super.startOnly(bootstrap).addListener(f ->
        ((NioDatagramServerChannel) getServeChannel())
            .idle(readerTimeout(), writerTimeout(), timeoutUnit()));
  }

  public long readerTimeout() {
    return readerTimeout;
  }

  public UdpNettyServer readerTimeout(long readerTimeout) {
    this.readerTimeout = readerTimeout;
    return self();
  }

  public long writerTimeout() {
    return writeTimeout;
  }

  public UdpNettyServer writerTimeout(long writeTimeout) {
    this.writeTimeout = writeTimeout;
    return self();
  }

  public TimeUnit timeoutUnit() {
    return timeoutUnit;
  }

  public UdpNettyServer timeoutUnit(TimeUnit timeoutUnit) {
    this.timeoutUnit = timeoutUnit;
    return self();
  }

  public UdpNettyServer idle(long reader, long writer, TimeUnit timeoutUnit) {
    this.readerTimeout(reader);
    this.writerTimeout(writer);
    this.timeoutUnit(timeoutUnit);
    return self();
  }

  /**
   * 服务端类型
   */
  @Override
  public final ServerType serverType() {
    return ServerType.UDP;
  }

}