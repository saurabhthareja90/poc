package org.idizital.poc.core.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.commons.util.DamUtil;
import java.util.HashMap;
import java.util.Map;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.idizital.poc.core.exception.ApplicationException;
import org.osgi.framework.Constants;


@Component
@Service
public class MetadataProcessorCustomProcess extends AbstractProcess implements WorkflowProcess {

	@Property(value = "Required field check workflow process")
	static final String DESCRIPTION = Constants.SERVICE_DESCRIPTION;

	@Property(value = "Adobe Systems")
	static final String VENDOR = Constants.SERVICE_VENDOR;

	@Property(value = "Required Field Check Process")
	static final String LABEL="process.label";

	protected Logger logger 	= LoggerFactory.getLogger(this.getClass());

	static final String SEPARATOR = "**********************************************";

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	private ResourceResolver resourceResolver;

	private static Map<String, String> WORKFLOW_MODEL_FORM_MAP = new HashMap<String, String> ();
	static {
		WORKFLOW_MODEL_FORM_MAP.put(WorkflowConstants.ARN_WORKFLOW_MODEL, WorkflowConstants.ARN_FORM_NAME);
	}

	@Override
	public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
		logger.debug (SEPARATOR);
		logger.debug ("Start Workflow Step " + workItem.getNode().getTitle());



		logger.debug("Required fields  **** " + metaDataMap.get("PROCESS_ARGS", String.class));
		String[] options = metaDataMap.get("PROCESS_ARGS", String.class).split(";");
		String[] folders = null;
		String[] requiredFields = null;

		if(options != null) {
			for(String option: options) {
				if(option.startsWith("folders")) {
					folders = option.split("=")[1].split(",");
				}else if(option.startsWith("requiredFields")) {
					requiredFields= option.split("=")[1].split(",");
				}
			}
		}


		try {
			if (((Boolean)workItem.getWorkflow().getMetaDataMap().get("com.day.cq.dam.core.process.meta.extraced", Boolean.FALSE)).booleanValue())

			{
				logger.debug("already executed in workflow {}", workItem.getWorkflow().getId());
				return;
			}
			for(String requiredField : requiredFields) {

				resourceResolver 					= 	getResourceResolver(resourceResolverFactory, workflowSession);

				String payloadPath 					= 	(String) workItem.getWorkflowData().getPayload();

				final Asset asset = DamUtil.resolveToAsset(resourceResolver.getResource(payloadPath));

				String metadataPath = String.format("%s/%s/%s",asset.getPath(), JcrConstants.JCR_CONTENT, DamConstants.METADATA_FOLDER);

				Resource metadataResource = resourceResolver.getResource(metadataPath);

				//get parent reosources

				Resource parentResource 					=	resourceResolver.getResource(asset.adaptTo(Resource.class).getParent().getPath());

				String profile = null;

				String parentMetadataPath = String.format("%s/%s",parentResource.getPath(), JcrConstants.JCR_CONTENT);

				Resource parentMetadataResource = parentMetadataPath != null ? resourceResolver.getResource(parentMetadataPath) : null;

				profile = parentMetadataResource != null ? parentMetadataResource.getValueMap().get("metadataProfile", String.class) : null; 


				String classfication = metadataResource.getValueMap().get(requiredField, String.class);

				//match check conditions on resource
				if(null == classfication && profile == null && parentResource.getResourceType().contains("Folder") && checkPayloadResource(folders, parentResource.getPath())) {
					logger.error("Required metadata is missing");
					throw new WorkflowException("Required metadata is missing.", null);
				}
			}

		}
		catch (Exception expGeneral) {
			new ApplicationException("Required Metadata is missing : " + workItem.getNode().getTitle(), expGeneral);
			workflowSession.terminateWorkflow(workItem.getWorkflow());
		}
		logger.debug ("End Workflow Step " + workItem.getNode().getTitle());
		logger.debug (SEPARATOR);
	}

	private boolean checkPayloadResource(String[] folders, String resourcePath) {
		for(String folder: folders) {
			if(resourcePath.contains("/"+folder)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected ResourceResolver getResourceResolver () {
		return resourceResolver;
	}

}
