package org.idizital.poc.core.servlets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Session;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.idizital.poc.core.PopulateDropDownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;

@SlingServlet(paths = { "/bin/generic/schemadropdown" }, methods = { "GET" }, metatype=false)
public class SchemaDropDownServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = 1L;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Reference
	protected  SlingRepository slingRepository;

	@Reference
	private SlingRepository repository;

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Reference
	private PopulateDropDownService populateDropDownService;

	@Override
	protected void doGet(SlingHttpServletRequest slingRequest, SlingHttpServletResponse slingResponse) throws ServletException, IOException {
		logger.info("Entered doGet()");

		String options = populateDropDownService.getOptionsFromDataBase();
	/*	String options = "{\n" + 
				" \"options\": [\n" + 
				" {\n" + 
				" \"value\": \"en-gb\",\n" + 
				" \"text\": \"English-United Kingdom\"\n" + 
				" },\n" + 
				" {\n" + 
				" \"value\": \"en-us\",\n" + 
				" \"text\": \"English-United States\"\n" + 
				" },\n" + 
				" {\n" + 
				" \"value\": \"fr-ca\",\n" + 
				" \"text\": \"French-Canada\"\n" + 
				" },\n" + 
				" {\n" + 
				" \"value\": \"fr-fr\",\n" + 
				" \"text\": \"French-France\"\n" + 
				" },\n" + 
				" {\n" + 
				" \"value\": \"de-de\",\n" + 
				" \"text\": \"German-Germany\"\n" + 
				" },\n" + 
				" {\n" + 
				" \"value\": \"es-mx\",\n" + 
				" \"text\": \"Spanish-Mexico\"\n" + 
				" },\n" + 
				" {\n" + 
				" \"value\": \"es-es\",\n" + 
				" \"text\": \"Spanish-Spain\"\n" + 
				" },\n" + 
				" {\n" + 
				" \"value\": \"new-opt\",\n" + 
				" \"text\": \"New option\"\n" + 
				" }\n" +
				" ]\n" + 
				"}";*/

		//use ByteArrayInputStream to get the bytes of the String and convert them to InputStream.
		InputStream inputStream = new ByteArrayInputStream(options.getBytes(Charset.forName("UTF-8")));


		AssetManager assetManager;
		try {
			assetManager = getAdminResourceResolver().adaptTo(AssetManager.class);
			Asset imageAsset = assetManager.createAsset("/content/dam/options.json", inputStream, "application/json", true);
			if(imageAsset!= null) {
				logger.info("Asset created : "+ imageAsset.getPath());
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			logger.error(e1.getMessage());
		}
		slingResponse.setContentType("application/json");
		slingResponse.getWriter().write(options);

		logger.info("Leaving doGet()");
	}

	private ResourceResolver getAdminResourceResolver () throws Exception  {
		Map<String, Object> params			= 	new HashMap<String, Object>();
		params.put( "user.jcr.session", getAdminSession());
		return resourceResolverFactory.getResourceResolver(params);
	}

	@SuppressWarnings("deprecation")
	private Session getAdminSession () throws Exception {
		return repository.loginAdministrative(null);
	}

}
