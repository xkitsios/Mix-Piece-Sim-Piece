package io.github.xkitsios;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class FloatEncoder {
    protected static void write(float number, ByteArrayOutputStream outputStream) throws IOException {
        int intBits = Float.floatToIntBits(number);
        IntEncoder.write(intBits, outputStream);
    }

    protected static float read(ByteArrayInputStream inputStream) throws IOException {
        int number = IntEncoder.read(inputStream);
        return Float.intBitsToFloat(number);
    }
}
