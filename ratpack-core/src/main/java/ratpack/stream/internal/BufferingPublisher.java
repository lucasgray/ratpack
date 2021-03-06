/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.stream.internal;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class BufferingPublisher<T> implements Publisher<T> {

  private final Publisher<T> publisher;

  public BufferingPublisher(Publisher<T> publisher) {
    this.publisher = publisher;
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    new Subscription(subscriber);
  }

  private class Subscription extends SubscriptionSupport<T> {

    // if null, using passthru
    private BufferingSubscriber bufferingSubscriber;

    private final AtomicBoolean upstreamFinished;

    private final AtomicReference<org.reactivestreams.Subscription> upstreamSubscription;
    private final AtomicBoolean requestedUpstream;

    public Subscription(Subscriber<? super T> subscriber) {
      super(subscriber);
      upstreamFinished = new AtomicBoolean();
      upstreamSubscription = new AtomicReference<>();
      requestedUpstream = new AtomicBoolean();
      start();
    }

    protected void doRequest(long n) {
      if (requestedUpstream.compareAndSet(false, true)) {
        if (n == Long.MAX_VALUE) {
          publisher.subscribe(new PassThruSubscriber());
          upstreamSubscription.get().request(n);
        } else {
          bufferingSubscriber = new BufferingSubscriber();
          publisher.subscribe(bufferingSubscriber);
          upstreamSubscription.get().request(Long.MAX_VALUE);
          bufferingSubscriber.wanted.addAndGet(n);
          bufferingSubscriber.tryDrain();
        }
      } else {
        if (bufferingSubscriber == null) {
          upstreamSubscription.get().request(n);
        } else {
          bufferingSubscriber.wanted.addAndGet(n);
          bufferingSubscriber.tryDrain();
        }
      }
    }

    @Override
    protected void doCancel() {
      org.reactivestreams.Subscription subscription = upstreamSubscription.get();
      if (subscription != null) {
        subscription.cancel();
      }
    }

    class PassThruSubscriber implements Subscriber<T> {
      @Override
      public void onSubscribe(org.reactivestreams.Subscription s) {
        upstreamSubscription.set(s);
      }

      @Override
      public void onNext(T t) {
        Subscription.this.onNext(t);
      }

      @Override
      public void onError(Throwable t) {
        Subscription.this.onError(t);
      }

      @Override
      public void onComplete() {
        Subscription.this.onComplete();
      }
    }

    class BufferingSubscriber implements Subscriber<T> {
      private final AtomicLong wanted = new AtomicLong();
      private final ConcurrentLinkedQueue<T> buffer = new ConcurrentLinkedQueue<>();
      private final AtomicBoolean draining = new AtomicBoolean();

      @Override
      public void onSubscribe(org.reactivestreams.Subscription s) {
        upstreamSubscription.set(s);
      }

      @Override
      public void onNext(T t) {
        buffer.add(t);
        tryDrain();
      }

      @Override
      public void onError(Throwable t) {
        buffer.clear();
        upstreamFinished.set(true);
        Subscription.this.onError(t);
      }

      @Override
      public void onComplete() {
        upstreamFinished.set(true);
        tryDrain();
      }

      public void tryDrain() {
        if (draining.compareAndSet(false, true)) {
          try {
            long i = wanted.get();
            while (i > 0) {
              T item = buffer.poll();
              if (item == null) {
                if (upstreamFinished.get()) {
                  Subscription.this.onComplete();
                  return;
                } else {
                  break;
                }
              } else {
                Subscription.this.onNext(item);
                i = wanted.decrementAndGet();
              }
            }
          } finally {
            draining.set(false);
          }
          if (buffer.peek() != null && wanted.get() > 0) {
            tryDrain();
          }
        }
      }

    }
  }
}

