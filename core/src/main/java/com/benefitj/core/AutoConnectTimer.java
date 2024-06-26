package com.benefitj.core;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自动重连的定时器
 */
public class AutoConnectTimer {

  /**
   * 连接器
   */
  public interface Connector {

    /**
     * 是否已连接
     */
    boolean isConnected();

    /**
     * 连接
     */
    void doConnect();
  }



  private final AtomicReference<ScheduledFuture<?>> timerRef = new AtomicReference<>();
  /**
   * 是否自动重连
   */
  private volatile boolean autoConnect = true;
  /**
   * 尝试间隔
   */
  private int period = 10;
  /**
   * 时间单位
   */
  private TimeUnit unit = TimeUnit.SECONDS;
  /**
   * 连接锁
   */
  private final AtomicBoolean connectLock = new AtomicBoolean(false);

  public AutoConnectTimer() {
    this(true);
  }

  public AutoConnectTimer(boolean autoConnect) {
    this.setAutoConnect(autoConnect);
  }

  public AutoConnectTimer(boolean autoConnect, int period, TimeUnit unit) {
    this.autoConnect = autoConnect;
    this.period = period;
    this.unit = unit;
  }

  /**
   * 重连
   *
   * @param socket 客户端
   */
  public void start(Connector socket) {
    if (isAutoConnect()) {
      synchronized (this) {
        if (timerRef.get() == null) {
          this.timerRef.set(EventLoop.asyncIOFixedRate(() -> {
            if (connectLock.compareAndSet(false, true)) {
              try {
                if (socket.isConnected()) {
                  EventLoop.cancel(timerRef.getAndSet(null));
                  return;
                }
                socket.doConnect();
              } finally {
                connectLock.set(false);
              }
            }
          }, 0, getPeriod(), getUnit()));
        }
      }
    }
  }

  public void stop() {
    synchronized (this) {
      EventLoop.cancel(timerRef.getAndSet(null));
    }
  }

  public boolean isAutoConnect() {
    return autoConnect;
  }

  public AutoConnectTimer setAutoConnect(boolean autoConnect) {
    this.autoConnect = autoConnect;
    return this;
  }

  public int getPeriod() {
    return period;
  }

  public AutoConnectTimer setPeriod(int period) {
    this.period = period;
    return this;
  }

  public AutoConnectTimer setPeriod(int period, TimeUnit unit) {
    return this.setPeriod(period).setUnit(unit);
  }

  public TimeUnit getUnit() {
    return unit;
  }

  public AutoConnectTimer setUnit(TimeUnit unit) {
    this.unit = unit;
    return this;
  }

  public static final AutoConnectTimer NONE = new AutoConnectTimer(false) {
    @Override
    public boolean isAutoConnect() {
      return false;
    }

    @Override
    public AutoConnectTimer setAutoConnect(boolean autoConnect) {
      return this;
    }

    @Override
    public AutoConnectTimer setPeriod(int period) {
      return this;
    }

    @Override
    public AutoConnectTimer setUnit(TimeUnit unit) {
      return this;
    }
  };

}