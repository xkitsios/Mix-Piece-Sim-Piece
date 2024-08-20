package io.github.xkitsios;

/**
 * A struct representing a time-series point
 */
public class Point {
    private final long timestamp;
    private final double value;

    /**
     * Constructor for Point
     * @param timestamp Timestamp
     * @param value Value
     */
    public Point(long timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    /**
     * Getter for timestamp
     * @return Timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Getter for value
     * @return Value
     */
    public double getValue() {
        return value;
    }
}
