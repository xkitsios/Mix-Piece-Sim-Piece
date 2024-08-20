package io.github.xkitsios.util;

import io.github.xkitsios.Point;

import java.util.List;

public class TimeSeries {
    public List<Point> data;
    public double range;
    public int size;

    public TimeSeries(List<Point> data, double range) {
        this.data = data;
        this.range = range;
        this.size = data.size() * (4 + 4);
    }
}
