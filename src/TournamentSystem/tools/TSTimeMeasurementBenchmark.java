package TournamentSystem.tools;

public class TSTimeMeasurementBenchmark {
    public static void main(String[] args) {
        //one();
        //two();
        three();
        //four();
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

    }
}
