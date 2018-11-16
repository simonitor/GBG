package TournamentSystem.tools;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
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
                /*
                50000, // ca 400ms
                10000, // ca 16ms
                7500,  // ca 9ms
                5000,  // ca 4ms
                4000,  // ca 2,5ms
                */
                3000,  // ca 1,5ms
                2000,  // ca 0.6ms
                1000,  // ca 0.2ms
                750,
                500,
                400,
                300,
                200,
                100/*,
                50,
                25,
                10,
                5,
                3,
                2*/
        };
        int runs = 75;
        long millisSleep = 250;
        String[][] data = new String[steps.length][runs+1];

        String seperator = ";";
        String csv = "Messreihe M5"+"RunsPerStep: "+runs+" MillisSleep: "+millisSleep+seperator+"NanoSekunden"+seperator+"MikroSekunden"+seperator+"\n";
        //String tmpRes = "\nSummary [musec]:"+seperator+"\n"+"Reihe"+seperator+"AVG"+seperator+"SD"+seperator+"ratio"+seperator+"\n";

        //for (int s = startCount; s > 0; s -= stepping) {
        for (int x=0; x<steps.length; x++) {
            Stack<Long> stapel = new Stack<>();
            int numValues = steps[x];
            int[] unsortiert = new int[numValues];
            int[] sortiert;

            for (int i = 0; i < runs; i++) {
                //int[] unsortiert = {8, 7, 6, 5, 4, 3, 2, 1, 0};
                //int[] unsortiert = new int[numValues];
                for (int j = 0; j < numValues; j++)
                    unsortiert[j] = numValues - j;

                long start = System.nanoTime();
                sortiert = insertionSort(unsortiert);
                long end = System.nanoTime();

                //long delta = end - start;
                //System.out.println("nanos: " + (end - start));
                stapel.push(end - start);

                try {
                    Thread.sleep(millisSleep);
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
            StandardDeviation sd = new StandardDeviation();
            System.out.println("[ns] Average : " + avg.evaluate(werte) + " | Median: " + mdn.evaluate(werte));
            System.out.println("[mys] Average: " + nanoToMikroS(avg.evaluate(werte)) + " | Median: " + nanoToMikroS(mdn.evaluate(werte)));
            System.out.println("[ms] Average : " + nanoToMilliS(avg.evaluate(werte)) + " | Median: " + nanoToMilliS(mdn.evaluate(werte)));
            /*
            tmpRes += "V"+numValues+seperator+
                    valToCSV(nanoToMikroS(avg.evaluate(werte)))+seperator+
                    valToCSV(nanoToMikroS(sd.evaluate(werte)))+seperator+
                    (valToCSV(nanoToMikroS(sd.evaluate(werte))/nanoToMikroS(avg.evaluate(werte)))+seperator+"\n");
                    */

            // csv output
            data[x][0] = "V"+numValues + seperator;
            System.out.println("###### CSV OUTPUT ######");
            String out = "";
            for (int d=0; d<werte.length; d++) {
                out += "V" + numValues + seperator + (""+numberFormat00.format(werte[d])).replace('.', ',') + seperator + (""+numberFormat00.format(werte[d]/1000)).replace('.', ',') + seperator + "\n";
                data[x][d+1] = (""+numberFormat00.format(werte[d]/1000)).replace('.', ',') + seperator;
            }
            csv += out;
            System.out.println(out);

        } // ende for
        //csv += tmpRes;
        String table = "\n";
        for (int a=0; a<data[0].length; a++) {
            for (int b=0; b<data.length; b++) {
                table += data[b][a];
            }
            table += "\n";
        }
        csv += table;

        String filename = "BoxPlot-CSV_Data-M5-" + datechain + "--Runs"+runs;
        File file = new File("C:\\Users\\Felix\\Desktop\\" + filename + ".csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(csv);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String valToCSV(double value) {
        NumberFormat numberFormat00 = new DecimalFormat("#0.0000");
        return numberFormat00.format(value).replace('.', ',');
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
