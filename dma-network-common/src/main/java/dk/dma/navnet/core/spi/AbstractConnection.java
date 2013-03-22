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
package dk.dma.navnet.core.spi;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import dk.dma.enav.communication.CloseReason;
import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.messages.s2c.ReplyMessage;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractConnection {

    final ConcurrentHashMap<Long, NetworkFutureImpl<?>> acks = new ConcurrentHashMap<>();

    final AtomicInteger ai = new AtomicInteger();

    public volatile long lastConnectionAttempt;

    protected final ReentrantLock lock = new ReentrantLock();

    final ConcurrentHashMap<String, NetworkFutureImpl<?>> replies = new ConcurrentHashMap<>();

    final ScheduledExecutorService ses;

    protected volatile AbstractMessageTransport transport;

    protected AbstractConnection() {
        this.ses = null;
    }

    protected AbstractConnection(ScheduledExecutorService ses) {
        this.ses = requireNonNull(ses);
    }

    public void closeNormally() {
        transport.tryClose(CloseReason.NORMAL);
    }

    protected abstract void handleMessage(AbstractTextMessage m) throws Exception;

    protected abstract void handleMessageReply(AbstractTextMessage m, NetworkFutureImpl<?> f) throws Exception;

    public final void sendMessage(AbstractTextMessage m) {
        transport.sendMessage(m);
    }

    public final <T> NetworkFutureImpl<T> sendMessage(ReplyMessage<T> m) {
        return transport.sendMessage(m);
    }

    protected void setTransport(AbstractMessageTransport transport) {
        lock.lock();
        try {
            AbstractMessageTransport old = this.transport;
            if (old != null) {
                old.setConnection(null);
            }
            this.transport = transport;
            transport.setConnection(this);
        } finally {
            lock.unlock();
        }
    }

    protected void unknownMessage(AbstractTextMessage m) {
        System.err.println("Received an unknown message " + m.getReceivedRawMesage());
    }
}
