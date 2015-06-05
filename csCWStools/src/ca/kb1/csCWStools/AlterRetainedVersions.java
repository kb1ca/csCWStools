package ca.kb1.csCWStools;

import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.Node;
import com.opentext.livelink.service.docman.NodeVersionInfo;

public class AlterRetainedVersions {

	public void alterRetainedVersions(Node thisNode, DocumentManagement docManClient) {
		
		// First, we need to pull a more complete Node object to work with NodeVersionInfo - thisNode is the low calorie version
		Node thisNodeWithMetadata = docManClient.getNode(thisNode.getID());
		
		// If it's still at default, then alter the maximum number of versions that will be retained by the node
		if (thisNodeWithMetadata.getVersionInfo().getVersionsToKeep() == 25) {
			NodeVersionInfo thisNodeVersionInfo = thisNodeWithMetadata.getVersionInfo();
			thisNodeVersionInfo.setVersionsToKeep(-1);
			thisNodeWithMetadata.setVersionInfo(thisNodeVersionInfo);
			docManClient.updateNode(thisNodeWithMetadata);
		}
		
	}
}
