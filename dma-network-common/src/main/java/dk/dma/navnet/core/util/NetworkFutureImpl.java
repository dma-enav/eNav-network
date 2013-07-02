/*
 * Copyright (c) 2008 Kasper Nielsen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.navnet.core.util;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jsr166e.CompletableFuture;
import dk.dma.enav.communication.ConnectionFuture;
import dk.dma.enav.util.function.BiConsumer;

/**
 * 
 * @author Kasper Nielsen
 */
public class NetworkFutureImpl<T> extends CompletableFuture<T> implements ConnectionFuture<T> {
    final ScheduledExecutorService ses;

    final String requestId;

    NetworkFutureImpl(ScheduledExecutorService ses) {
        this.ses = ses;
        this.requestId = "fixme";
    }

    public NetworkFutureImpl<T> timeout(final long timeout, final TimeUnit unit) {
        // timeout parameters checked by ses.schedule
        final NetworkFutureImpl<T> cf = new NetworkFutureImpl<>(requireNonNull(ses, "executor is null"));
        final Future<?> f;
        try {
            f = ses.schedule(new Runnable() {
                public void run() {
                    if (!isDone()) {
                        cf.completeExceptionally(new TimeoutException("Timed out after " + timeout + " "
                                + unit.toString().toLowerCase()));
                    }
                }
            }, timeout, unit);
        } catch (RejectedExecutionException e) {
            // Unfortunately TimeoutException does not allow exceptions in its constructor
            cf.completeExceptionally(new RuntimeException("Could not scedule task, ", e));
            return cf;
        }
        if (f.isCancelled()) {
            cf.completeExceptionally(new RuntimeException("Could not scedule task"));
        }
        // Check if scheduler is shutdown
        // do it after cf.f is set (reversed in shutdown code)
        handle(new BiFun<T, Throwable, Void>() {
            public Void apply(T t, Throwable throwable) {
                // Users must manually purge if many outstanding tasks
                f.cancel(false);
                if (throwable != null) {
                    cf.completeExceptionally(throwable);
                } else {
                    cf.complete(t);
                }
                return null;
            }
        });
        return cf;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(final BiConsumer<T, Throwable> consumer) {
        requireNonNull(consumer, "consumer is null");
        handle(new BiFun<T, Throwable, Void>() {
            public Void apply(T a, Throwable b) {
                consumer.accept(a, b);
                return null;
            }
        });
    }

    // /** {@inheritDoc} */
    // @Override
    // public String getRequestId() {
    // return requestId;
    // }
}
