package org.idizital.poc.core.util;

import java.io.InputStream;
import java.util.Set;

import javax.jcr.Binary;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameworkUtil {
	private final static Logger logger = LoggerFactory.getLogger(FrameworkUtil.class);

    public static String getUserName(JSONObject jsonObject) {
    		String userName = "";
		try {
			if (jsonObject != null && jsonObject.has("data")) {
				userName = jsonObject.getJSONObject("data").getString("casa_username");
			}
			else if (jsonObject != null && jsonObject.has("subflow_data")) {
				userName = jsonObject.getJSONObject("subflow_data").getString("casa_username");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		logger.debug ("userName : " + userName);
		return userName;
    }
    
    public static <T> T getService(Class<T> serviceClass) {
        BundleContext bContext = org.osgi.framework.FrameworkUtil.getBundle(serviceClass).getBundleContext();
        ServiceReference sr = bContext.getServiceReference(serviceClass.getName());
        return serviceClass.cast(bContext.getService(sr));
    }
    
    public static String getInstance () {
    	SlingSettingsService slingSettingsService = getService (SlingSettingsService.class);
    	String instance = "";
        if (slingSettingsService.getRunModes().contains ("dev")) {
            instance = "D";
        }
        else if (slingSettingsService.getRunModes().contains ("test")) {
            instance = "T";
        }
        else if (slingSettingsService.getRunModes().contains ("uat")) {
            instance = "U";
        }
        else if (slingSettingsService.getRunModes().contains ("stage")) {
            instance = "S";
        }
        else if (slingSettingsService.getRunModes().contains ("prod")) {
            instance = "P";
        }
        if (slingSettingsService.getRunModes().contains ("publish1")) {
            instance += "-P-01";
        }
        else if (slingSettingsService.getRunModes().contains ("publish2")) {
            instance += "-P-02";
        }
        else if (slingSettingsService.getRunModes().contains ("author")) {
            instance += "-A-00";
        }
        return instance;
    }
    
    public static String getCurrentInstance () {
    	SlingSettingsService slingSettingsService 	= 	getService (SlingSettingsService.class);
    	Set<String> runModes						=	slingSettingsService.getRunModes();
    	String currentInstance 						= 	"";
    	for (String runMode: runModes) {
            if (runMode.startsWith ("publish") && !runMode.equals ("publish")) {
            	currentInstance = runMode;
            }
            else if (runMode.startsWith ("author") && !runMode.equals ("author")) {
            	currentInstance = runMode;
            }
        }
        return currentInstance;
    }
    
    public static boolean isPublishInstance () {
    	SlingSettingsService slingSettingsService = getService (SlingSettingsService.class);
    	if (slingSettingsService.getRunModes().contains ("publish")) {
            return true;
        }
    	return false;
    }
    
    public static boolean isAuthorInstance () {
    	SlingSettingsService slingSettingsService = getService (SlingSettingsService.class);
    	if (slingSettingsService.getRunModes().contains ("author")) {
            return true;
        }
    	return false;
    }

    public static boolean isProdInstance () {
    	SlingSettingsService slingSettingsService = getService (SlingSettingsService.class);
    	if (slingSettingsService.getRunModes().contains ("prod")) {
            return true;
        }
    	return false;
    }
    
    public static boolean isDevInstance () {
    	SlingSettingsService slingSettingsService = getService (SlingSettingsService.class);
    	if (slingSettingsService.getRunModes().contains ("dev")) {
            return true;
        }
    	return false;
    }

    public static void close (InputStream stream) {
    	try {
	    	if (stream != null) {
	    		stream.close();
	    		stream = null;
			}
    	}
    	catch (Exception expGeneral) {}
    }
    
    public static void dispose (Binary binary) {
    	try {
	    	if (binary != null) {
	    		binary.dispose();
	    		binary = null;
			}
    	}
    	catch (Exception expGeneral) {}
    }
    

}