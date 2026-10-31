package com.knot.provisioning.utill;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

/**
 * @author Sai D
 *
 */

public class FileUtil {

	private static final Logger logger = LogManager.getLogger(FileUtil.class);
	
	static List<String> listOfValues = new ArrayList<>();

	static Map<String, String> finalResponseMap = new HashMap<>();

	public FileUtil() {

	}

	public static String getInQueryValue(String value) {
		String result = "'0'";
		try {
			if (value != null && !value.isEmpty()) {
				String[] array = value.split(",");
				StringBuilder builder = new StringBuilder();
				int count = 0;
				for (String val : array) {
					count++;
					if (count != array.length) {
						builder.append(val).append(",");
					} else {
						builder.append(val);
					}
				}
				result = builder.toString();
			}
		} catch (Exception e) {
			logger.info("The Exception is FileUtil :: getInQueryValue ", e);
		}
		return result;
	}

	public static String getFeatureCodes(String[] featureCodes) {
		StringBuilder builder = new StringBuilder();
		try {
			for (String featureCode : featureCodes) {
				if (builder.length() > 0) {
					builder.append(',');
				}
				builder.append(featureCode);
			}
		} catch (Exception e) {
			logger.info("The Exception is FileUtil :: getFeatureCodes ", e);
		}
		return builder.toString();
	}

	public static String getDateFormatter() {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		String formattedDateTime = now.format(formatter);
		return formattedDateTime;
	}

	public String getFormatName(String methodName, String tenantId) {
        String tntCustomerId = null;

        if (StringUtils.hasText(methodName)) {
            try {
                Method method = this.getClass().getDeclaredMethod(methodName, String.class);
                tntCustomerId = (String) method.invoke(this, tenantId); // Pass 'this' as the first argument
            } catch (NoSuchMethodException e) {
                logger.error("Method not found: {}", methodName, e);
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error("Error invoking method: {}", methodName, e);
            }
        }
        
        return tntCustomerId; // Will return null if method invocation fails or if method returns null
    }

    public String getTenantUniqueCode(String tenantID) {
        StringBuilder concatVal = new StringBuilder();
        try {
            String format = "AAA_BBB_CCC"; // configObj.getStrValue(Constants.CC_FORMAT, tenantID);
            String[] formatSplitVal = format.split("_");
            for (String formatPart : formatSplitVal) {
            	concatVal.append(formatPart).append(ThreadLocalRandom.current().nextInt(10, 100));
            }
        } catch (Exception e) {
            logger.error("The Exception in CustomerServiceImpl : getTenantUniqueCode()", e);
        }
        return concatVal.toString(); 
    }
    
    public static Map<String, String> convertResponseJsonToMap(String responseString, String keyObj) throws Exception {
  		JSONObject jsonObject = new JSONObject(responseString);
  		new HashMap<String, Object>();
  		if (jsonObject != JSONObject.NULL) {
  			toMap(jsonObject,keyObj);
  		}
  		return finalResponseMap;

  	}
    public static Map<String, String> ResponseJsonToMap(String responseString, String keyObj) throws Exception {
  		JSONObject jsonObject = new JSONObject(responseString);
  		new HashMap<String, Object>();
  		if (jsonObject != JSONObject.NULL) {
  			toMapObj(jsonObject,keyObj);
  		}
  		return finalResponseMap;

  	}

    public static Map<String, Object> toMap(JSONObject object,String keyObj) throws Exception {
		Map<String, Object> map = new HashMap<>();

		Iterator<String> keysItr = object.keys();
		while (keysItr.hasNext()) {
			String key = keysItr.next();
			Object value = object.get(key);
			if (key.equalsIgnoreCase(keyObj)) {
				finalResponseMap.put(key.trim(), value.toString().trim());
			}
			else if (value instanceof JSONArray) {
				value = toList((JSONArray) value, key);
			}

			else if (value instanceof JSONObject) {
				value = toMap((JSONObject) value,null);
			} else {
				listOfValues.add(value.toString().trim());
				if (finalResponseMap.containsKey(key)) {
					finalResponseMap.put(key.trim(), finalResponseMap.get(key) + "," + value.toString().trim());
				} else {
					finalResponseMap.put(key.trim(), value.toString().trim());
				}
			}

			map.put(key, value);
		}
		return map;
	}

	public static List<Object> toList(JSONArray array, String key) throws Exception {
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			Object value = array.get(i);
			if (value instanceof JSONArray) {
				value = toList((JSONArray) value, key);
			} else if (value instanceof JSONObject) {
				value = toMap((JSONObject) value,null);
			} else {

				listOfValues.add(value.toString().trim());
				if (finalResponseMap.containsKey(key)) {
					finalResponseMap.put(key.trim(), finalResponseMap.get(key) + "," + value.toString().trim());
				} else {
					finalResponseMap.put(key.trim(), value.toString().trim());
				}

			}
			list.add(value);
		}
		return list;
	}

	public static List<String> foundResponseReplaceValues(String jsonString) {
		List<String> oValues = new ArrayList<>();
		Pattern pattern = Pattern.compile("O\\|([\\w-]+)");
		Matcher matcher = pattern.matcher(jsonString);
		while (matcher.find()) {
			oValues.add(matcher.group(1));
		}
		return oValues;
	}
	
	public static Map<String, Object> toMapObj(JSONObject object, String keyObj) throws Exception {
	    Map<String, Object> map = new HashMap<>();
	    Map<String, Set<String>> uniqueValuesMap = new HashMap<>();
	    Iterator<String> keysItr = object.keys();
	    while (keysItr.hasNext()) {
	        String key = keysItr.next();
	        Object value = object.get(key);
	        
	        if (key.equalsIgnoreCase(keyObj)) {
	            finalResponseMap.put(key.trim(), value.toString().trim());
	        } else if (value instanceof JSONArray) {
	            value = toList((JSONArray) value, key);
	        } else if (value instanceof JSONObject) {
	            value = toMap((JSONObject) value, null);
	        } else {
	            listOfValues.add(value.toString().trim());
	            uniqueValuesMap.computeIfAbsent(key.trim(), k -> new HashSet<>()).add(value.toString().trim());
	        }
	        
	        map.put(key, value);
	    }
	    for (Map.Entry<String, Set<String>> entry : uniqueValuesMap.entrySet()) {
	        finalResponseMap.put(entry.getKey(), String.join(",", entry.getValue()));
	    }

	    return map;
	}


}
