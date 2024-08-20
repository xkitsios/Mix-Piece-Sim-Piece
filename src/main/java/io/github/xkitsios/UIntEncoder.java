package io.github.xkitsios;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

class UIntEncoder {
    protected static void write(long number, ByteArrayOutputStream outputStream) throws IOException {
        if (number > Math.pow(2, 8 * 4) - 1 || number < 0)
            throw new UnsupportedOperationException("Can't save number " + number + " as unsigned int");
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt((int) (number & 0xffffffffL));
        outputStream.write(buffer.array());
    }

    protected static long read(ByteArrayInputStream inputStream) throws IOException {
        byte[] byteArray = new byte[Integer.BYTES];
        int k = inputStream.read(byteArray);
        if (k != Integer.BYTES) throw new IOException();
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.put(byteArray);
        buffer.flip();

        return buffer.getInt() & 0xffffffffL;
    }
}
