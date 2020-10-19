package com.benefitj.netty.handler;

import com.benefitj.netty.ByteBufCopy;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * 监听 Channel 的状态
 */
public class ActiveChangeChannelHandler extends ChannelDuplexHandler {

  private final ByteBufCopy bufCopy = new ByteBufCopy();

  private ActiveStateListener listener;

  public ActiveChangeChannelHandler(ActiveStateListener listener) {
    this.listener = listener;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    getListener().onChanged(this, ctx, ActiveState.ACTIVE);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
    getListener().onChanged(this, ctx, ActiveState.INACTIVE);
  }

  public ActiveStateListener getListener() {
    return listener;
  }

  public void setListener(ActiveStateListener listener) {
    this.listener = listener;
  }

  public ByteBufCopy getBufCopy() {
    return bufCopy;
  }

  public interface ActiveStateListener {
    /**
     * 监听
     *
     * @param ctx     上下文
     * @param handler 当前的Handler
     * @param state   状态
     */
    void onChanged(ActiveChangeChannelHandler handler, ChannelHandlerContext ctx, ActiveState state);
  }


  /**
   * 创建 Handler
   *
   * @param listener 监听
   * @return 返回创建的Handler
   */
  public static ActiveChangeChannelHandler newHandler(ActiveStateListener listener) {
    return new ActiveChangeChannelHandler(listener);
  }
}
