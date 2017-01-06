package com.au.mit.torrent.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * ByteBuffer wrapper for usual torrent protocol tasks
 */
public class SmartBuffer {
    private ByteBuffer buffer;
    private boolean isReadState;

    public static SmartBuffer allocate(int capacity) {
        return new SmartBuffer(ByteBuffer.allocate(capacity));
    }

    public SmartBuffer(ByteBuffer buffer) {
        this(buffer, false);
    }

    public SmartBuffer(ByteBuffer buffer, boolean isReadState) {
        this.buffer = buffer;
        this.isReadState = isReadState;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public int writeTo(SocketChannel channel) throws IOException {
        setReadState();
        return channel.write(buffer);
    }

    public void writeSync(SocketChannel channel) throws IOException {
        setReadState();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    public int readFrom(SocketChannel channel) throws IOException {
        setWriteState();
        return channel.read(buffer);
    }

    /**
     * Read from channel into buffer until stream or buffer is over
     * @param channel channel to read from
     * @return true, if input stream is not over, false otherwise
     * @throws IOException
     */
    public boolean readSync(SocketChannel channel) throws IOException {
        setWriteState();
        int numRead = 0;
        while (buffer.hasRemaining() && numRead >= 0) {
            numRead = channel.read(buffer);
        }
        return !buffer.hasRemaining() && numRead >= 0;
    }

    public void putByte(byte value) {
        setWriteState();
        buffer.put(value);
    }

    public Byte getByte() {
        setReadState();
        if (buffer.remaining() < Byte.BYTES) {
            return null;
        }
        return buffer.get();
    }

    public Byte getByteSync(SocketChannel channel) throws IOException {
        Byte result;
        while ((result = getByte()) == null) {
            readFrom(channel);
        }
        return result;
    }

    public void putInt(int value) {
        setWriteState();
        buffer.putInt(value);
    }

    public Integer getInt() {
        setReadState();
        if (buffer.remaining() < Integer.BYTES) {
            return null;
        }
        return buffer.getInt();
    }

    public Integer getIntSync(SocketChannel channel) throws IOException {
        Integer result;
        while ((result = getInt()) == null) {
            readFrom(channel);
        }
        return result;
    }

    public void putShort(short value) {
        setWriteState();
        buffer.putShort(value);
    }

    public Short getShort() {
        setReadState();
        if (buffer.remaining() < Short.BYTES) {
            return null;
        }
        return buffer.getShort();
    }

    public Short getShortSync(SocketChannel channel) throws IOException {
        Short result;
        while ((result = getShort()) == null) {
            readFrom(channel);
        }
        return result;
    }

    public void putBool(boolean value) {
        setWriteState();
        buffer.put(value ? (byte) 1 : 0);
    }

    public Boolean getBool() {
        setReadState();
        if (buffer.remaining() < Byte.BYTES) {
            return null;
        }
        return buffer.get() == 1;
    }

    public Boolean getBoolSync(SocketChannel channel) throws IOException {
        Boolean result;
        while ((result = getBool()) == null) {
            readFrom(channel);
        }
        return result;
    }

    public void putLong(long value) {
        setWriteState();
        buffer.putLong(value);
    }

    public Long getLong() {
        setReadState();
        if (buffer.remaining() < Long.BYTES) {
            return null;
        }
        return buffer.getLong();
    }

    public Long getLongSync(SocketChannel channel) throws IOException {
        Long result;
        while ((result = getLong()) == null) {
            readFrom(channel);
        }
        return result;
    }

    public void putString(String string) {
        setWriteState();
        buffer.putInt(string.length());
        buffer.put(string.getBytes());
    }

    public String getString() {
        setReadState();
        if (buffer.remaining() < Integer.BYTES) {
            return null;
        }
        int len = buffer.getInt();
        if (buffer.remaining() < len) {
            buffer.position(buffer.position() - Integer.BYTES);
            return null;
        }
        byte[] bytes = new byte[len];
        buffer.get(bytes);
        return new String(bytes, Charset.defaultCharset());
    }

    public String getStringSync(SocketChannel channel) throws IOException {
        String result;
        while ((result = getString()) == null) {
            readFrom(channel);
        }
        return result;
    }

    public void setReadState() {
        if (!isReadState) {
            buffer.flip();
            isReadState = true;
        }
    }

    public void setWriteState() {
        if (isReadState) {
            compact();
            buffer.flip();
            buffer.position(buffer.limit());
            buffer.limit(buffer.capacity());
            isReadState = false;
        }
    }

    public void compact() {
        buffer.compact();
    }
}
