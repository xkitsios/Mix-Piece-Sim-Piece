package gr.aueb.delorean.mixpiece;

import com.github.luben.zstd.Zstd;
import gr.aueb.delorean.util.Encoding.FloatEncoder;
import gr.aueb.delorean.util.Encoding.UIntEncoder;
import gr.aueb.delorean.util.Encoding.VariableByteEncoder;
import gr.aueb.delorean.util.Point;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class MixPiece {
    private ArrayList<MixPieceSegment> perBSegments;
    private ArrayList<MixPieceSegment> perASegments;
    private ArrayList<MixPieceSegment> restSegments;

    private double epsilon;
    private int globalMinB;
    private long lastTimeStamp;

    public MixPiece(List<Point> points, double epsilon) throws IOException {
        if (points.isEmpty()) throw new IOException();

        this.epsilon = epsilon;
        this.lastTimeStamp = points.get(points.size() - 1).getTimestamp();
        merge(compress(points));
    }

    public MixPiece(byte[] bytes, boolean variableByte, boolean zstd) {
        readByteArray(bytes, variableByte, zstd);
    }

    private double quantization(double value, int mode) {
        if (mode == 1) return (int) Math.ceil(value / epsilon) * epsilon;
        else if (mode == 2) return (int) Math.floor(value / epsilon) * epsilon;
        else return Math.round(value / epsilon) * epsilon;
    }

    private int createSegment(int startIdx, List<Point> points, ArrayList<MixPieceSegment> segments, int quantizationMode) {
        long initTimestamp = points.get(startIdx).getTimestamp();
        double b = quantization(points.get(startIdx).getValue(), quantizationMode);
        if (startIdx + 1 == points.size()) {
            segments.add(new MixPieceSegment(initTimestamp, -Double.MAX_VALUE, Double.MAX_VALUE, b));
            return startIdx + 1;
        }
        double aMax = ((points.get(startIdx + 1).getValue() + epsilon) - b) / (points.get(startIdx + 1).getTimestamp() - initTimestamp);
        double aMin = ((points.get(startIdx + 1).getValue() - epsilon) - b) / (points.get(startIdx + 1).getTimestamp() - initTimestamp);
        if (startIdx + 2 == points.size()) {
            segments.add(new MixPieceSegment(initTimestamp, aMin, aMax, b));
            return startIdx + 2;
        }

        for (int idx = startIdx + 2; idx < points.size(); idx++) {
            double upValue = points.get(idx).getValue() + epsilon;
            double downValue = points.get(idx).getValue() - epsilon;

            double upLim = aMax * (points.get(idx).getTimestamp() - initTimestamp) + b;
            double downLim = aMin * (points.get(idx).getTimestamp() - initTimestamp) + b;
            if ((downValue > upLim || upValue < downLim)) {
                segments.add(new MixPieceSegment(initTimestamp, aMin, aMax, b));
                return idx;
            }

            if (upValue < upLim)
                aMax = Math.max((upValue - b) / (points.get(idx).getTimestamp() - initTimestamp), aMin);
            if (downValue > downLim)
                aMin = Math.min((downValue - b) / (points.get(idx).getTimestamp() - initTimestamp), aMax);
        }
        segments.add(new MixPieceSegment(initTimestamp, aMin, aMax, b));

        return points.size();
    }

    private ArrayList<MixPieceSegment> compress(List<Point> points) {
        ArrayList<MixPieceSegment> segments = new ArrayList<>();
        int currentIdx = 0;
        while (currentIdx < points.size()) {
            int currentCeilIdx = createSegment(currentIdx, points, segments, 1);
            int currentFloorIdx = createSegment(currentIdx, points, segments, 2);
            if (currentCeilIdx > currentFloorIdx) {
                segments.remove(segments.size() - 1);
                currentIdx = currentCeilIdx;
            } else if (currentCeilIdx < currentFloorIdx) {
                segments.remove(segments.size() - 2);
                currentIdx = currentFloorIdx;
            } else {
                double firstValue = points.get(currentIdx).getValue();
                if (Math.round(firstValue / epsilon) == Math.ceil(firstValue / epsilon))
                    segments.remove(segments.size() - 1);
                else segments.remove(segments.size() - 2);
                currentIdx = currentFloorIdx;
            }
            globalMinB = (int) Math.min(globalMinB, segments.get(segments.size() - 1).getB() / epsilon);
        }

        return segments;
    }

    private void mergePerB(ArrayList<MixPieceSegment> segments, ArrayList<MixPieceSegment> mergedSegments, ArrayList<MixPieceSegment> unmergedSegments) {
        double aMinTemp = -Double.MAX_VALUE;
        double aMaxTemp = Double.MAX_VALUE;
        double b = Double.NaN;
        ArrayList<Long> timestamps = new ArrayList<>();

        segments.sort(Comparator.comparingDouble(MixPieceSegment::getB).thenComparingDouble(MixPieceSegment::getA));
        for (int i = 0; i < segments.size(); i++) {
            if (b != segments.get(i).getB()) {
                if (timestamps.size() == 1)
                    unmergedSegments.add(new MixPieceSegment(timestamps.get(0), aMinTemp, aMaxTemp, b));
                else {
                    for (Long timestamp : timestamps)
                        mergedSegments.add(new MixPieceSegment(timestamp, aMinTemp, aMaxTemp, b));
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
                if (timestamps.size() == 1) unmergedSegments.add(segments.get(i - 1));
                else {
                    for (long timestamp : timestamps)
                        mergedSegments.add(new MixPieceSegment(timestamp, aMinTemp, aMaxTemp, b));
                }
                timestamps.clear();
                timestamps.add(segments.get(i).getInitTimestamp());
                aMinTemp = segments.get(i).getAMin();
                aMaxTemp = segments.get(i).getAMax();
            }
        }
        if (!timestamps.isEmpty()) {
            if (timestamps.size() == 1)
                unmergedSegments.add(new MixPieceSegment(timestamps.get(0), aMinTemp, aMaxTemp, b));
            else {
                for (long timestamp : timestamps)
                    mergedSegments.add(new MixPieceSegment(timestamp, aMinTemp, aMaxTemp, b));
            }
        }
    }

    private void mergeAll(ArrayList<MixPieceSegment> segments, ArrayList<MixPieceSegment> mergedSegments, ArrayList<MixPieceSegment> unmergedSegments) {
        double aMinTemp = -Double.MAX_VALUE;
        double aMaxTemp = Double.MAX_VALUE;
        ArrayList<Double> bValues = new ArrayList<>();
        ArrayList<Long> timestamps = new ArrayList<>();

        segments.sort(Comparator.comparingDouble(MixPieceSegment::getAMin));
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).getAMin() <= aMaxTemp && segments.get(i).getAMax() >= aMinTemp) {
                timestamps.add(segments.get(i).getInitTimestamp());
                aMinTemp = Math.max(aMinTemp, segments.get(i).getAMin());
                aMaxTemp = Math.min(aMaxTemp, segments.get(i).getAMax());
                bValues.add(segments.get(i).getB());
            } else {
                if (timestamps.size() == 1) unmergedSegments.add(segments.get(i - 1));
                else {
                    for (int j = 0; j < timestamps.size(); j++)
                        mergedSegments.add(new MixPieceSegment(timestamps.get(j), aMinTemp, aMaxTemp, bValues.get(j)));
                }
                timestamps.clear();
                timestamps.add(segments.get(i).getInitTimestamp());
                aMinTemp = segments.get(i).getAMin();
                aMaxTemp = segments.get(i).getAMax();
                bValues.clear();
                bValues.add(segments.get(i).getB());
            }
        }
        if (!timestamps.isEmpty()) {
            if (timestamps.size() == 1)
                unmergedSegments.add(new MixPieceSegment(timestamps.get(0), aMinTemp, aMaxTemp, bValues.get(0)));
            else {
                for (int i = 0; i < timestamps.size(); i++)
                    mergedSegments.add(new MixPieceSegment(timestamps.get(i), aMinTemp, aMaxTemp, bValues.get(i)));
            }
        }
    }

    private void merge(ArrayList<MixPieceSegment> segments) {
        perBSegments = new ArrayList<>();
        perASegments = new ArrayList<>();
        restSegments = new ArrayList<>();
        ArrayList<MixPieceSegment> temp = new ArrayList<>();

        mergePerB(segments, perBSegments, temp);
        if (!temp.isEmpty()) {
            mergeAll(temp, perASegments, restSegments);
        }
    }

    public List<Point> decompress() {
        ArrayList<MixPieceSegment> segments = new ArrayList<>();
        segments.addAll(perBSegments);
        segments.addAll(perASegments);
        segments.addAll(restSegments);
        segments.sort(Comparator.comparingLong(MixPieceSegment::getInitTimestamp));
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

    private void toByteArrayPerBSegments(ArrayList<MixPieceSegment> segments, boolean variableByte, ByteArrayOutputStream outStream) throws IOException {
        TreeMap<Integer, HashMap<Double, ArrayList<Long>>> input = new TreeMap<>();
        for (MixPieceSegment segment : segments) {
            double a = segment.getA();
            int b = (int) Math.round(segment.getB() / epsilon);
            long t = segment.getInitTimestamp();
            if (!input.containsKey(b)) input.put(b, new HashMap<>());
            if (!input.get(b).containsKey(a)) input.get(b).put(a, new ArrayList<>());
            input.get(b).get(a).add(t);
        }

        VariableByteEncoder.write(input.size(), outStream);
        if (input.isEmpty())
            return;
        int previousB = input.firstKey() - globalMinB;
        VariableByteEncoder.write(previousB, outStream);
        for (Map.Entry<Integer, HashMap<Double, ArrayList<Long>>> bSegments : input.entrySet()) {
            VariableByteEncoder.write(bSegments.getKey() - globalMinB - previousB, outStream);
            previousB = bSegments.getKey() - globalMinB;
            VariableByteEncoder.write(bSegments.getValue().size(), outStream);
            for (Map.Entry<Double, ArrayList<Long>> aSegment : bSegments.getValue().entrySet()) {
                FloatEncoder.write(aSegment.getKey().floatValue(), outStream);
                if (variableByte) Collections.sort(aSegment.getValue());
                VariableByteEncoder.write(aSegment.getValue().size(), outStream);
                long previousTS = 0;
                for (Long timestamp : aSegment.getValue()) {
                    if (variableByte) VariableByteEncoder.write((int) (timestamp - previousTS), outStream);
                    else UIntEncoder.write(timestamp, outStream);
                    previousTS = timestamp;
                }
            }
        }
    }

    private void toByteArrayPerASegments(ArrayList<MixPieceSegment> segments, ByteArrayOutputStream outStream) throws IOException {
        TreeMap<Double, ArrayList<MixPieceSegment>> input = new TreeMap<>();
        for (MixPieceSegment segment : segments) {
            if (!input.containsKey(segment.getA())) input.put(segment.getA(), new ArrayList<>());
            input.get(segment.getA()).add(segment);
        }

        VariableByteEncoder.write(input.size(), outStream);
        for (Map.Entry<Double, ArrayList<MixPieceSegment>> aSegments : input.entrySet()) {
            FloatEncoder.write(aSegments.getKey().floatValue(), outStream);
            VariableByteEncoder.write(aSegments.getValue().size(), outStream);
            aSegments.getValue().sort(Comparator.comparingDouble(MixPieceSegment::getB));
            int previousB = (int) Math.round(aSegments.getValue().get(0).getB() / epsilon) - globalMinB;
            VariableByteEncoder.write(previousB, outStream);
            for (MixPieceSegment segment : aSegments.getValue()) {
                VariableByteEncoder.write((int) (Math.round(segment.getB() / epsilon) - globalMinB - previousB), outStream);
                previousB = (int) Math.round(segment.getB() / epsilon) - globalMinB;
                UIntEncoder.write(segment.getInitTimestamp(), outStream);
            }
        }
    }

    private void toByteArrayRestSegments(ArrayList<MixPieceSegment> segments, ByteArrayOutputStream outStream) throws IOException {
        VariableByteEncoder.write(segments.size(), outStream);
        if (segments.isEmpty())
            return;
        segments.sort(Comparator.comparingDouble(MixPieceSegment::getB));
        int previousB = (int) Math.round(segments.get(0).getB() / epsilon) - globalMinB;
        VariableByteEncoder.write(previousB, outStream);
        for (MixPieceSegment segment : segments) {
            VariableByteEncoder.write((int) (Math.round(segment.getB() / epsilon) - globalMinB - previousB), outStream);
            previousB = (int) Math.round(segment.getB() / epsilon) - globalMinB;
            FloatEncoder.write((float) segment.getA(), outStream);
            UIntEncoder.write(segment.getInitTimestamp(), outStream);
        }
    }

    public byte[] toByteArray(boolean variableByte, boolean zstd) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] bytes = null;

        try {
            FloatEncoder.write((float) epsilon, outStream);
            VariableByteEncoder.write(globalMinB, outStream);

            toByteArrayPerBSegments(perBSegments, variableByte, outStream);
            toByteArrayPerASegments(perASegments, outStream);
            toByteArrayRestSegments(restSegments, outStream);

            if (variableByte) VariableByteEncoder.write((int) lastTimeStamp, outStream);
            else UIntEncoder.write(lastTimeStamp, outStream);

            if (zstd) bytes = Zstd.compress(outStream.toByteArray());
            else bytes = outStream.toByteArray();

            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bytes;
    }

    private ArrayList<MixPieceSegment> readMergedPerBSegments(boolean variableByte, ByteArrayInputStream inStream) throws IOException {
        ArrayList<MixPieceSegment> segments = new ArrayList<>();
        long numB = VariableByteEncoder.read(inStream);
        if (numB == 0)
            return segments;
        int previousB = VariableByteEncoder.read(inStream);
        for (int i = 0; i < numB; i++) {
            int b = VariableByteEncoder.read(inStream) + globalMinB + previousB;
            previousB = b - globalMinB;
            int numA = VariableByteEncoder.read(inStream);
            for (int j = 0; j < numA; j++) {
                float a = FloatEncoder.read(inStream);
                int numTimestamps = VariableByteEncoder.read(inStream);
                long timestamp = 0;
                for (int k = 0; k < numTimestamps; k++) {
                    if (variableByte) timestamp += VariableByteEncoder.read(inStream);
                    else timestamp = UIntEncoder.read(inStream);
                    segments.add(new MixPieceSegment(timestamp, a, (float) (b * epsilon)));
                }
            }
        }

        return segments;
    }

    private ArrayList<MixPieceSegment> readMergedPerASegments(ByteArrayInputStream inStream) throws IOException {
        ArrayList<MixPieceSegment> segments = new ArrayList<>();
        int numA = VariableByteEncoder.read(inStream);
        for (int i = 0; i < numA; i++) {
            float a = FloatEncoder.read(inStream);
            int numBT = VariableByteEncoder.read(inStream);
            int previousB = VariableByteEncoder.read(inStream);
            for (int j = 0; j < numBT; j++) {
                int b = VariableByteEncoder.read(inStream) + globalMinB + previousB;
                previousB = b - globalMinB;
                long timestamp = UIntEncoder.read(inStream);
                segments.add(new MixPieceSegment(timestamp, a, (float) (b * epsilon)));
            }
        }

        return segments;
    }

    private ArrayList<MixPieceSegment> readUnmerged(ByteArrayInputStream inStream) throws IOException {
        ArrayList<MixPieceSegment> segments = new ArrayList<>();
        int num = VariableByteEncoder.read(inStream);
        if (num == 0)
            return segments;
        int previousB = VariableByteEncoder.read(inStream);
        for (int i = 0; i < num; i++) {
            int b = VariableByteEncoder.read(inStream) + globalMinB + previousB;
            previousB = b - globalMinB;
            float a = FloatEncoder.read(inStream);
            long timestamp = UIntEncoder.read(inStream);
            segments.add(new MixPieceSegment(timestamp, a, (float) (b * epsilon)));
        }

        return segments;
    }

    private void readByteArray(byte[] input, boolean variableByte, boolean zstd) {
        byte[] binary;
        if (zstd) binary = Zstd.decompress(input, input.length * 2); //TODO: How to know apriori original size?
        else binary = input;
        ByteArrayInputStream inStream = new ByteArrayInputStream(binary);

        try {
            this.epsilon = FloatEncoder.read(inStream);
            this.globalMinB = VariableByteEncoder.read(inStream);
            this.perBSegments = readMergedPerBSegments(variableByte, inStream);
            this.perASegments = readMergedPerASegments(inStream);
            this.restSegments = readUnmerged(inStream);
            if (variableByte) this.lastTimeStamp = VariableByteEncoder.read(inStream);
            else this.lastTimeStamp = UIntEncoder.read(inStream);
            inStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}