package tools;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Instant;

/**
 *  A test class for different ways to measure time in Java
 *  
 */
public class TestTimers {
	
	// approximate duration 60 microsec on my laptop
	private static void doSomethingLong() {
    	double sum=0.0;
    	long NN=60000; // 60k
    	for (long i=0; i<NN; i++)
    		sum=sum+Math.sqrt(Math.abs(Math.sin(0.3)));
	}
	
    public static void main(String[] args)
    {
    	ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    	System.out.println("CpuTimeSupported():"+threadMXBean.isCurrentThreadCpuTimeSupported());
    	long startTNanoThread = threadMXBean.getCurrentThreadCpuTime();
    	long startTNanoInstant = Instant.now().getNano();
    	long startTNano = System.nanoTime();
    	
    	long N=1000;
    	for (long i=0; i<N; i++) 
    		doSomethingLong();
    			
    	
    	// the following time enters System.nanoTime, Instant.* but NOT threadMXBean-time 
    	// (the current thread sleeps, so it does not get the time passing by added)
		try {
	    	Thread.sleep(0);
			// waiting time between agent-agent actions in milliseconds
		} catch (Exception e) {
			System.out.println("Thread 1");
		}
    	
    	long endTNano = System.nanoTime();
    	long endTNanoThread = threadMXBean.getCurrentThreadCpuTime();
    	long endTNanoInstant = Instant.now().getNano();
    	System.out.println("Time    nanoTime: "+(endTNano-startTNano));
    	System.out.println("Time  ThreadNano: "+(endTNanoThread-startTNanoThread));
    	System.out.println("Time InstantNano: "+(endTNanoInstant-startTNanoInstant));
    	System.out.println(startTNanoThread+","+endTNanoThread);
    	System.out.println(startTNanoInstant+","+endTNanoInstant);
    	long offset = 0;
    	System.out.println("Micros per op: "+((double)(endTNano-startTNano-offset))/(N*1000));
    }

}
