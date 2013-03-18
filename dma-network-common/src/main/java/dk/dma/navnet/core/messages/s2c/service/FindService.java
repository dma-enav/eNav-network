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
package dk.dma.navnet.core.messages.s2c.service;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.s2c.ReplyMessage;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class FindService extends ReplyMessage<FindServiceResult> {

    final String serviceName;

    final int meters;

    /**
     * @return the meters
     */
    public int getMeters() {
        return meters;
    }

    final int max;

    public FindService(TextMessageReader pr) throws IOException {
        super(MessageType.FIND_SERVICE, pr);
        this.serviceName = requireNonNull(pr.takeString());
        this.meters = pr.takeInt();
        this.max = pr.takeInt();
    }

    /**
     * @param messageType
     */
    public FindService(String serviceName, int meters, int max) {
        super(MessageType.FIND_SERVICE);
        this.serviceName = requireNonNull(serviceName);
        this.meters = meters;
        this.max = max;

    }

    public int getMax() {
        return max;
    }

    public String getServiceName() {
        return serviceName;
    }

    /** {@inheritDoc} */
    @Override
    protected void write0(TextMessageWriter w) {
        w.writeString(serviceName);
        w.writeInt(meters);
        w.writeInt(max);
    }

    public FindServiceResult createReply(String[] array) {
        return new FindServiceResult(getReplyTo(), array);
    }
}
