package ca.kb1.csCWStools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.ws.soap.SOAPFaultException;

import com.opentext.livelink.service.core.DataValue;
import com.opentext.livelink.service.core.StringValue;
import com.opentext.livelink.service.docman.AttributeGroup;
import com.opentext.livelink.service.docman.Metadata;
import com.opentext.livelink.service.docman.Node;
import com.sun.xml.ws.developer.StreamingDataHandler;

public class CalculateFingerprintForDocumentVersion {

	public static void calculateFingerprintForDocumentVersion(Node thisNode) {
		
		// get a reference to the CWS services we need
		SOAPServices soapServices = SOAPServices.getInstance();
		assert soapServices.docManClient() != null;
		assert soapServices.contentService() != null;
		
		String targetPath = null;
		
		StreamingDataHandler downloadStream = null;
		
		// Store the context ID for the download
		String contextID = null;
		
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ByteBuffer buff = ByteBuffer.allocate(1024000);
		
		try {
		
			// see if there is already an MD5sum defined for this document
			
			// pull the Metadata associated with this Node 
			Metadata thisNodeMetadata = soapServices.docManClient().getNode(thisNode.getID()).getMetadata();
//System.out.println("thisNodeAttributeGroups.size()=" +thisNodeAttributeGroups.size());
			
			// so long as there is something to look through
			if ((thisNodeMetadata.getAttributeGroups()).size() > 0) {
				List<AttributeGroup> thisNodeAttributeGroups = thisNodeMetadata.getAttributeGroups();
				
			// walk the sets of Attributes
			for (AttributeGroup thisNodeAttributeGroup : thisNodeAttributeGroups) {
//System.out.println("thisNodeAttributeGroup.getDisplayName()=" + thisNodeAttributeGroup.getDisplayName());
				
				// until we find the set of interest
				if (thisNodeAttributeGroup.getDisplayName().equals("Legacy Document Info")) {
					List<DataValue> thisNodeAttributeGroupDataValues = thisNodeAttributeGroup.getValues();
					
					// then walk the attributes within the set of interest
					for (DataValue thisNodeAttributeGroupDataValue : thisNodeAttributeGroupDataValues){
// System.out.println("thisNodeAttributeGroupDataValue.getDescription()=" + thisNodeAttributeGroupDataValue.getDescription());
						
						// until we find the attribute of interest
						if (thisNodeAttributeGroupDataValue.getDescription().equals("Legacy Fingerprint")) {
							
							// only work on it if it's not already defined
							if ((((StringValue)thisNodeAttributeGroupDataValue).getValues()).toString().equals("") 
											|| ((StringValue)thisNodeAttributeGroupDataValue).getValues().isEmpty()
											|| true) {
								
								// what is the NodeID of version #1 of this document?
								long versionID = (soapServices.docManClient().getVersion(thisNode.getID(), 1).getID());
								
								// Get the contentID for the first version and prep to download it
								contextID = soapServices.docManClient().getVersionContentsContext(versionID, 0);
								downloadStream = (StreamingDataHandler) soapServices.contentService().downloadContent(contextID);
								
								// stash a working copy of the body of this version somewhere
								File file = new File("c:/dump/" + versionID);
								
								try
								{
									downloadStream.moveTo(file);
// System.out.println("Downloaded " + file.length() + " bytes to z:/dump/" + versionID + ".\n");		
									
									FileChannel channel = (new FileInputStream(file).getChannel());
									while(channel.read(buff) != -1) {
										buff.flip();
										digest.update(buff);
										buff.clear();
									}
									channel.close();
									
								} catch (Exception e) {
									System.out.println("Failed to download file: " + file + "\n");
									System.out.println(e.getMessage());
								} finally {
									// Always close the streams
									try {
										downloadStream.close();
									} catch (IOException e) {
										System.out.println("Failed to close the download stream?\n");
										System.out.println(e.getMessage());
									}
								}
								
								String fingerprint = (new HexBinaryAdapter()).marshal(digest.digest());
//System.out.println ("MD5 is: " + fingerprint + " for " + thisNode.getID());
								
								// and then clean up the temporary download file
								file.delete();
								
								// clear out the attribute if it isn't already empty
								if (!((StringValue)thisNodeAttributeGroupDataValue).getValues().isEmpty())  ((StringValue)thisNodeAttributeGroupDataValue).getValues().clear();
								
								// and add in the new value
								((StringValue)thisNodeAttributeGroupDataValue).getValues().add(fingerprint);
								
								// now go and update the List of AttributeGroupDataValues with the modified AttributeGroupDataValue
								int indexOfAGDV = thisNodeAttributeGroupDataValues.indexOf(thisNodeAttributeGroupDataValue);
								thisNodeAttributeGroupDataValues.set(indexOfAGDV, thisNodeAttributeGroupDataValue);
								
								// finally go and update the List of AttributeGroups with the modified AttributeGroup
								int indexOfAG = thisNodeAttributeGroups.indexOf(thisNodeAttributeGroup);
								thisNodeMetadata.getAttributeGroups().set(indexOfAG, thisNodeAttributeGroup);
								
								// and commit the change back to the database
								soapServices.docManClient().setNodeMetadata(thisNode.getID(), thisNodeMetadata);
							}
						}
					}
				}
			}
		} else {
			System.out.println("Node: " + thisNode.getID() + " has no AttributeGroups?!?");
		}
		
		} catch (NullPointerException e) {
			System.out.println("Version 1 of node: " + thisNode.getID() + " is no longer available!");
		} catch (SOAPFaultException e) {
			if (e.getFault().getFaultCode().equals("a:DocMan.VersionRetrievalError")) {
				// when we tried to get the 'original' version that was checked into the system it had already aged into oblivion, so a different strategy is needed.
				System.out.println(" version not found at node: " + thisNode.getID() + "!");
			} else {
				// this isn't the Exception we're looking for...
				throw e;
			}
		}

	}
}
