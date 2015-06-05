package ca.kb1.csCWStools;

import java.util.ArrayList;
import java.util.List;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodePermissions;
import com.opentext.livelink.service.docman.NodeRight;
import com.opentext.livelink.service.docman.NodeRights;

public class AddNodeRightACL {

	static class RightID
	{
	    private long rightID;
	    private boolean updated;
	    
	    public RightID(long rightID) {
	    	this.rightID = rightID;
	    	this.updated = false;
	    }
	    
	    public long getRightID() {
	    	return this.rightID;
	    }
	    
	    public boolean isUpdated() {
	    	return this.updated;
	    }
	    
	    public void isUpdated(boolean updated) {
	    	this.updated = updated;
	    }

	}
	
	public static String addNodeRightACL(Node thisNode, DocumentManagement docManClient) {

		// reach out to the server and get the NodeRights associated with this Node
	    NodeRights thisNodeRights = docManClient.getNodeRights(thisNode.getID());

	    //-----			// extract the set of additional ACL's rights to this Node
	    List<NodeRight> thisNodeACLRights = thisNodeRights.getACLRights();

//	    System.out.println("| [" + thisNode.getID() + "] thisNodeACLRights.size()=" + thisNodeACLRights.size());

	    NodePermissions newNodePermissions = new NodePermissions();
	    // the usual setup for Content Owner
	    newNodePermissions.setSeePermission(true);
	    newNodePermissions.setSeeContentsPermission(true);
	    newNodePermissions.setModifyPermission(true);
	    newNodePermissions.setEditAttributesPermission(true);
	    newNodePermissions.setAddItemsPermission(true);
	    newNodePermissions.setReservePermission(true);
	    newNodePermissions.setDeletePermission(true);
	    newNodePermissions.setDeleteVersionsPermission(true);
	    newNodePermissions.setEditPermissionsPermission(false);

	    ArrayList<RightID> targetRightIDs = new ArrayList<RightID>();

	    targetRightIDs.add(new RightID(47815));
	    targetRightIDs.add(new RightID(47816));

	    // if the ID already has permissions on the node, reset them
		for (NodeRight thisNodeACLRight : thisNodeACLRights) {
			for (RightID thisTargetRightID : targetRightIDs) {
				if (thisTargetRightID.getRightID() == thisNodeACLRight.getRightID()) {
					thisNodeACLRight.setPermissions(newNodePermissions);
					docManClient.updateNodeRight(thisNode.getID(), thisNodeACLRight);
					thisTargetRightID.isUpdated(true); 
				}
			}
		}

		// attach the bits to a new user ACL
	    NodeRight newNodeACLRight = new NodeRight();
	    newNodeACLRight.setType("ACL");
	    newNodeACLRight.setPermissions(newNodePermissions);

	    // and add the user ACL for each of the users if it wasn't already there to update
		for (RightID thisTargetRightID : targetRightIDs) {
			if (!thisTargetRightID.isUpdated()) {
				newNodeACLRight.setRightID(thisTargetRightID.getRightID());
				docManClient.addNodeRight(thisNode.getID(), newNodeACLRight);
			}
		}

		return "";
	}	
}
