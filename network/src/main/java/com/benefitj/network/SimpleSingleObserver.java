package com.benefitj.network;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class SimpleSingleObserver<T> implements SingleObserver<T> {
  @Override
  public void onSubscribe(@NotNull Disposable d) {
    d.dispose();
  }

  @Override
  public abstract void onSuccess(@NotNull T t);

  @Override
  public void onError(@NotNull Throwable e) {
    e.printStackTrace();
  }


  public static <T> SimpleSingleObserver<T> create(Consumer<T> consumer) {
    return create(consumer, Throwable::printStackTrace);
  }

  public static <T> SimpleSingleObserver<T> create(Consumer<T> consumer, Consumer<Throwable> errorConsumer) {
    return new SimpleSingleObserver<>() {

      @Override
      public void onSuccess(@NotNull T t) {
        consumer.accept(t);
      }

      @Override
      public void onError(Throwable e) {
        errorConsumer.accept(e);
      }
    };
  }
}
