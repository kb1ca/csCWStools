package ca.kb1.csCWStools;

import java.io.File;
import java.io.IOException;

import javax.xml.ws.soap.SOAPFaultException;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.makernotes.PanasonicMakernoteDescriptor;
import com.drew.metadata.exif.makernotes.PanasonicMakernoteDirectory;
import com.opentext.livelink.service.docman.Node;
import com.sun.xml.ws.developer.StreamingDataHandler;

public class FindImagesByEXIF {

	public static String findImagesByEXIF(Node thisNode) throws SOAPFaultException {
		// see https://drewnoakes.com/code/exif/
		
		// get a reference to the CWS services we need
		SOAPServices soapServices = SOAPServices.getInstance();
		assert soapServices.docManClient() != null;
		assert soapServices.contentService() != null;
		
		StreamingDataHandler downloadStream = null;
		
		// Store the context ID for the download
		String contextID = null;
		
// System.out.print("Getting contextID...");
		contextID = soapServices.docManClient().getVersionContentsContext(thisNode.getID(), 0);
		
// System.out.print("Downloading file...");
		downloadStream = (StreamingDataHandler) soapServices.contentService().downloadContent(contextID);
		
		// download the blob to a temporary location somewhere on disk
		// we could probably stream this in memory instead...
		File file = new File("z:/dump/" + thisNode.getID());
		
		try
		{
			downloadStream.moveTo(file);
// System.out.println("Downloaded " + file.length() + " bytes to z:/dump/" + thisNode.getID() + ".\n");		
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
		
		// now grab some data from the blob
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(file);
			ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			PanasonicMakernoteDirectory makernote = metadata.getFirstDirectoryOfType(PanasonicMakernoteDirectory.class);
			
			if (directory != null) {
				if (directory.getDescription(ExifIFD0Directory.TAG_MODEL).equals("DMC-FZ8")
						&& makernote.getDescription(PanasonicMakernoteDirectory.TAG_INTERNAL_SERIAL_NUMBER).equals("S0123456789")) {
							System.out.println ("Hit! at node: " + thisNode.getID());
				}
			}
		} catch (ImageProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// and then clean up the temporary download file
		file.delete();
		
		return "";
	}
}
