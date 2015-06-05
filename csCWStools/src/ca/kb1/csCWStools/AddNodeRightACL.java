package ca.kb1.csCWStools;

import java.util.ArrayList;
import java.util.List;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodePermissions;
import com.opentext.livelink.service.docman.NodeRight;
import com.opentext.livelink.service.docman.NodeRights;

public class AddNodeRightACL {

	// somewhere to efficiently track the users/groups who we're targeting to be updated or added
	static class RightID
	{
		// what is the Content Server ID for the user or group who's ACL we're targeting?
		private long rightID;
		
		// did we manage to update an existing ACL, or do we still need to add a new one?
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
		
		// extract the set of Assigned Access ACLs associated with this Node
		// note: a Content Server node also has a mandatory set of Owner permissions, an 
		//		 optional Owner Group with permissions, and optional Public Access 
		//		 permissions, in addition to these zero or more discrete user/group ACLs
		List<NodeRight> thisNodeACLRights = thisNodeRights.getACLRights();
		
		// the usual arrangement for a 'Content Owner'
		NodePermissions newNodePermissions = new NodePermissions();
		newNodePermissions.setSeePermission(true);
		newNodePermissions.setSeeContentsPermission(true);
		newNodePermissions.setModifyPermission(true);
		newNodePermissions.setEditAttributesPermission(true);
		newNodePermissions.setAddItemsPermission(true);
		newNodePermissions.setReservePermission(true);
		newNodePermissions.setDeleteVersionsPermission(true);
		newNodePermissions.setDeletePermission(true);
		newNodePermissions.setEditPermissionsPermission(false);
		
		// define some users we want to update/add ACLs for
		ArrayList<RightID> targetRightIDs = new ArrayList<RightID>();
		targetRightIDs.add(new RightID(47815));
		targetRightIDs.add(new RightID(47816));
		
		// if an ID already has some permissions on the node, reset them
		for (NodeRight thisNodeACLRight : thisNodeACLRights) {
			for (RightID thisTargetRightID : targetRightIDs) {
				if (thisTargetRightID.getRightID() == thisNodeACLRight.getRightID()) {
					thisNodeACLRight.setPermissions(newNodePermissions);
					docManClient.updateNodeRight(thisNode.getID(), thisNodeACLRight);
					thisTargetRightID.isUpdated(true); 
				}
			}
		}
		
		// otherwise attach the NodePermissions bits to a new template ACL
		NodeRight newNodeACLRight = new NodeRight();
		newNodeACLRight.setType("ACL");
		newNodeACLRight.setPermissions(newNodePermissions);
		
		// and then add an ACL for each of the users that didn't already have an ACL to update
		for (RightID thisTargetRightID : targetRightIDs) {
			if (!thisTargetRightID.isUpdated()) {
				newNodeACLRight.setRightID(thisTargetRightID.getRightID());
				docManClient.addNodeRight(thisNode.getID(), newNodeACLRight);
			}
		}
		
		return "";
	}	
}
