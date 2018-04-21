package tuc.sk;
 
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*; 
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import org.json.*;



 
public class Fileservice extends HttpServlet {
	
	public static boolean validateHmac(String fileId, String userId, String validTill, String hmacString) {
		return true;
	}
	
	public static String getRepositoryPath() {
		// outside of webapp!!
		return "/tmp/";
	}
	
	private int counter = 0;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			   throws IOException, ServletException {
		
		if (Zooconf.getInstance().getServiceConfig() == null) { 
			System.err.println("Service not configured");
			response.setContentType("text/html");
			response.sendError(response.SC_SERVICE_UNAVAILABLE , "Service not configured");
			return;
		}
		
		
		String path = request.getPathInfo();
		if (path == null || path.equals("/")) {
			response.setContentType("text/html");
			response.sendError(response.SC_NOT_FOUND, "Missing file path");
			return;
		}
		String[] parts = path.split("/");
		if (parts.length != 2) {
			response.setContentType("text/html");
			response.sendError(response.SC_NOT_FOUND, "Invalid file");
			return;
		}
		String fileId = parts[1];
		
		String userId = request.getParameter("userId");
		String validTill = request.getParameter("validTill");
		String hmacString = request.getParameter("hmac");
		
		if (!this.validateHmac(fileId, userId, validTill, hmacString)) {
			response.setContentType("text/html");
			response.sendError(response.SC_FORBIDDEN, "Access denied");
			return;
		}
		
		//"/usr/share/pixmaps/language-selector.png"
		String fullFilePath = this.getRepositoryPath()+fileId;
		File f = new File(fullFilePath);
		if(!f.exists() || f.isDirectory()) { 
			// should never happen!!! 
			response.setContentType("text/html");
			response.sendError(response.SC_NOT_FOUND, "Invalid file");
			return;
		}
		

		String extension = "";

		int i = fullFilePath.lastIndexOf('.');
		if (i > 0) {
			extension = fullFilePath.substring(i+1);
		}
		String mime = "application/octet-stream";
		if (extension.equalsIgnoreCase("png")) {
			mime = "image/png"; 
		}
		if (extension.equalsIgnoreCase("jpg")) {
			mime = "image/jpeg"; 
		}
		if (extension.equalsIgnoreCase("gif")) {
			mime = "image/gif"; 
		}
		response.setContentType(mime); 
		ServletOutputStream sos = response.getOutputStream();
		DataInputStream dis = new DataInputStream(new FileInputStream(f));
		byte[] barray = new byte[(int) f.length()];

		try 
		{ 
			dis.readFully(barray);           // now the array contains the image
		}
		catch (Exception e) 
		{ 
			barray = null; 
		}
		finally 
		{ 
			dis.close( ); 
		}

		sos.write(barray);                 // send the byte array to client
		sos.close();

		

	}
	
}
