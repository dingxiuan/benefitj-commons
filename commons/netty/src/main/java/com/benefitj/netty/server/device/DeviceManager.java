package com.benefitj.netty.server.device;

import java.util.Map;

/**
 * 设备管理
 *
 * @param <D> 设备类型
 */
public interface DeviceManager<D extends Device> extends Map<String, D> {

  /**
   * 移除设备，通知设备下线
   *
   * @param key 设备ID
   * @return 返回被移除的设备
   */
  @Override
  D remove(Object key);

  /**
   * 移除设备
   *
   * @param key    设备ID
   * @param notify 是否通知设备被移除
   * @return 返回被移除的设备
   */
  D remove(Object key, boolean notify);

  /**
   * 获取设备状态监听
   */
  DeviceStateChangeListener<D> getStateChangeListener();

  /**
   * 设备状态监听
   *
   * @param listener 监听
   */
  void setStateChangeListener(DeviceStateChangeListener<D> listener);

}
