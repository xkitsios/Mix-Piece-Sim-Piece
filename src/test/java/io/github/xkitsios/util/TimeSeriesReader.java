package io.github.xkitsios.util;

import io.github.xkitsios.Point;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class TimeSeriesReader {
    public static TimeSeries getTimeSeries(InputStream inputStream, String delimiter, boolean gzip) {
        ArrayList<Point> ts = new ArrayList<>();
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;

        try {
            if (gzip) inputStream = new GZIPInputStream(inputStream);
            Reader decoder = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(decoder);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] elements = line.split(delimiter);
                long timestamp = Long.parseLong(elements[0]);
                double value = Double.parseDouble(elements[1]);
                ts.add(new Point(timestamp, value));

                max = Math.max(max, value);
                min = Math.min(min, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new TimeSeries(ts, max - min);
    }
}
