package ca.kb1.csCWStools;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPFaultException;

import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.developer.WSBindingProvider;
import com.opentext.ecm.api.OTAuthentication;
import com.opentext.livelink.service.core.Authentication;
import com.opentext.livelink.service.core.Authentication_Service;
import com.opentext.livelink.service.core.ContentService;
import com.opentext.livelink.service.core.ContentService_Service;
import com.opentext.livelink.service.docman.DocumentManagement;
import com.opentext.livelink.service.docman.DocumentManagement_Service;
import com.opentext.livelink.service.physobj.PhysicalObjects;
import com.opentext.livelink.service.physobj.PhysicalObjects_Service;

// follows thread-safe Singleton pattern
public class SOAPServices {

	// this class stores a collection of instances of Content Server CWS SOAP services
	private static SOAPServices instance;

	private SOAPServices(){}
	
    public static synchronized SOAPServices getInstance(){
        if(instance == null){
            instance = new SOAPServices();
        }
        return instance;
    }
    
	// Constructor
	public void init(String USERNAME, String PASSWORD, List<String> SOAPServers ) throws MalformedURLException {
		// we assume that the 1st SOAPServer is going to provide authentication
		Authentication authClient = (new Authentication_Service(new URL( SOAPServers.get(0) + "Authentication.svc"))).getBasicHttpBindingAuthentication();

		// Call the AuthenticateUser() method to get the authentication token
		try
		{
			System.out.print("Authenticating User...");
			this.authToken = authClient.authenticateUser(USERNAME, PASSWORD);
			System.out.println("SUCCESS! [got " + this.authToken + " ]\n");

			// Create the OTAuthentication object and set the authentication token obtained above
			OTAuthentication otAuth = new OTAuthentication();
			otAuth.setAuthenticationToken(this.authToken);

			// Create a SOAP header
			SOAPHeader header = MessageFactory.newInstance().createMessage().getSOAPPart().getEnvelope().getHeader();

			// Add the OTAuthentication SOAP header element
			SOAPHeaderElement otAuthElement = header.addHeaderElement(new QName(ECM_API_NAMESPACE, "OTAuthentication"));

			// Add the AuthenticationToken SOAP element
			SOAPElement authTokenElement = otAuthElement.addChildElement(new QName(ECM_API_NAMESPACE, "AuthenticationToken"));
			authTokenElement.addTextNode(otAuth.getAuthenticationToken());
			
		
			for (String SOAPServer : SOAPServers ) {
				DocumentManagement aDocManClient = (new DocumentManagement_Service(new URL( SOAPServer + "DocumentManagement.svc"))).getBasicHttpBindingDocumentManagement();
				((WSBindingProvider) aDocManClient).setOutboundHeaders(Headers.create(otAuthElement));
				this.docManClients.add(aDocManClient);

				ContentService aContentService = (new ContentService_Service(new URL( SOAPServer + "ContentService.svc"))).getBasicHttpBindingContentService(new MTOMFeature());
				((WSBindingProvider) aContentService).setOutboundHeaders(Headers.create(otAuthElement));
				this.contentServices.add(aContentService);

				PhysicalObjects aPhysicalObjectsService = (new PhysicalObjects_Service(new URL( SOAPServer + "PhysicalObjects.svc"))).getBasicHttpBindingPhysicalObjects();
				((WSBindingProvider) aPhysicalObjectsService).setOutboundHeaders(Headers.create(otAuthElement));
				this.physicalObjectsServices.add(aPhysicalObjectsService);
			}
			
		} catch (SOAPException e) {
			System.out.println("SOAP fault in SOAPServices constructor!\n");
			System.out.println(e.getMessage());
			return;
		} catch (SOAPFaultException e) {
			System.out.println("SOAP fault in SOAPServices constructor!\n");
			System.out.println(e.getFault().getFaultCode() + " : " + e.getMessage());
			return;
		}
		
	}

	// Namespace for the SOAP headers
	public final static String ECM_API_NAMESPACE = "urn:api.ecm.opentext.com";

	// store the Authentication token
	private String authToken = null;

	// store the available Document_Management services
	private List<DocumentManagement> docManClients = new ArrayList<DocumentManagement>();
	
	// return a random service from the pool
	public DocumentManagement docManClient() {
		return docManClients.get((new Random()).nextInt(docManClients.size()));
	}
	
	// store the available Document_Management services
	private List<ContentService> contentServices = new ArrayList<ContentService>();
	
	// return a random service from the pool
	public ContentService contentService() {
		return contentServices.get((new Random()).nextInt(contentServices.size()));
	}

	// store the available PhysicalObjects services
	private List<PhysicalObjects> physicalObjectsServices = new ArrayList<PhysicalObjects>();
	
	public PhysicalObjects physicalObjectsService() {
	// return a random service from the pool
		return physicalObjectsServices.get((new Random()).nextInt(physicalObjectsServices.size()));
	}
}
