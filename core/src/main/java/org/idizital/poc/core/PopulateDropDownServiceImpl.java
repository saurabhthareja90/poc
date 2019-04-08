package org.idizital.poc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Dictionary;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adobe.fd.fp.util.FormsPortalConstants;
import com.adobe.granite.resourceresolverhelper.ResourceResolverHelper;


@Service(value = { PopulateDropDownService.class})
@Component(metatype = true, immediate = true, label = "CASA forms portal classification dropdown service")
public class PopulateDropDownServiceImpl extends AbstractFormService implements PopulateDropDownService{

	@Reference
	private ResourceResolverHelper resourceResolverHelper;

    @Property(value = FormsPortalConstants.STR_DEFAULT_DATA_SOURCE_NAME, label = "Name of the configured Data Source")
    private static final String DATA_SOURCE_PROP_NAME = "datasource";
    private String dataSource = FormsPortalConstants.STR_DEFAULT_DATA_SOURCE_NAME;
       
    private BundleContext bundleContext;
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
	
	protected void activate(ComponentContext context){
		@SuppressWarnings("unchecked")
		Dictionary<String, Object> props	= 	context.getProperties();
		dataSource              			= 	PropertiesUtil.toString(props.get(DATA_SOURCE_PROP_NAME), FormsPortalConstants.STR_DEFAULT_DATA_SOURCE_NAME);
		bundleContext 						= 	context.getBundleContext();
		// Initialise Attachments Directory
	}
	
	@Override
	public String getOptionsFromDataBase() {
		
		
		logger.info("Get options ");
		Connection connection 		= 	null;
		PreparedStatement prStmt 	= 	null;
		ResultSet resultSet 		= 	null;
		JSONArray options 			= new JSONArray();
		JSONObject response = new JSONObject();
		
		try{
			connection 				= 	getConnection();
			
			StringBuilder builder 	= 	new StringBuilder();
			builder.append("SELECT * ");
			builder.append("FROM " + getClassificationOptions() + " ");
			
			logger.debug("getOptionsFromDataBase() SQL:" + builder.toString()); 
			
			prStmt = connection.prepareStatement(builder.toString());
			
			resultSet = prStmt.executeQuery();
			while (resultSet.next()) {
				JSONObject option			= 	new JSONObject();
				option.put("text", resultSet.getString("text"));
				option.put("value", resultSet.getString("value"));	
				options.put(option);
			}
			closeAll(connection, prStmt, resultSet);
			response.put("options", options);
			
		} catch(Exception expGeneral) {
			logger.error("Exception occured in getOptions");
		} finally{
			closeAll(connection, prStmt, resultSet);
		}
		logger.info("Leaving getOptionsFromDataBase()");
		
		return response.toString();
	}
	
	private String getClassificationOptions(){
		return "aem_forms_classification_options";
	}
	
	/**
	 *  
	 * @return a connection using the configured DataSourcePool
	 * @throws SQLException
	 */
	private Connection getConnection() throws SQLException {
		return getConnection(getDataSourceName(), bundleContext);
	}
	
	private String getDataSourceName(){
		return dataSource;
	}
	

}
