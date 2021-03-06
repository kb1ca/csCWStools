package ca.kb1.csCWStools;

import java.util.List;

import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodeRight;
import com.opentext.livelink.service.docman.NodeRights;

public class AlterNodeRightBits {

	public static String alterNodeRightBits(Node thisNode) {
		
		// get a reference to the CWS services we need
		SOAPServices soapServices = SOAPServices.getInstance();
		assert soapServices.docManClient() != null;
		
		// reach out to the server and get the NodeRights associated with this Node
		NodeRights thisNodeRights = soapServices.docManClient().getNodeRights(thisNode.getID());
		
		// extract the Owners rights to this Node
		NodeRight thisNodeOwnerRight = thisNodeRights.getOwnerRight(); 
		
		// add the Delete bit to the Owners existing set of permissions if it is not already set
		if (thisNodeOwnerRight.getPermissions().isDeletePermission() == false) {
			(thisNodeOwnerRight.getPermissions()).setDeletePermission(true);
			soapServices.docManClient().updateNodeRight(thisNode.getID(), thisNodeOwnerRight);
		}
		
		// note: we're completely ignoring the Owner Group and Public Access rights in this example
		
		// extract the set of additional ACL's rights to this Node
		List<NodeRight> thisNodeACLRights = thisNodeRights.getACLRights();
		
		// remove the Delete bit from each ALC's existing set of permissions
		for (NodeRight thisNodeACLRight : thisNodeACLRights) {
			if (thisNodeACLRight.getPermissions().isDeletePermission() == true) {
				(thisNodeACLRight.getPermissions()).setDeletePermission(false);
				soapServices.docManClient().updateNodeRight(thisNode.getID(), thisNodeACLRight);
			}
		}
		
		return "";
	}
}
