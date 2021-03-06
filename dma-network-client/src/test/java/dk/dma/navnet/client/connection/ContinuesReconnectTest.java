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
package dk.dma.navnet.client.connection;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import dk.dma.enav.maritimecloud.MaritimeCloudClient;
import dk.dma.navnet.client.AbstractClientConnectionTest;
import dk.dma.navnet.client.broadcast.stubs.HelloWorld;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSend;

/**
 * 
 * @author Kasper Nielsen
 */
public class ContinuesReconnectTest extends AbstractClientConnectionTest {

    static final int NUMBER_OF_MESSAGES = 100;

    @Test
    public void test() throws Exception {
        final AtomicInteger ack = new AtomicInteger();
        MaritimeCloudClient c = createAndConnect();

        Runnable closer = new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run() {
                while (ack.get() < NUMBER_OF_MESSAGES) {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextLong(1000));
                        t.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        new Thread(closer).start();

        Runnable server = new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run() {
                int latestId = 0;
                Integer ackIt = null;
                while (ack.get() < NUMBER_OF_MESSAGES) {
                    TransportMessage tm = null;
                    try {
                        tm = t.take(TransportMessage.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (tm instanceof BroadcastSend) {
                        BroadcastSend bs = (BroadcastSend) tm;
                        try {
                            HelloWorld hw = (HelloWorld) bs.tryRead();
                            int id = Integer.parseInt(hw.getMessage());
                            assertEquals(++latestId, id);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        ackIt = null;
                    } else {
                        if (ackIt == null) {
                            ackIt = ThreadLocalRandom.current().nextInt(ack.get(), latestId + 1);
                            // System.out.println(ack.get() + " " + (latestId + 1) + "" + ackIt);
                        }
                        ack.set(ackIt);
                        latestId = ackIt;
                        System.out.println("CONNECTED  " + ackIt);
                        t.send(new ConnectedMessage("ABC", ackIt));
                    }
                }
            }
        };
        new Thread(server).start();


        for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
            Thread.sleep(ThreadLocalRandom.current().nextLong(100));
            c.broadcast(new HelloWorld("" + (i + 1)));
        }

    }
}
