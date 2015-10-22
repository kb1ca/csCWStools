package ca.kb1.csCWStools;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ca.kb1.csCWStools.WorkQueue;

public class Main {
	
	// Example of setting up and running NodeProducerConsumer to traverse an Opentext Content Server 10.5 system 
	// and perform specific actions against particular content, without foreknowedge of the nodes of interest
	
	public static void main(String[] args) throws MalformedURLException {
		
		// instantiate Singleton that provides interfaces to Content Server CWS
		List<String> SOAPServers = new ArrayList<String>();
		SOAPServers.add("http://cs-test-admin/cws/");
//		SOAPServers.add("http://cs-test-web1:81/cws/");
//		SOAPServers.add("http://cs-test-web2:81/cws/");
		// ...and any additional servers...
		
		// authorize the connection to CWS
		SOAPServices soapServices = SOAPServices.getInstance();
		soapServices.init("admin", "livelink", SOAPServers); // you have changed the default password, riiight?
		
		// where should we begin?
		int startingNode = 2000;	// all of Enterprise\
		
		// and how many threads are we going to use?
		// you should not use more threads than the total number of threads available across all the CWS servers in the pool
		int threadCount = 90;
		
		// note the starting time for statistics reporting
		long start = (new Date()).getTime();
		long prevPass = (new Date()).getTime();
		
		// create somewhere to keep all our tasks
		WorkQueue workQueue = new WorkQueue(threadCount, threadCount, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		
		// Start to recurse the tree from a specific starting node, producing more tasks as we go...
		Runnable task = new NodeProducerConsumer(workQueue, 
			soapServices.docManClient().getNode(startingNode), 
			soapServices.docManClient().getNode(startingNode).isIsContainer());
		
		workQueue.execute(task);
		
		// allow processing to come up to full speed
		try
		{
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Output some stats while we wait for the troops to finish up...
		long thisNodesProcessed = 0;
		long thisNodesQueued = 0;
		long thisNodesRecursed = 0;
		long thisPass = (new Date()).getTime();
		long prevNodesProcessed = 0;
		long prevNodesQueued = 0;
		long prevNodesRecursed = 0;
		do
		{
			try {
				thisNodesProcessed = workQueue.getProcessedNodeCount();
				thisNodesQueued = workQueue.getQueuedNodeCount();
				thisNodesRecursed = workQueue.getRecursedNodeCount();
				thisPass = (new Date()).getTime();
				System.out.println("Nodes Processed: " + thisNodesProcessed 
									+ " (" + (thisNodesProcessed - prevNodesProcessed)/((thisPass-prevPass)/1000) + "/sec), "
									+ "\tQueued: " + workQueue.getQueuedNodeCount() 
									+ " (" + (thisNodesQueued - prevNodesQueued)/((thisPass-prevPass)/1000) + "/sec), "
									+ "\tRecursed: " + workQueue.getRecursedNodeCount() 
									+ " (" + (thisNodesRecursed - prevNodesRecursed)/((thisPass-prevPass)/1000) + "/sec), "
//									+ "\tThreads Completed: " + workQueue.getCompletedTaskCount() 
//									+ "\tQueued: " + workQueue.getQueuedTaskCount() 
									+ "\tActive: " + workQueue.getActiveCount()
									);
				prevNodesProcessed = thisNodesProcessed;
				prevNodesQueued = thisNodesQueued;
				prevNodesRecursed = thisNodesRecursed;
				prevPass = thisPass;
			} catch (ArithmeticException e) {
				// if we get a math error bad things have happened?
				e.printStackTrace();
				System.exit(1);
			}
			
			// pause between statistics updates
			try {
				TimeUnit.SECONDS.sleep(60);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		// keep going until all our threads go inactive
		} while (!(workQueue.getActiveCount() == 0));
		
		long end = ((new Date()).getTime() - 10000); // offset the ending time to allow for the last thread inactivation timer delay
		
		// shut down all the worker threads in an orderly fashion
		workQueue.shutdown();
		try {
			workQueue.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// NOOP
		}
		
		// ... and report out the overall statistics
		System.out.println("+++Queue loaded Started at: " + (new SimpleDateFormat("HH:mm:ss")).format(start));
		System.out.println("+++Queue processing Finished " + workQueue.getProcessedNodeCount()  
								+ " nodes at: " + (new SimpleDateFormat("HH:mm:ss")).format(end) 
								+ " rate: " + workQueue.getProcessedNodeCount()/((end-start)/1000) 
								+ " nodes/sec ("+((end-start)/1000) 
								+ " seconds)"
								);
		
/*		
		// capture a record of the actions
		PrintWriter writer;
		try {
			writer = new PrintWriter ("c:/twiddler-log.txt", "UTF-8");
			for (String result : results) {
				writer.println(result);
			}
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/
	}	
}