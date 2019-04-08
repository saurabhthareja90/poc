package org.idizital.poc.core;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class AbstractFormService {

	protected final String COL_DRAFT_ID = "draftID";
	protected final String COL_RENDER_PATH = "renderPath";
	
	protected final String COL_USERDATA_ID = "userdataID";
	
	protected final String COL_USERID = "userid";
	protected final String COL_CONTACTID = "contact_id";
	protected final String COL_LAST_MODIFIED = "lastModified";
	protected final String COL_LAST_MODIFIED_DATE = "lastModifiedDate";
	protected final String COL_PAYLOAD_DATA = "payload_data";
	protected final String COL_PAYLOAD_TYPE = "payload_type";
	protected final String COL_ONBEHALF_OF = "onBehalfOf";
	protected final String COL_FORM_NAME = "formName";
	protected final String COL_FORM_TITLE = "formTitle";
	protected final String COL_FORM_PATH = "formPath";
	protected final String COL_STATUS = "status";
	
	protected final String SERVICE_CATALOGUEITEM_ID = "casa_servicecatalogueitemid";
	
	protected final String PAGE_NUMBER = "pageNumber";
	protected final String PAGE_SIZE = "count";
	
	protected final String COL_APPLICANT_NAME = "applicant_name";
	
	protected String attachmentsDir = null;
	

	protected String getId() {
		//return String.valueOf(System.nanoTime());
		return UUID.randomUUID().toString();
	}
	
	protected void closeAll (Connection connection, PreparedStatement preparedStatement, ResultSet resultSet) {
		
		try {
			if (resultSet != null) {
				resultSet.close();
				resultSet = null;
			}
		}
		catch (SQLException expSQL) {
			resultSet = null;
		}
		
		try {
			if (preparedStatement != null) {
				preparedStatement.close();
				preparedStatement = null;
			}
		}
		catch (SQLException expSQL) {
			preparedStatement = null;
		}
		
		try {
			if (connection != null) {
				connection.close();
				connection = null;
			}
		}
		catch (SQLException expSQL) {
			connection = null;
		}
		
	}
	
	protected void rollback (Connection connection) {	
		try {
			if(connection != null){
	            connection.rollback();
			 }
		}
		catch (SQLException expSQL) {
			connection = null;
		}		
	}
	
	// Returns a connection using the configured DataSourcePool
	protected Connection getConnection (String datasourceName, BundleContext bundleContext) throws SQLException {
		Connection connection = null;
		try {
			String filter = "(&(objectclass=javax.sql.DataSource)(datasource.name=" + datasourceName + "))";
			ServiceReference[] refs = bundleContext.getAllServiceReferences(null, filter);
			if (refs != null && refs.length == 1) {
				DataSource dataSource = (javax.sql.DataSource) bundleContext.getService(refs[0]);
				connection = dataSource.getConnection();
				if (connection == null) {
					throw new SQLException("Cannot acquire SQL Connection for datasource : " + datasourceName);
				}
			}
			else {
				throw new SQLException("Datasource configuration not found for : " + datasourceName);
			}
		}
		catch (SQLException expSQL) {
			throw expSQL;
		}
		catch (Exception expGeneral) {
			throw new SQLException(expGeneral);
		}		
		return connection;
	}
	
	
	protected String getNodeValue (Document document, String fieldName) {
		String value = null;
		NodeList nodeList	= 	document.getElementsByTagName(fieldName);
		if (nodeList != null && nodeList.getLength() > 0) {
			Node node = nodeList.item(0).getFirstChild();
			if (node != null) {
				value = node.getNodeValue();
			}			
		}
		return value;
	}
	
	protected void initAttachmentsDirectory () {
		String tempDir	= 	System.getProperty("java.io.tmpdir");
		if (!tempDir.endsWith("" + File.separatorChar)) {
			tempDir += File.separatorChar;
		}		
		String aem_attachments_dir 			= 	tempDir + "aem_attachments";
		File dir = new File (aem_attachments_dir);
		if (!dir.exists()) {
			dir.mkdir();
		}
		this.attachmentsDir = aem_attachments_dir;
	}
	
}
