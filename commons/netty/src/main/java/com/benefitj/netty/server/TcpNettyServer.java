package com.benefitj.netty.server;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.UnixChannelOption;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TCP 服务
 */
public class TcpNettyServer extends AbstractNettyServer<TcpNettyServer> {

  public static final Map<ChannelOption<?>, Object> DEFAULT_OPTIONS;
  public static final Map<ChannelOption<?>, Object> DEFAULT_CHILD_OPTIONS;

  static {
    // options
    Map<ChannelOption<?>, Object> optionMap = new HashMap<>();
    optionMap.put(ChannelOption.SO_REUSEADDR, true);
    optionMap.put(ChannelOption.SO_BACKLOG, 1024);
    DEFAULT_OPTIONS = Collections.unmodifiableMap(optionMap);

    // childOptions
    Map<ChannelOption<?>, Object> childOptionMap = new HashMap<>();
    childOptionMap.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    childOptionMap.put(ChannelOption.SO_RCVBUF, (1024 << 10) * 4);
    childOptionMap.put(ChannelOption.SO_SNDBUF, (1024 << 10) * 4);
    childOptionMap.put(ChannelOption.TCP_NODELAY, true);
    childOptionMap.put(ChannelOption.SO_KEEPALIVE, true);
    childOptionMap.put(ChannelOption.AUTO_READ, true);
    childOptionMap.put(ChannelOption.AUTO_CLOSE, true);
    childOptionMap.put(ChannelOption.ALLOW_HALF_CLOSURE, true);
    childOptionMap.put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);
    DEFAULT_CHILD_OPTIONS = Collections.unmodifiableMap(childOptionMap);
  }

  public TcpNettyServer() {
    super();
  }

  @Override
  public TcpNettyServer useDefaultConfig() {
    if (useLinuxNativeEpoll()) {
      this.executeWhileNull(this.workerGroup(), () -> this.group(new EpollEventLoopGroup(), new EpollEventLoopGroup()));
      this.executeWhileNull(this.channelFactory(), () -> this.channel(EpollServerSocketChannel.class));
      this.option(UnixChannelOption.SO_REUSEPORT, true);
    } else {
      this.executeWhileNull(this.workerGroup(), () -> this.group(new NioEventLoopGroup(), new NioEventLoopGroup()));
      this.executeWhileNull(this.channelFactory(), () -> channel(NioServerSocketChannel.class));
    }

    // add options
    Map<ChannelOption<?>, Object> oMap = DEFAULT_OPTIONS;
    Map<ChannelOption<?>, Object> options = new HashMap<>(oMap.size() + 16);
    options.putAll(oMap);
    options.putAll(options());
    options(options);

    // add child options
    Map<ChannelOption<?>, Object> coMap = DEFAULT_CHILD_OPTIONS;
    Map<ChannelOption<?>, Object> childOptions = new HashMap<>(coMap.size() + 16);
    childOptions.putAll(coMap);
    childOptions.putAll(childOptions());
    childOptions(childOptions);
    return self();
  }

  @Deprecated
  @Override
  public TcpNettyServer group(EventLoopGroup group) {
    return super.group(group, group);
  }

}
