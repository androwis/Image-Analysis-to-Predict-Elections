<%@page import="scenedipity.model.User"%>
<%@page import="java.util.HashMap,java.util.Iterator"%>
<%@page import="java.net.URL,java.net.URLEncoder,java.net.URLConnection"%>
<%@page import="java.io.BufferedReader,java.io.IOException,java.io.InputStreamReader,java.io.OutputStreamWriter"%>
<%@page import="org.json.JSONException, org.json.JSONObject, org.json.JSONArray"%>

<%! //here are the functions used to access facebook's api

// --- This function converts a string to a json object
public static JSONObject stringToJSON(String data) throws IOException, JSONException {
	JSONObject json = new JSONObject(data);
   return json;
  }

// --- This functions read's the returned value from a given URL
private String readUrl(String urlString){
	try{
    URL url = new URL(urlString);
    BufferedReader in = new BufferedReader(
    		new InputStreamReader(
    			url.openStream()
  			)
		);
 
    String response = "";
    String inputLine;
    while ((inputLine = in.readLine()) != null)
        response += inputLine;
 
    in.close();
    return response;
	}catch(Exception e){ return e.toString();}
}

// --- this executes a post command to write info to facebook
private String post(String urlString, String message, String token){
	    String line="";
	try {
	    // Construct data
	    String data = URLEncoder.encode("message", "UTF-8") + "=" + URLEncoder.encode(message, "UTF-8");
	    data += "&" + URLEncoder.encode("access_token", "UTF-8") + "=" + URLEncoder.encode(token, "UTF-8");

	    // Send data
	    URL url = new URL(urlString);
	    URLConnection conn = url.openConnection();
	    conn.setDoOutput(true);
	    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
	    wr.write(data);
	    wr.flush();

	    // Get the response
	    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    while ((line = rd.readLine()) != null) {
	    }
	    wr.close();
	    rd.close();
	} catch (Exception e) {
	}
	    return line;
}

// end functions to access facebook's api
%><%

// --- check to see if they're logged into a valid account
User user = (User) session.getAttribute("user");
boolean isLoggedIn = user!=null;
// --- application sp. parameters
String apiKey = "117564374937567";
String apiSecret="e76a677786699cb860963bd806aec4ae";

// --- initiate facebook's handshake
if(request.getParameter("code")==null || request.getParameter("code").length()<1){
	String url="https://graph.facebook.com/oauth/authorize?"+
		   "scope=manage_pages"+
		   "&client_id="+apiKey+
		   "&redirect_uri=http://scenedipity.com/oauth/facebook";
	%><div class="message red" style="color:#333; margin-bottom:150px">sorry, it seems as if weren't logged into facebook.  please login <a href="<%=url%>" style="font-size:inherit">here</a> to connect scenedipity to facebook.</div><%
}else{

	String accessToken = readUrl("https://graph.facebook.com/oauth/access_token?"
    		+"client_id="+apiKey
    		+"&client_secret="+apiSecret 
    		+"&code="+request.getParameter("code")+
    		"&redirect_uri=http://scenedipity.com/oauth/facebook")
    		.split("&")[0].replaceFirst("access_token=", "");
   request.setAttribute("accessToken", accessToken);
   
   
   // --- request.getParameter("code")
   // --- accessToken
   if(isLoggedIn){
	   %><span class = "span12 alert alert-success">Choose the <b>facebook</b> page to which you'd like your information published: </span><%
	    }else{
   	%><span class="span 12 alert alert-error">Login to <b>scenedipity</b> above to link your accounts.</span><%
   }
   
   // retrieve data for logged in user.
   JSONObject me = stringToJSON(
			readUrl("https://graph.facebook.com/me?access_token="+accessToken)
	);  
   %><div class="container"><%
   %><div class="row span11"><%
   %><div class="facebookpage well span3"><%
   	%><img src="https://graph.facebook.com/me/picture?access_token=<%=accessToken%>&type=large"/><%
		%><div class="check"><%
	   	%><input type="radio" id="<%=me.get("id")%>" name="fbpage" value="<%=accessToken%>" /><%
	   	%>link to scenedipity<%
		%></div><%
		%><span class="fb_name"><%
			%><%=me.get("first_name")%> <%=me.get("last_name")%><%
		%></span><%
		%><br/>personal wall<%
	%></div><%
   // see what pages this user can write to.
    JSONObject account = stringToJSON(
			readUrl("https://graph.facebook.com/me/accounts?access_token="+accessToken)
		); 
    JSONArray accounts = account.getJSONArray("data");
    
    for(int i=0; i<accounts.length();i++){
    	JSONObject theAccount=accounts.getJSONObject(i);
    	%><div class="facebookpage well span3"><%
   		%><img src="https://graph.facebook.com/<%=theAccount.getString("id")%>/picture?type=large"/><%
	  %><div class="check"><%
	   	%><input type="radio" id="<%=theAccount.getString("id")%>" name="fbpage" value="<%=theAccount.getString("access_token")%>" /><%
	   	%>link to scenedipity<%
		%></div><%
     %><span class="fb_name"><%
     %><%=theAccount.getString("name")%><%
     %></span><br/> facebook page<%
     %></div><%
  	  %><%//theAccount.getString("access_token")%><%
    }
    
   %></div><%// end row.
   %></div><%// end container
   %><div class="facebookPage"><%
    
    if(isLoggedIn){
    	%><div style="width:100%; float:left; clear:both; text-align:center; margin-bottom:200px"><a class="btn" id="save" type="button">link facebook &amp; scenedipity!</a></div><%
    }
  	  %><%//post("https://graph.facebook.com/"+theAccount.getString("id")+"/feed", "hi this is a test from scenedipity!", theAccount.getString("access_token")) %><%
    
}%>