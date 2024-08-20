package io.github.xkitsios;

import com.github.luben.zstd.Zstd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Sim-Piece Algorithm for Compressing Time-Series Data
 */
public class SimPiece {
    private static ArrayList<SimPieceSegment> segments;

    private static double epsilon;
    private static long lastTimeStamp;

    /**
     * Compress a list of Point and return a binary representation
     * @param points Time-series data
     * @param error Maximum absolute error
     * @return Binary representation
     * @throws Exception
     */
    public static byte[] compress(List<Point> points, double error) throws Exception {
        if (points.isEmpty() || error <= 0) throw new Exception();

        epsilon = error;
        lastTimeStamp = points.get(points.size() - 1).getTimestamp();
        segments = mergePerB(compress(points));
        return toByteArray();
    }

    /**
     * Decompress a binary representation and return a list of Points
     * @param binary Binary representation
     * @return Time-series data
     * @throws IOException
     */
    public static List<Point> decompress(byte[] binary) throws IOException {
        readByteArray(binary);
        return toPoints();
    }

    private static double quantization(double value) {
        return Math.round(value / epsilon) * epsilon;
    }

    private static int createSegment(int startIdx, List<Point> points, ArrayList<SimPieceSegment> segments) {
        long initTimestamp = points.get(startIdx).getTimestamp();
        double b = quantization(points.get(startIdx).getValue());
        if (startIdx + 1 == points.size()) {
            segments.add(new SimPieceSegment(initTimestamp, -Double.MAX_VALUE, Double.MAX_VALUE, b));
            return startIdx + 1;
        }
        double aMax = ((points.get(startIdx + 1).getValue() + epsilon) - b) / (points.get(startIdx + 1).getTimestamp() - initTimestamp);
        double aMin = ((points.get(startIdx + 1).getValue() - epsilon) - b) / (points.get(startIdx + 1).getTimestamp() - initTimestamp);
        if (startIdx + 2 == points.size()) {
            segments.add(new SimPieceSegment(initTimestamp, aMin, aMax, b));
            return startIdx + 2;
        }

        for (int idx = startIdx + 2; idx < points.size(); idx++) {
            double upValue = points.get(idx).getValue() + epsilon;
            double downValue = points.get(idx).getValue() - epsilon;

            double upLim = aMax * (points.get(idx).getTimestamp() - initTimestamp) + b;
            double downLim = aMin * (points.get(idx).getTimestamp() - initTimestamp) + b;
            if ((downValue > upLim || upValue < downLim)) {
                segments.add(new SimPieceSegment(initTimestamp, aMin, aMax, b));
                return idx;
            }

            if (upValue < upLim)
                aMax = Math.max((upValue - b) / (points.get(idx).getTimestamp() - initTimestamp), aMin);
            if (downValue > downLim)
                aMin = Math.min((downValue - b) / (points.get(idx).getTimestamp() - initTimestamp), aMax);
        }
        segments.add(new SimPieceSegment(initTimestamp, aMin, aMax, b));

        return points.size();
    }

    private static ArrayList<SimPieceSegment> compress(List<Point> points) {
        ArrayList<SimPieceSegment> segments = new ArrayList<>();
        int currentIdx = 0;
        while (currentIdx < points.size()) currentIdx = createSegment(currentIdx, points, segments);

        return segments;
    }

    private static ArrayList<SimPieceSegment> mergePerB(ArrayList<SimPieceSegment> segments) {
        double aMinTemp = -Double.MAX_VALUE;
        double aMaxTemp = Double.MAX_VALUE;
        double b = Double.NaN;
        ArrayList<Long> timestamps = new ArrayList<>();
        ArrayList<SimPieceSegment> mergedSegments = new ArrayList<>();

        segments.sort(Comparator.comparingDouble(SimPieceSegment::getB).thenComparingDouble(SimPieceSegment::getA));
        for (int i = 0; i < segments.size(); i++) {
            if (b != segments.get(i).getB()) {
                if (timestamps.size() == 1)
                    mergedSegments.add(new SimPieceSegment(timestamps.get(0), aMinTemp, aMaxTemp, b));
                else {
                    for (Long timestamp : timestamps)
                        mergedSegments.add(new SimPieceSegment(timestamp, aMinTemp, aMaxTemp, b));
                }
                timestamps.clear();
                timestamps.add(segments.get(i).getInitTimestamp());
                aMinTemp = segments.get(i).getAMin();
                aMaxTemp = segments.get(i).getAMax();
                b = segments.get(i).getB();
                continue;
            }
            if (segments.get(i).getAMin() <= aMaxTemp && segments.get(i).getAMax() >= aMinTemp) {
                timestamps.add(segments.get(i).getInitTimestamp());
                aMinTemp = Math.max(aMinTemp, segments.get(i).getAMin());
                aMaxTemp = Math.min(aMaxTemp, segments.get(i).getAMax());
            } else {
                if (timestamps.size() == 1) mergedSegments.add(segments.get(i - 1));
                else {
                    for (long timestamp : timestamps)
                        mergedSegments.add(new SimPieceSegment(timestamp, aMinTemp, aMaxTemp, b));
                }
                timestamps.clear();
                timestamps.add(segments.get(i).getInitTimestamp());
                aMinTemp = segments.get(i).getAMin();
                aMaxTemp = segments.get(i).getAMax();
            }
        }
        if (!timestamps.isEmpty()) {
            if (timestamps.size() == 1)
                mergedSegments.add(new SimPieceSegment(timestamps.get(0), aMinTemp, aMaxTemp, b));
            else {
                for (long timestamp : timestamps)
                    mergedSegments.add(new SimPieceSegment(timestamp, aMinTemp, aMaxTemp, b));
            }
        }

        return mergedSegments;
    }

    private static List<Point> toPoints() {
        segments.sort(Comparator.comparingLong(SimPieceSegment::getInitTimestamp));
        List<Point> points = new ArrayList<>();
        long currentTimeStamp = segments.get(0).getInitTimestamp();


        for (int i = 0; i < segments.size() - 1; i++) {
            while (currentTimeStamp < segments.get(i + 1).getInitTimestamp()) {
                points.add(new Point(currentTimeStamp, segments.get(i).getA() * (currentTimeStamp - segments.get(i).getInitTimestamp()) + segments.get(i).getB()));
                currentTimeStamp++;
            }
        }

        while (currentTimeStamp <= lastTimeStamp) {
            points.add(new Point(currentTimeStamp, segments.get(segments.size() - 1).getA() * (currentTimeStamp - segments.get(segments.size() - 1).getInitTimestamp()) + segments.get(segments.size() - 1).getB()));
            currentTimeStamp++;
        }

        return points;
    }

    private static void toByteArrayPerBSegments(ArrayList<SimPieceSegment> segments, ByteArrayOutputStream outStream) throws IOException {
        TreeMap<Integer, HashMap<Double, ArrayList<Long>>> input = new TreeMap<>();
        for (SimPieceSegment segment : segments) {
            double a = segment.getA();
            int b = (int) Math.round(segment.getB() / epsilon);
            long t = segment.getInitTimestamp();
            if (!input.containsKey(b)) input.put(b, new HashMap<>());
            if (!input.get(b).containsKey(a)) input.get(b).put(a, new ArrayList<>());
            input.get(b).get(a).add(t);
        }

        VariableByteEncoder.write(input.size(), outStream);
        if (input.isEmpty()) return;
        int previousB = input.firstKey();
        VariableByteEncoder.write(previousB, outStream);
        for (Map.Entry<Integer, HashMap<Double, ArrayList<Long>>> bSegments : input.entrySet()) {
            VariableByteEncoder.write(bSegments.getKey() - previousB, outStream);
            previousB = bSegments.getKey();
            VariableByteEncoder.write(bSegments.getValue().size(), outStream);
            for (Map.Entry<Double, ArrayList<Long>> aSegment : bSegments.getValue().entrySet()) {
                FloatEncoder.write(aSegment.getKey().floatValue(), outStream);
                Collections.sort(aSegment.getValue());
                VariableByteEncoder.write(aSegment.getValue().size(), outStream);
                long previousTS = 0;
                for (Long timestamp : aSegment.getValue()) {
                    VariableByteEncoder.write((int) (timestamp - previousTS), outStream);
                    previousTS = timestamp;
                }
            }
        }
    }


    private static byte[] toByteArray() throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] bytes;

        FloatEncoder.write((float) epsilon, outStream);
        toByteArrayPerBSegments(segments, outStream);
        VariableByteEncoder.write((int) lastTimeStamp, outStream);
        bytes = Zstd.compress(outStream.toByteArray());

        outStream.close();

        return bytes;
    }

    private static ArrayList<SimPieceSegment> readMergedPerBSegments(ByteArrayInputStream inStream) throws IOException {
        ArrayList<SimPieceSegment> segments = new ArrayList<>();
        long numB = VariableByteEncoder.read(inStream);
        if (numB == 0) return segments;
        int previousB = VariableByteEncoder.read(inStream);
        for (int i = 0; i < numB; i++) {
            int b = VariableByteEncoder.read(inStream) + previousB;
            previousB = b;
            int numA = VariableByteEncoder.read(inStream);
            for (int j = 0; j < numA; j++) {
                float a = FloatEncoder.read(inStream);
                int numTimestamps = VariableByteEncoder.read(inStream);
                long timestamp = 0;
                for (int k = 0; k < numTimestamps; k++) {
                    timestamp += VariableByteEncoder.read(inStream);
                    segments.add(new SimPieceSegment(timestamp, a, (float) (b * epsilon)));
                }
            }
        }

        return segments;
    }

    private static void readByteArray(byte[] input) throws IOException {
        byte[] binary = Zstd.decompress(input, input.length * 2); //TODO: How to know apriori original size?
        ByteArrayInputStream inStream = new ByteArrayInputStream(binary);

        epsilon = FloatEncoder.read(inStream);
        segments = readMergedPerBSegments(inStream);
        lastTimeStamp = VariableByteEncoder.read(inStream);
        inStream.close();
    }
}
