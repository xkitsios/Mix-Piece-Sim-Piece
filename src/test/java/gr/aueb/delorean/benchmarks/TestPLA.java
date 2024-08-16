package gr.aueb.delorean.benchmarks;

import gr.aueb.delorean.mixpiece.MixPiece;
import gr.aueb.delorean.pmcmr.PMCMR;
import gr.aueb.delorean.simpiece.SimPiece;
import gr.aueb.delorean.swingfilter.SwingFilter;
import gr.aueb.delorean.util.Point;
import gr.aueb.delorean.util.TimeSeries;
import gr.aueb.delorean.util.TimeSeriesReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPLA {
    private Duration duration;

    private long PMCMR(List<Point> ts, double epsilon) throws IOException {
        duration = Duration.ZERO;
        Instant start = Instant.now();
        PMCMR pmcmr = new PMCMR(ts, epsilon);
        duration = Duration.between(start, Instant.now());

        byte[] binary = pmcmr.toByteArray();

        long compressedSize = binary.length;

        pmcmr = new PMCMR(binary);
        List<Point> tsDecompressed = pmcmr.decompress();
        int idx = 0;
        for (Point expected : tsDecompressed) {
            Point actual = ts.get(idx);
            if (expected.getTimestamp() != actual.getTimestamp()) continue;
            idx++;
            assertEquals(actual.getValue(), expected.getValue(), 1.1 * epsilon, "Value did not match for timestamp " + actual.getTimestamp());
        }
        assertEquals(idx, ts.size());

        return compressedSize;
    }


    private long Swing(List<Point> ts, double epsilon) throws IOException {
        duration = Duration.ZERO;
        Instant start = Instant.now();
        SwingFilter swingFilter = new SwingFilter(ts, epsilon);
        duration = Duration.between(start, Instant.now());
        byte[] binary = swingFilter.toByteArray();

        long compressedSize = binary.length;

        swingFilter = new SwingFilter(binary);
        List<Point> tsDecompressed = swingFilter.decompress();
        int idx = 0;
        for (Point expected : tsDecompressed) {
            Point actual = ts.get(idx);
            if (expected.getTimestamp() != actual.getTimestamp()) continue;
            idx++;
            assertEquals(actual.getValue(), expected.getValue(), 1.1 * epsilon, "Value did not match for timestamp " + actual.getTimestamp());
        }
        assertEquals(idx, ts.size());

        return compressedSize;
    }


    private long SimPiece(List<Point> ts, double epsilon, boolean variableByte, boolean zstd) throws IOException {
        duration = Duration.ZERO;
        Instant start = Instant.now();
        SimPiece simPiece = new SimPiece(ts, epsilon);
        duration = Duration.between(start, Instant.now());
        byte[] binary = simPiece.toByteArray(variableByte, zstd);

        long compressedSize = binary.length;

        simPiece = new SimPiece(binary, variableByte, zstd);
        List<Point> tsDecompressed = simPiece.decompress();
        int idx = 0;
        for (Point expected : tsDecompressed) {
            Point actual = ts.get(idx);
            if (expected.getTimestamp() != actual.getTimestamp()) continue;
            idx++;
            assertEquals(actual.getValue(), expected.getValue(), 1.1 * epsilon, "Value did not match for timestamp " + actual.getTimestamp());
        }
        assertEquals(idx, ts.size());

        return compressedSize;
    }

    private long MixPiece(List<Point> ts, double epsilon, boolean variableByte, boolean zstd) throws IOException {
        duration = Duration.ZERO;
        Instant start = Instant.now();
        MixPiece mixPiece = new MixPiece(ts, epsilon);
        duration = Duration.between(start, Instant.now());
        byte[] binary = mixPiece.toByteArray(variableByte, zstd);

        long compressedSize = binary.length;

        mixPiece = new MixPiece(binary, variableByte, zstd);
        List<Point> tsDecompressed = mixPiece.decompress();
        int idx = 0;
        for (Point expected : tsDecompressed) {
            Point actual = ts.get(idx);
            if (expected.getTimestamp() != actual.getTimestamp()) continue;
            idx++;
            assertEquals(actual.getValue(), expected.getValue(), 1.1 * epsilon, "Value did not match for timestamp " + actual.getTimestamp());
        }
        assertEquals(idx, ts.size());

        return compressedSize;
    }


    private void run(String[] filenames, double epsilonStart, double epsilonStep, double epsilonEnd) throws IOException {
        for (String filename : filenames) {
            System.out.println(filename);
            String delimiter = ",";
            TimeSeries ts = TimeSeriesReader.getTimeSeries(getClass().getResourceAsStream(filename), delimiter, true);

            System.out.println("Mix-Piece");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\tExecution Time: %dms\n", epsilonPct * 100, (double) ts.size / MixPiece(ts.data, ts.range * epsilonPct, false, false), duration.toMillis());

            System.out.println("Mix-Piece Variable Byte");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\tExecution Time: %dms\n", epsilonPct * 100, (double) ts.size / MixPiece(ts.data, ts.range * epsilonPct, true, false), duration.toMillis());

            System.out.println("Mix-Piece Variable Byte & ZStd");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\tExecution Time: %dms\n", epsilonPct * 100, (double) ts.size / MixPiece(ts.data, ts.range * epsilonPct, true, true), duration.toMillis());

            System.out.println("Sim-Piece");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\tExecution Time: %dms\n", epsilonPct * 100, (double) ts.size / SimPiece(ts.data, ts.range * epsilonPct, false, false), duration.toMillis());

            System.out.println("Sim-Piece Variable Byte");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\tExecution Time: %dms\n", epsilonPct * 100, (double) ts.size / SimPiece(ts.data, ts.range * epsilonPct, true, false), duration.toMillis());

            System.out.println("Sim-Piece Variable Byte & ZStd");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\tExecution Time: %dms\n", epsilonPct * 100, (double) ts.size / SimPiece(ts.data, ts.range * epsilonPct, true, true), duration.toMillis());

            System.out.println("Swing");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\tExecution Time: %dms\n", epsilonPct * 100, (double) ts.size / Swing(ts.data, ts.range * epsilonPct), duration.toMillis());

            System.out.println("PMCMR");
            for (double epsilonPct = epsilonStart; epsilonPct <= epsilonEnd; epsilonPct += epsilonStep)
                System.out.printf("Epsilon: %.2f%%\tCompression Ratio: %.3f\tExecution Time: %dms\n", epsilonPct * 100, (double) ts.size / PMCMR(ts.data, ts.range * epsilonPct), duration.toMillis());

            System.out.println();
        }
    }


    @Test
    public void TestCRAndTime() throws IOException {
        double epsilonStart = 0.005;
        double epsilonStep = 0.005;
        double epsilonEnd = 0.05;

        String[] filenames = {
                "/Cricket.csv.gz",
                "/FaceFour.csv.gz",
                "/Lightning.csv.gz",
                "/MoteStrain.csv.gz",
                "/Wafer.csv.gz",
                "/WindSpeed.csv.gz",
                "/WindDirection.csv.gz",
        };

        run(filenames, epsilonStart, epsilonStep, epsilonEnd);

        epsilonStart = 0.0005;
        epsilonStep = 0.0005;
        epsilonEnd = 0.0051;

        filenames = new String[]{
                "/Pressure.csv.gz",
                "/BTCUSD.csv.gz",
                "/ETHUSD.csv.gz",
                "/SPX.csv.gz",
                "/STOXX50E.csv.gz"
        };

        run(filenames, epsilonStart, epsilonStep, epsilonEnd);    }
}
