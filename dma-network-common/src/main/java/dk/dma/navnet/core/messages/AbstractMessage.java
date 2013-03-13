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
package dk.dma.navnet.core.messages;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.TreeMap;

import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractMessage {
    static final MessageType[] TYPES;

    static {
        TreeMap<Integer, MessageType> m = new TreeMap<>();
        for (MessageType mt : MessageType.values()) {
            m.put(mt.type, mt);
        }
        TYPES = new MessageType[m.lastKey() + 1];
        for (Entry<Integer, MessageType> e : m.entrySet()) {
            TYPES[e.getKey()] = e.getValue();
        }
    }

    /** The type of message. */
    private final MessageType messageType;

    /**
     * Creates a new AbstractMessage.
     * 
     * @param messageType
     *            the type of message
     * @throws NullPointerException
     *             if the specified message type is null
     */
    public AbstractMessage(MessageType messageType) {
        this.messageType = requireNonNull(messageType);
    }

    /**
     * Returns the message type.
     * 
     * @return the message type
     */
    public final MessageType getMessageType() {
        return messageType;
    }

    static Class<? extends AbstractMessage> getType(int type) {
        Class<? extends AbstractMessage> cl = TYPES[type].cl;
        return cl;
    }

    public String toJSON() {
        TextMessageWriter w = new TextMessageWriter();
        w.writeInt(messageType.type);
        write(w);
        String s = w.sb.append("]").toString();
        return s;
    }

    protected abstract void write(TextMessageWriter w);

    public static AbstractMessage read(String msg) throws IOException {
        TextMessageReader pr = new TextMessageReader(msg);
        int type = pr.takeInt();
        // TODO guard indexes
        Class<? extends AbstractMessage> cl = getType(type);
        try {
            return cl.getConstructor(TextMessageReader.class).newInstance(pr);
        } catch (ReflectiveOperationException e) {
            throw new IOException(e);
        }
    }
}
