package ca.kb1.csCWStools;

/* 
 * Multiple dual-purpose producer/consumer threads - multiple DocumentManagerClient instances with SOAP calls spread amongst them
 *  also implements final processing on only one node per thread
 */

/*
 * Note: Consumer threads may crash unexpectedly on fast Content Server infrastructure with exceptions similar to:
 *  com.sun.xml.ws.client.clientTransportException java.net.bindException address already in use
 *  
 *  This can occur if the computer running this process runs out of ephemeral TCP sockets. ie. 
 *	netstat -anop tcp 
 *  will show a very large number of sockets in the TIME_WAIT state. The problem will be exacerbated if the CWS server
 *  does not allow keep-alives (depending on your JAX-WS version).
 *
 *  It may be necessary to alter the TcpTimedWaitDelay registry key on this to ensure sockets are recycled fast enough to 
 *	prevent exhaustion. See http://technet.microsoft.com/en-us/library/cc938218.aspx
 */

/* 
 * Note: javax.xml.ws.soap.SOAPFaultException: Your session has timed-out. may be thrown for long-running jobs.
 *  you may need to alter 
 *	'Security Parameters -> Secure Request Token Expiration' to 'unlimited' 
 *  and/or 
 *	'Security Parameters -> Cookie Authentication Information' to 'never expire' 
 *  on every participating CWS server while this job is running.
 */

import ca.kb1.csCWStools.WorkQueue;
// import ca.kb1.csCWStools.Main.WorkQueue;
// import ca.kb1.csCWStools.FindImagesByEXIF;
import ca.kb1.csCWStools.CalculateFingerprintForDocumentVersion;

import java.util.List;

import javax.xml.ws.soap.SOAPFaultException;

import com.opentext.livelink.service.docman.Node;
import com.sun.xml.ws.client.ClientTransportException;

public class NodeProducerConsumer implements Runnable{

	// Store the executor queue
	private final WorkQueue workQueue;
	
	// Store the Node this task is going to process
	private final Node thisNode;

	// Do we need to look inside this node?
	private final boolean recurseInto;
	
	// Constructor
	public NodeProducerConsumer(WorkQueue workQueue, Node thisNode, boolean recurseInto) {
		this.workQueue = workQueue;
		this.thisNode = thisNode;
		this.recurseInto = recurseInto;
	}

@Override
public void run() 
	{
		// walk through the nodes, randomly directing work at each CWS SOAP server in the array of docManClients
		
		// get a reference to the CWS services we need
		SOAPServices soapServices = SOAPServices.getInstance();
		assert soapServices.docManClient() != null;
		
		// if we were passed a container that needs to be examined
		if (recurseInto) {
			// find out if there are any sub-nodes of this node.
			// Note: just fetch the 'light' definition of each sub-node and we'll pull what we need later (much faster, especially if a node might not end up being altered)
			List<Node> children = soapServices.docManClient().listNodes(thisNode.getID(), true);
			
			// process the nodes within this container
			for (Node child : children) {
				
				// if this is a sub-container put it on to the work list for someone else to recurse into later
				if (child.isIsContainer() && !workQueue.isRecursedNode(child.getID())) {
					
					// but we're not going to recurse into Physical Folders as they have no contents
					//  or Physical Boxes either as we encounter their contents elsewhere in the tree
					if (!child.getType().equals("PhysicalItemContainer") && !child.getType().equals("PhysicalItemBox")) {
// System.out.println("|-[" + child.getID() + "] " + child.getType() + ":" + child.getName() + " on to the queue");
// System.out.println("|-Queueing [" + child.getID() + "] for recursion");
						if (workQueue.getRecursedNodePath(thisNode.getID()) == null) {
							// we must be working in the starting node, so work out where we are in the tree
							Long myParent = thisNode.getParentID();
							String pathFromRoot = "";
							if (myParent == -1) {
								// this node is the top of the tree
								pathFromRoot = "\\";
							} else {
								do {
									// determine how to get here using recursion up the tree
									pathFromRoot = (soapServices.docManClient().getNode(myParent)).getName() + "\\" + pathFromRoot;
									myParent = (soapServices.docManClient()).getNode(myParent).getParentID();
// System.out.println("parent node: " + myParent + " pathFromRoot: " + pathFromRoot);
								} while (!(myParent == -1));
							}
// System.out.println("root node " + thisNode.getName());
							workQueue.addRecursedNode(thisNode.getID(), pathFromRoot + thisNode.getName() + "\\");
// System.out.println("root node path " + workQueue.getRecursedNodePath(thisNode.getID()));
						} 
// System.out.println("child node " + child.getName());
						workQueue.addRecursedNode(child.getID(), workQueue.getRecursedNodePath(thisNode.getID()) + "" + child.getName() + "\\");
// System.out.println("child node path " + workQueue.getRecursedNodePath(thisNode.getID()) + "" + child.getName() + "\\");
						
						Runnable task = new NodeProducerConsumer(workQueue, child, true);
						workQueue.execute(task);
					}
				}
				// put sub-nodes on to the work list as a leaf, for someone else to process (this includes the containers themselves)
// System.out.println("|-[" + child.getID() + "] " + child.getType() + ":" + child.getName() + " on to the queue");
				if (!workQueue.isQueuedNode(child.getID())) {
					Runnable task = new NodeProducerConsumer(workQueue, child, false);
// System.out.println("|-Queueing [" + child.getID() + "] for processing");
					workQueue.execute(task);
					workQueue.addQueuedNode(child.getID());
				}
			}
//***********
		} else {
			// thisNode is a leaf or a reference to a container itself so process it 
			try
				{
				//--------------------------------------------
				// put your node-manipulation work here...
				//--------------------------------------------
				
System.out.println("  processing " + thisNode.getID() + " : " + thisNode.getName() + " : " + thisNode.getType() + " : " + thisNode.isIsContainer() + " : " + recurseInto);
				
//-----
/*
				if (thisNode.getType().equals("Document") && thisNode.getName().toLowerCase().endsWith(".jpg")) {
						String result = FindImagesByEXIF.findImagesByEXIF(thisNode);
				}
*/
				if (thisNode.getType().equals("Document")) {
						CalculateFingerprintForDocumentVersion.calculateFingerprintForDocumentVersion(thisNode);
				}
//-----
				workQueue.addProcessedNode(thisNode.getID());
			} catch (SOAPFaultException e) {
				System.out.println("Something broke at node: " + thisNode.getID() + "when using CWS server!");
				System.out.println(e.getFault().getFaultCode() + " : " + e.getMessage());
			} catch (ClientTransportException e) {
//				System.out.println("Something broke at node: " + thisNode.getID() + "when using CWS server #" + serverChosen + " !");
				System.out.println("Something broke at node: " + thisNode.getID() + "when using CWS server!");
				System.out.println(e + " : " + e.getMessage());
			}
//			NodeAuditRecord auditrecord = soapServices.docManClient().getNodeAuditRecords(0).
		}
	}
}
