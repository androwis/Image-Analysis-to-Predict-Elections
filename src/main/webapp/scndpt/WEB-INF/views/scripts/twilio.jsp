<%@ page contentType="text/xml" %><%
%><%@ page import="scene.tools.TwilioAPI"%><%

%><?xml version='1.0' ?><%
%><Response><%
		try{
			if(request.getParameter("Body")!=null && request.getParameter("Body").contains("yes")){	
				%><Sms>Scenedipity: Great!! we've confirmed your number and saved the subscriptions.  you can stop at any time by responding with "stop".  scndp.it/terms</Sms><%
			}
			else if(request.getParameter("Body")!=null && request.getParameter("Body").contains("stop")){
				%><Sms>Scenedipity: Sorry!! we've completely removed you from our alert system.  scndp.it/terms</Sms><%
			}	
		
		}catch(Exception e){}
%></Response>