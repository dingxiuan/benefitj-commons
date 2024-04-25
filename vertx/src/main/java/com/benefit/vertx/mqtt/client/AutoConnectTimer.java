package com.benefit.vertx.mqtt.client;

import com.benefitj.core.EventLoop;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自动重连的定时器
 */
public class AutoConnectTimer {

  private final AtomicReference<ScheduledFuture<?>> timerRef = new AtomicReference<>();
  /**
   * 尝试间隔
   */
  private int period = 10;
  /**
   * 时间单位
   */
  private TimeUnit unit = TimeUnit.SECONDS;

  /**
   * 是否自动重连
   */
  private volatile boolean autoConnect = true;

  public AutoConnectTimer() {
    this(true);
  }

  public AutoConnectTimer(boolean autoConnect) {
    this.setAutoConnect(autoConnect);
  }

  /**
   * 重连
   *
   * @param client 客户端
   */
  public void start(VertxMqttClient client) {
    if (isAutoConnect()) {
      synchronized (this) {
        if (timerRef.get() == null) {
          this.timerRef.set(EventLoop.asyncIOFixedRate(() -> {
            if (client.isConnected()) {
              EventLoop.cancel(timerRef.getAndSet(null));
              return;
            }
            client.reconnect();
          }, 1, getPeriod(), getUnit()));
        }
      }
    }
  }

  public void stop() {
    synchronized (this) {
      EventLoop.cancel(timerRef.getAndSet(null));
    }
  }

  public int getPeriod() {
    return period;
  }

  public AutoConnectTimer setPeriod(int period) {
    this.period = period;
    return this;
  }

  public TimeUnit getUnit() {
    return unit;
  }

  public AutoConnectTimer setUnit(TimeUnit unit) {
    this.unit = unit;
    return this;
  }

  public boolean isAutoConnect() {
    return autoConnect;
  }

  public AutoConnectTimer setAutoConnect(boolean autoConnect) {
    this.autoConnect = autoConnect;
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
