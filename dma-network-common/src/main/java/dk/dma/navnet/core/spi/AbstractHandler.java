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

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import dk.dma.enav.communication.CloseReason;
import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.messages.c2c.AbstractRelayedMessage;
import dk.dma.navnet.core.messages.s2c.AckMessage;
import dk.dma.navnet.core.messages.s2c.ReplyMessage;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractHandler {

    final ConcurrentHashMap<Long, NetworkFutureImpl<?>> acks = new ConcurrentHashMap<>();
    final AtomicInteger ai = new AtomicInteger();

    private final Listener listener = new Listener();

    protected final ReentrantLock lock = new ReentrantLock();

    final ConcurrentHashMap<String, NetworkFutureImpl<?>> replies = new ConcurrentHashMap<>();

    volatile Session session;

    protected void closed(int statusCode, String reason) {

    }

    public void connected() {}

    protected void failed(String message) {

    }

    public final WebSocketListener getListener() {
        return listener;
    }

    protected abstract AbstractConnection client();

    void handleText0(AbstractTextMessage m) {
        if (m instanceof AckMessage) {
            AckMessage am = (AckMessage) m;
            NetworkFutureImpl<?> f = acks.remove(am.getMessageAck());
            if (f == null) {
                System.err.println("Orphaned packet with id " + am.getMessageAck() + " registered " + acks.keySet()
                        + ", local " + "" + " p = ");
                // TODO close connection with error
            } else {
                try {
                    client().handleTextReply(m, f);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // System.out.println("RELEASING " + am.getMessageAck() + ", remaining " + acks.keySet());
        } else {
            try {
                client().handleText(m);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onError(Throwable cause) {
        cause.printStackTrace();
        System.out.println("ERROR " + cause);
    }

    public final <T> NetworkFutureImpl<T> sendMessage(AbstractRelayedMessage m) {
        sendMessage((AbstractTextMessage) m);
        return null;
    }

    public final void sendMessage(AbstractTextMessage m) {
        sendRawTextMessage(m.toJSON());
    }

    public final <T> NetworkFutureImpl<T> sendMessage(ReplyMessage<T> m) {
        // we need to send the messages in the same order as they are numbered for now
        synchronized (ai) {
            long id = ai.incrementAndGet();
            NetworkFutureImpl<T> f = new NetworkFutureImpl<>();
            acks.put(id, f);
            m.setReplyTo(id);
            sendMessage((AbstractTextMessage) m);
            return f;
        }
    }

    public final void sendRawTextMessage(String m) {
        Session s = session;
        RemoteEndpoint r = s == null ? null : s.getRemote();
        if (r != null) {
            try {
                System.out.println("Sending " + m);
                r.sendString(m);
            } catch (IOException e) {
                onError(e);
                failed(e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Dropping message " + m);
        }
    }

    public final void tryClose(int statusCode, String reason) {
        Session s = session;
        if (s != null) {
            try {
                s.close(statusCode, reason);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    final class Listener implements WebSocketListener {

        /** {@inheritDoc} */
        @Override
        public final void onWebSocketBinary(byte[] payload, int offset, int len) {
            tryClose(CloseReason.BAD_DATA.getId(), "Expected text only");
        }

        /** {@inheritDoc} */
        @Override
        public final void onWebSocketClose(int statusCode, String reason) {
            session = null;
            closed(statusCode, reason);
            System.out.println("CLOSED:" + reason);
        }

        /** {@inheritDoc} */
        @Override
        public final void onWebSocketConnect(Session s) {
            session = s;
            connected();
        }

        /** {@inheritDoc} */
        @Override
        public final void onWebSocketError(Throwable cause) {
            onError(cause);
        }

        /** {@inheritDoc} */
        @Override
        public final void onWebSocketText(String message) {
            System.out.println("Received: " + message);
            try {
                AbstractTextMessage m = AbstractTextMessage.read(message);
                m.setReceivedRawMesage(message);
                handleText0(m);
            } catch (Throwable e) {
                e.printStackTrace();
                tryClose(5004, e.getMessage());
            }
        }
    }
}
