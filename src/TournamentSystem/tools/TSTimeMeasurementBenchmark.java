package TournamentSystem.tools;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Stack;

public class TSTimeMeasurementBenchmark {
    private static String datechain = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH.mm.ss"));

    public static void main(String[] args) {
        //one();
        //two();
        //three();
        four();
        //five();
    }

    private static void one() {
        long startTNano = System.nanoTime();
        int limit = 999;
        for (int i=0; i<limit; i++) {
            Object o = new Object();
        }
        long endTNano = System.nanoTime();
        System.out.println("Time in [ns]: "+(endTNano-startTNano));
    }

    private static void two() {
        for (int runs=0; runs<10; runs++) {
            long lastDeltaT = 999999999;
            int limit = 50000;

            for (int i = limit; i > 0; i--) {
                long startTNano = System.nanoTime();

                for (int j = 0; j < i; j++) {
                    //Object o = new Object();
                    new Object();
                    //Math.sqrt(Math.abs(Math.sin(0.5)));
                }

                long endTNano = System.nanoTime();

                long deltaT = endTNano - startTNano;
                /*
                if (deltaT > lastDeltaT) {
                    System.out.println("last deltaT was [ns]: " + lastDeltaT + " / [mikro s]: " + (lastDeltaT / Math.pow(10, 3)) + " @ i:" + i + " diff [ns]: " + (deltaT-lastDeltaT));
                    break;
                }*/
                System.out.println("last deltaT was [ns]: " + lastDeltaT + " / [mikro s]: " + (lastDeltaT / Math.pow(10, 3)) + " @ i:" + i + " diff [ns]: " + (deltaT-lastDeltaT));
                lastDeltaT = deltaT;
            }
        }
    }

    private static void three() {
        int limit = 100;

        for (int i=0; i<limit; i++) {
            long start = System.nanoTime();
            //Object o = new Object();
            new Object();
            long end = System.nanoTime();
            long delta = end - start;
            if (delta>1)
                System.out.println("nanos: "+(end-start)); // daurt ca 485NS
            //System.out.println("nanos: "+(System.nanoTime()-start)); // daurt ca 1500NS
        }
    }

    private static void four() {
        NumberFormat numberFormat00 = new DecimalFormat("#0.00");
        //int startCount = 500;
        //int stepping = 25;
        int[] steps = {
                7500,
                5000,
                2500,
                1000,
                500,
                400,
                300,
                200,
                100,
                50,
                25,
                10,
                5,
                3,
                2
        };
        int runs = 25;

        String seperator = ";";
        String csv = "Messreihe M5"+seperator+"RunsPerStep: "+runs+seperator+"\n";

        //for (int s = startCount; s > 0; s -= stepping) {
        for (int s : steps) {
            Stack<Long> stapel = new Stack<>();
            int numValues = s; // 1000

            for (int i = 0; i < runs; i++) {
                //int[] unsortiert = {8, 7, 6, 5, 4, 3, 2, 1, 0};
                int[] unsortiert = new int[numValues];
                for (int j = 0; j < numValues; j++)
                    unsortiert[j] = numValues - j;

                long start = System.nanoTime();
                int[] sortiert = insertionSort(unsortiert);
                long end = System.nanoTime();

                //long delta = end - start;
                //System.out.println("nanos: " + (end - start));
                stapel.push(end - start);

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // println statistics
            System.out.println("[ns] Min: " + Collections.min(stapel) + " | Max: " + Collections.max(stapel));
            double[] werte = new double[stapel.size()];
            for (int i = 0; i < werte.length; i++)
                werte[i] = stapel.pop();
            Mean avg = new Mean();
            Median mdn = new Median();
            System.out.println("[ns] Average : " + avg.evaluate(werte) + " | Median: " + mdn.evaluate(werte));
            System.out.println("[mys] Average: " + nanoToMikroS(avg.evaluate(werte)) + " | Median: " + nanoToMikroS(mdn.evaluate(werte)));
            System.out.println("[ms] Average : " + nanoToMilliS(avg.evaluate(werte)) + " | Median: " + nanoToMilliS(mdn.evaluate(werte)));
            //for (double d : werte) System.out.println(d);

            // csv output
            System.out.println("###### CSV OUTPUT ######");
            String out = "";
            for (double d : werte) {
                out += "V" + numValues + seperator + (""+numberFormat00.format(d)).replace('.', ',') + seperator + "\n";
            }
            csv += out;
            System.out.println(out);
        } // ende for

        String filename = "BoxPlot-CSV_Data-M5-" + datechain;
        File file = new File("C:\\Users\\Felix\\Desktop\\" + filename + ".csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(csv);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void five() {
        int[] steps = {
                10000000,
                1000000,
                100000,
                10000,
                1000,
                1000,
                750,
                500,
                250,
                100,
                50,
                25,
                10,
                5,
                3,
                2
        };
        int runs = 10;
        long millisSleep = 250;

        String seperator = ";";
        String csv = "Messreihe M5"+seperator+"RunsPerStep: "+runs+" MillisSleep: "+millisSleep+seperator+"\n";

        //for (int s = startCount; s > 0; s -= stepping) {
        for (int stepCount : steps) {
            Stack<Long> stapel = new Stack<>();

            for (int i = 0; i < runs; i++) {
                long start = System.nanoTime();

                for (int j=0; j<stepCount; j++){
                    Object o = new Object();
                    o = null;
                }

                long end = System.nanoTime();
                //System.out.println("nanos: " + (end - start));
                stapel.push(end - start);

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // println statistics
            System.out.println("[ns] Min: " + Collections.min(stapel) + " | Max: " + Collections.max(stapel));
            double[] werte = new double[stapel.size()];
            for (int i = 0; i < werte.length; i++)
                werte[i] = stapel.pop();
            Mean avg = new Mean();
            Median mdn = new Median();
            System.out.println("[ns] Average : " + avg.evaluate(werte) + " | Median: " + mdn.evaluate(werte));
            System.out.println("[mys] Average: " + nanoToMikroS(avg.evaluate(werte)) + " | Median: " + nanoToMikroS(mdn.evaluate(werte)));
            System.out.println("[ms] Average : " + nanoToMilliS(avg.evaluate(werte)) + " | Median: " + nanoToMilliS(mdn.evaluate(werte)));
            //for (double d : werte) System.out.println(d);

            // csv output
            System.out.println("###### CSV OUTPUT ######");
            String out = "";
            for (double d : werte) {
                out += "V" + stepCount + seperator + (""+d).replace('.', ',') + seperator + "\n";
            }
            csv += out;
            System.out.println(out);
        } // ende for

        String filename = "BoxPlot-CSV_Data-M5-" + datechain;
        File file = new File("C:\\Users\\Felix\\Desktop\\" + filename + ".csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(csv);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static int[] insertionSort(int[] sortieren) {
        int temp;
        for (int i = 1; i < sortieren.length; i++) {
            temp = sortieren[i];
            int j = i;
            while (j > 0 && sortieren[j - 1] > temp) {
                sortieren[j] = sortieren[j - 1];
                j--;
            }
            sortieren[j] = temp;
        }
        return sortieren;
    }
    private static double nanoToMilliS(double timeNS) {
        return timeNS/Math.pow(10,6);
    }
    private static double nanoToMikroS(double timeNS) {
        return timeNS/Math.pow(10,3);
    }
}
