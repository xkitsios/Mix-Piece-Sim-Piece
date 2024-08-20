package io.github.xkitsios;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/*
 * Source code by:
 * https://github.com/lemire/JavaFastPFOR/blob/master/src/main/java/me/lemire/integercompression/VariableByte.java
 */
class VariableByteEncoder {

    private static byte extract7bits(int i, long val) {
        return (byte) ((val >> (7 * i)) & ((1 << 7) - 1));
    }

    private static byte extract7bitsmaskless(int i, long val) {
        return (byte) ((val >> (7 * i)));
    }

    protected static void write(int number, ByteArrayOutputStream outputStream) {
        final long val = number & 0xFFFFFFFFL;

        if (val < (1 << 7)) {
            outputStream.write((byte) (val | (1 << 7)));
        } else if (val < (1 << 14)) {
            outputStream.write(extract7bits(0, val));
            outputStream.write((byte) (extract7bitsmaskless(1, (val)) | (1 << 7)));
        } else if (val < (1 << 21)) {
            outputStream.write(extract7bits(0, val));
            outputStream.write(extract7bits(1, val));
            outputStream.write((byte) (extract7bitsmaskless(2, (val)) | (1 << 7)));
        } else if (val < (1 << 28)) {
            outputStream.write(extract7bits(0, val));
            outputStream.write(extract7bits(1, val));
            outputStream.write(extract7bits(2, val));
            outputStream.write((byte) (extract7bitsmaskless(3, (val)) | (1 << 7)));
        } else {
            outputStream.write(extract7bits(0, val));
            outputStream.write(extract7bits(1, val));
            outputStream.write(extract7bits(2, val));
            outputStream.write(extract7bits(3, val));
            outputStream.write((byte) (extract7bitsmaskless(4, (val)) | (1 << 7)));
        }
    }

    protected static int read(ByteArrayInputStream inputStream) {
        byte in;
        int number;

        in = (byte) inputStream.read();
        number = in & 0x7F;
        if (in < 0) return number;

        in = (byte) inputStream.read();
        number = ((in & 0x7F) << 7) | number;
        if (in < 0) return number;

        in = (byte) inputStream.read();
        number = ((in & 0x7F) << 14) | number;
        if (in < 0) return number;

        in = (byte) inputStream.read();
        number = ((in & 0x7F) << 21) | number;
        if (in < 0) return number;

        number = (((byte) inputStream.read() & 0x7F) << 28) | number;

        return number;
    }
}
