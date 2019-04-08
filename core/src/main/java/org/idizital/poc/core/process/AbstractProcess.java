package org.idizital.poc.core.process;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.xml.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.metadata.MetaDataMap;


public abstract class AbstractProcess {

	protected Logger logger 					= 	LoggerFactory.getLogger(this.getClass());
	public final String PAYLOAD_XML				=	"PAYLOAD_XML";
	final String PAYLOAD_JSON					=	"PAYLOAD_JSON";
	final String SUBFLOW_PAYLOAD_JSON			=	"SUBFLOW_PAYLOAD_JSON";
	final String IS_SUBFLOW						=	"IS_SUBFLOW";

	protected abstract ResourceResolver getResourceResolver ();

	protected ResourceResolver getResourceResolver (ResourceResolverFactory resourceResolverFactory, WorkflowSession workflowSession) throws Exception {
		final Map<String, Object> map = new HashMap<String, Object>();
        map.put( "user.jcr.session", getSession (workflowSession));
        return resourceResolverFactory.getResourceResolver(map);
	}

	protected Session getSession (WorkflowSession workflowSession) {
		return workflowSession.adaptTo(Session.class);
	}

	protected String getPayloadXML (ResourceResolver resourceResolver, String payloadPath, MetaDataMap metaDataMap) throws Exception {

		String payloadXML 			= 	null;
		if (metaDataMap.containsKey(PAYLOAD_XML)) {
			payloadXML =  metaDataMap.get(PAYLOAD_XML, String.class);
		}
		else {
			Resource resourcePayload	= 	resourceResolver.getResource(payloadPath);
			logger.debug("resourcePayload:" + resourcePayload);
			if (resourcePayload != null) {

				// Copy main data XML into data_dor.xml
				Session session 				= 	resourceResolver.adaptTo(Session.class);
	            Workspace workspace 			= 	session.getWorkspace();
	            Resource resourceDOR			= 	resourceResolver.getResource(payloadPath + "/" + "data_dor.xml");
	            if (resourceDOR != null) {
	            	resourceDOR.adaptTo(Node.class).remove();
	            	resourceResolver.commit();
	            	session.save();
	            	logger.debug("Removed existing DOR file.");
	            }

				workspace.copy(payloadPath + "/" + "data.xml", payloadPath + "/" + "data_dor.xml");
	            session.save();

				Node nodePayload				=	resourcePayload.adaptTo(Node.class).getNode("data.xml/jcr:content");
				InputStream streamData			=	nodePayload.getProperty (javax.jcr.Property.JCR_DATA).getBinary().getStream();
				DocumentBuilderFactory factory 	= 	DocumentBuilderFactory.newInstance();
				Document document 				= 	factory.newDocumentBuilder().parse(streamData);
		        XPath xpath						= 	XPathFactory.newInstance().newXPath();
		        org.w3c.dom.Node dataNode		=	getDataNode(document, xpath, "/afData/afBoundData/data");
		        if (dataNode == null) {
		        	dataNode					=	getDataNode(document, xpath, "/afData/afUnboundData/data");
		        }
		        if (dataNode == null) {
		        	dataNode					=	getDataNode(document, xpath, "/data");
		        }
				DOMSource domSource 			= 	new DOMSource(dataNode);
				StringWriter writer 			= 	new StringWriter();
				StreamResult result 			= 	new StreamResult(writer);
				TransformerFactory tFactory 	= 	TransformerFactory.newInstance();
				Transformer transformer 		= 	tFactory.newTransformer();
				transformer.transform(domSource, result);
				payloadXML 						= 	writer.toString();
				logger.debug("payloadXML:" + payloadXML);
				metaDataMap.put(PAYLOAD_XML, payloadXML);
			}
		}
		return payloadXML;
	}

	protected boolean isBlank (String value) {
    	if (value != null && !value.trim().equals("")) {
    		return false;
    	}
    	return true;
    }


	protected void updateResponseDataMappings(MetaDataMap metaDataMap, MetaDataMap workflowMetaDataMap, JSONObject jsonPayload, String responseString) throws JSONException {
		JSONObject jsonObject 	= 	new JSONObject(responseString);
		String [] responseDataMappings 	= 	(String []) metaDataMap.get("responseDataMappings", String[].class);
		//logger.debug ("Response Data Mapping : " + responseDataMappings);
		//logger.debug ("metaDataMap : " + metaDataMap);
		if (responseDataMappings != null) {
			JSONObject jsonObjectData = null;
			if (!workflowMetaDataMap.containsKey(IS_SUBFLOW)) {
				jsonObjectData = jsonPayload.getJSONObject("data");
			}
			else {
				jsonObjectData = jsonPayload.getJSONObject("subflow_data");
			}
			for (String responseDataMapping : responseDataMappings) {
		    	//logger.debug ("Response Data Mapping : " + responseDataMapping);
		        if (jsonObject.has(responseDataMapping)) {
		        	jsonObjectData.put(responseDataMapping, jsonObject.getString(responseDataMapping));
		        }
		    }
			if (!workflowMetaDataMap.containsKey(IS_SUBFLOW)) {
				workflowMetaDataMap.put(PAYLOAD_JSON, jsonPayload.toString());
			}
			else {
				workflowMetaDataMap.put(SUBFLOW_PAYLOAD_JSON, jsonPayload.toString());
			}

		}
		logger.debug ("Updated JSON Payload : " + jsonPayload.toString());
	}


	protected MetaDataMap getWorkflowMetaDataMap (WorkItem workItem) throws Exception {
		return workItem.getWorkflow().getWorkflowData().getMetaDataMap();
	}

	protected String getPayloadJSON (ResourceResolver resourceResolver, String payloadPath, MetaDataMap metaDataMap) throws Exception {
		String payloadJSON 	=	null;
		if (metaDataMap.containsKey(PAYLOAD_JSON)) {
			payloadJSON =  metaDataMap.get(PAYLOAD_JSON, String.class);
		}
		else {
			String payloadXML		=	getPayloadXML(resourceResolver, payloadPath, metaDataMap);
			JSONObject jsonPayload 	=	XML.toJSONObject(payloadXML);
			if (jsonPayload != null && jsonPayload.has("data")) {
				JSONObject data 	=	jsonPayload.getJSONObject("data");
				JSONArray names 	= 	data.names();
				for (int i = 0; i < names.length(); i++) {
					String name = (String) names.get(i);
					Object value = data.get(name);
					if (value instanceof JSONObject) {
						if (((JSONObject) value).names() == null) {
							data.remove(name);
						}
					}

				}
			}
			payloadJSON 		=	jsonPayload.toString();
			metaDataMap.put(PAYLOAD_JSON, payloadJSON);
		}
		return payloadJSON;
	}

	protected org.w3c.dom.Node getDataNode (Document document, XPath xpath, String pattern) throws Exception {
        XPathExpression expr 	= 	xpath.compile(pattern);
        NodeList nodes 			= 	(NodeList) expr.evaluate(document, XPathConstants.NODESET);
        if (nodes.getLength() == 1) {
        	if (nodes.item(0).getChildNodes().getLength() > 0) {
        		return nodes.item(0);
        	}
        }
		return null;
	}

	protected void runScript(WorkflowSession session, WorkItem workItem, MetaDataMap metaDataMap, String scriptAttribute) throws WorkflowException {
    	if (metaDataMap.containsKey(scriptAttribute)) {
            ResourceResolver resourceResolver 	= 	(ResourceResolver)session.adaptTo(ResourceResolver.class);
            String scriptPath 					= 	(String) metaDataMap.get(scriptAttribute, String.class);
            Resource scriptResource				= 	resourceResolver.getResource(scriptPath);
            if(scriptResource == null) {
                throw new WorkflowException("Script " + scriptPath + " not found for workflow " + workItem.getWorkflow().getWorkflowModel().getId() + " and step " + workItem.getNode().getTitle());
            }
            SlingScript script = (SlingScript) scriptResource.adaptTo(SlingScript.class);
            if (script != null) {
	            SlingBindings props = new SlingBindings();
	            props.put("workflowData", workItem.getWorkflowData());
	            props.put("workItem", workItem);
	            props.put("workflowSession", session);
	            props.put("payload", (String) metaDataMap.get("payload", String.class));
	            props.put("jcrSession", resourceResolver.adaptTo(Session.class));
	            script.eval(props);
            }
        }
    }

    protected Object evalScript(WorkflowSession session, WorkItem workItem, MetaDataMap metaDataMap, String scriptAttribute) throws WorkflowException {
    	logger.debug("Calling evalScript **********:", scriptAttribute);
    	if (metaDataMap.containsKey(scriptAttribute)) {
            ResourceResolver resourceResolver 	= 	(ResourceResolver)session.adaptTo(ResourceResolver.class);
            String scriptPath 					= 	(String) metaDataMap.get(scriptAttribute, String.class);
            Resource scriptResource				= 	resourceResolver.getResource(scriptPath);
            if(scriptResource == null) {
                throw new WorkflowException("Script " + scriptPath + " not found for workflow " + workItem.getWorkflow().getWorkflowModel().getId() + " and step " + workItem.getNode().getTitle());
            }
            SlingScript script = (SlingScript) scriptResource.adaptTo(SlingScript.class);
            if (script != null) {
	            SlingBindings props = new SlingBindings();
	            props.put("workflowData", workItem.getWorkflowData());
	            props.put("workItem", workItem);
	            props.put("workflowSession", session);
	            props.put("payload", (String) metaDataMap.get("payload", String.class));
	            props.put("dataMappings", (String) metaDataMap.get("dataMappings", String.class));
	            props.put("jcrSession", resourceResolver.adaptTo(Session.class));
	            logger.debug("Calling script {}.", script);
	            Object result = (Boolean) script.eval(props);
	            logger.debug("Result from script {} is {}", script, result);
	            return result;
            }
        }
        return false;
    }


	protected void printMetadataMap(MetaDataMap metaDataMap) {
		if (logger.isDebugEnabled()) {
	    	Iterator<String> iterator 	= 	metaDataMap.keySet().iterator();
	    	while (iterator.hasNext()) {
				String key = (String) iterator.next();
				logger.debug ("Workflow Step Parameter [" + key + "]: " + metaDataMap.get(key, String.class));
			}
    	}
	}

}
