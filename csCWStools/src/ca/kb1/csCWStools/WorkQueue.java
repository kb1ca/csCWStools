package ca.kb1.csCWStools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WorkQueue extends ThreadPoolExecutor {
		
		//-----
		//
		public WorkQueue(int corePoolSize, int maximumPoolSize,
						 long keepAliveTime, TimeUnit unit,
						 BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		}
		
		public long getQueuedTaskCount() {
			return super.getQueue().size();
		}
		
		//-----
		// Track nodes we've already recursed into
		private final HashMap<Long, String> recursedNodes = new HashMap<Long, String>();
		
		public void addRecursedNode(Long nodeID, String path) {
			recursedNodes.put(nodeID, path);
		}
		
		public boolean isRecursedNode(Long nodeID) {
			return (recursedNodes.containsKey(nodeID));
		}
		
		public long getRecursedNodeCount() {
			return recursedNodes.size();
		}
		
		public String getRecursedNodePath(Long nodeId) {
			return recursedNodes.get(nodeId);
		}
		
		//-----
		// Track nodes we've queued for processing
		private final HashSet<Long> queuedNodes = new HashSet<Long>();
		
		public void addQueuedNode(Long nodeID) {
			queuedNodes.add(nodeID);
		}
		
		public boolean isQueuedNode(Long nodeID) {
			return (queuedNodes.contains(nodeID));
		}
		
		public long getQueuedNodeCount() {
			return queuedNodes.size();
		}
		
		//-----
		// Track nodes we've already processed
		private final HashSet<Long> processedNodes = new HashSet<Long>();
		
		public void addProcessedNode(Long nodeID) {
			processedNodes.add(nodeID);
		}

		public boolean isProcessedNode(Long nodeID) {
			return (processedNodes.contains(nodeID));
		}
 
		public long getProcessedNodeCount() {
			return processedNodes.size();
		}
	}
