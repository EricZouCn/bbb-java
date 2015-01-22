package org.mconf.bbb.api;

import java.io.ByteArrayInputStream;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JoinService0Dot9 extends JoinServiceBase {
	private static final Logger log = LoggerFactory.getLogger(JoinService0Dot9.class);

	private String salt;
	private long timestamp = 0,
			lastRequest = 0;

	public void setSalt(String salt) {
		if (!salt.equals(this.salt)) {
			this.salt = salt;
			// if the salt is changed, we need to reset the timestamp stuff
			this.timestamp = 0;
			this.lastRequest = 0;
		}
	}
	
	public String getSalt() {
		return this.salt;
	}
	
	@Override
	protected String getCreateMeetingUrl(String meetingID) {
		String parameters = "meetingID=" + urlEncode(meetingID);
		return "create?" + parameters + "&checksum=" + checksum("create" + parameters + salt);
	}
	
	@Override
	public int createMeeting(String meetingID) { //.
		int code = updateTimestamp();
		if (code != E_OK)
			return code;

		return super.createMeeting(meetingID);
	}

	@Override
	protected String getDemoPath() {
//		return "/demo/mobile.jsp";
		return "/bigbluebutton/api/";
	}

	@Override
	protected String getJoinUrl(Meeting meeting, String name, boolean moderator) {
		String parameters = "meetingID=" + urlEncode(meeting.getMeetingID())
			+ "&fullName=" + urlEncode(name)
			+ "&redirectClient=false"
			+ "&password=" + urlEncode(moderator? meeting.getModeratorPW(): meeting.getAttendeePW());
		return "join?" + parameters + "&checksum=" + checksum("join" + parameters + salt);
	}
	
	@Override
	public int join(String meetingID, String name, boolean moderator) { //.
		int code = updateTimestamp();
		if (code != E_OK)
			return code;
		
		return super.join(meetingID, name, moderator);
	}

	@Override
	protected String getLoadUrl() {
		String parameters = "random=" + new Random().nextInt(9999);
		return "getMeetings?" + parameters + "&checksum=" + checksum("getMeetings" + parameters + salt);
	}
	
	@Override
	public int load() { //.
//		int code = updateTimestamp();
//		if (code != E_OK)
//			return code;
//		
//		return super.load();

		String loadUrl = getFullDemoPath() + getLoadUrl();
		log.debug("getMeetings URL: " + loadUrl);
		
		int returnCode;
		loaded = false;
		try {
			returnCode = meetings.parse(getUrl(loadUrl)); 
		} catch (Exception e) {
			e.printStackTrace();
			log.info("Can't connect to {}", loadUrl);
			return E_SERVER_UNREACHABLE;
		}
		
		if(returnCode == E_OK) {
			log.debug(meetings.toString());
			loaded = true;
		}
		
		return returnCode;

	}

	private int parseTimestamp(String str) { //.
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new ByteArrayInputStream(str.getBytes("UTF-8")));
			doc.getDocumentElement().normalize();
			Element nodeResponse = (Element) doc.getElementsByTagName("response").item(0);
			String returncode = nodeResponse.getElementsByTagName("returncode").item(0).getFirstChild().getNodeValue();

			if (returncode.equals("SUCCESS")) {	
				timestamp = Long.parseLong(nodeResponse.getElementsByTagName("timestamp").item(0).getFirstChild().getNodeValue());
				return E_OK;
			}
			else
			{
				log.debug("Failed getting the timestamp");
				log.debug("Start parsing the message key");
				String messageKey = nodeResponse.getElementsByTagName("messageKey").item(0).getFirstChild().getNodeValue();
				int errorCode = getErrorCode(messageKey);
				log.debug("{}",errorCode);
				log.debug(str);
				
				return errorCode;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.warn("Failed to parse: {}", str);
			return E_INVALID_TIMESTAMP; //que constante?
		}
	}

	private int getTimestamp() {	//.
		String parameters = "action=getTimestamp";
		String timestampUrl = getFullDemoPath() + "?" + parameters + "&checksum=" + checksum(parameters + salt);

		log.debug("getTimestamp URL: " + timestampUrl);
		String response = "Unknown error";
		try {
			response = getUrl(timestampUrl);
			log.debug("getTimestamp response: " + response);
			return parseTimestamp(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Can't get the timestamp from {}", serverUrl);
			timestamp = 0;
			return E_INVALID_TIMESTAMP; //que constante?
		}
	}
	
	private int updateTimestamp() { //.
//		int code;
//		if (System.currentTimeMillis() < lastRequest + 55000)
//			return E_OK;
//		else {
//			code = getTimestamp();
//			if (code == E_OK) {  
//				lastRequest = System.currentTimeMillis();
//				return E_OK;
//			} else {
//				log.error("Invalid security key");
//			}
//		}
//		return code;
		return E_OK;
	}

	@Override
	public String getVersion() {
		return "0.9";
	}
	
	private int getErrorCode(String message)
	{
	    if(message.equals("checksumError"))
	    	return E_INVALID_CHECKSUM; //could return E_INVALID_CHECKSUM;
	    
	    if(message.equals("invalidTimestamp"))
	    	return E_INVALID_TIMESTAMP;
	    
	    if(message.equals("emptySecurityKey"))
	    	return E_EMPTY_SECURITY_KEY;
	    
	    if(message.equals("missingParamMeetingID"))
	    	return  E_MISSING_PARAM_MEETINGID;
	    
	    if(message.equals("missingParamFullName"))
	    	return  E_MISSING_PARAM_FULLNAME;
	    
	    if(message.equals("invalidPassword"))
	    	return E_MISSING_PARAM_PASSWORD;
	    
	    if(message.equals("missingParamTimestamp"))
	    	return E_MISSING_PARAM_TIMESTAMP;
	    
	    if(message.equals("invalidAction"))
	    	return E_INVALID_URL;
	    
	    else
	    	return E_UNKNOWN_ERROR; 	
	}

}
