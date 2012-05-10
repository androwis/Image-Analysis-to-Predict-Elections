<%@ page import="java.io.BufferedReader,java.io.InputStreamReader,java.net.URLEncoder,java.net.*"%><%
%><%@ page import="org.apache.http.HttpResponse,org.apache.http.client.HttpClient,org.apache.http.client.methods.HttpPost,org.apache.http.entity.StringEntity,org.apache.http.impl.client.DefaultHttpClient"%><%
%><%@ page import=" twitter4j.*, twitter4j.auth.*" %><%

try{
	// --- retrieve the consumer and provider from the session
	Twitter twitter = new TwitterFactory().getInstance();

	//--- set the consumer info
	twitter.setOAuthConsumer("2snlpuNIWEoOFIzFHrTQ", "DGUvu4AOJDDGYgo2ePuDjhZUI5cYFnRlYBmzUO9A");
	RequestToken requestToken = (RequestToken) session.getAttribute("token");
	session.removeAttribute("token");
	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

	// --- retrieve bits
	String pin=request.getParameter("oauth_verifier");
	try{
		// --- check consumer token & secret
		AccessToken accessToken = twitter.getOAuthAccessToken(requestToken,pin);

		// --- give a visual confirmation to the user
		%><div class="facebookPage"><%
		  	%><img src="<%=twitter.getProfileImage(twitter.getScreenName(), ProfileImage.BIGGER).getURL()%>"/><%
			%><span class="fb_name"><%
				%><%=twitter.getScreenName()%><%
			%></span><%
			%><br/>twitter account<%
		%></div><%

		%><br/><%

		%><div><%
			%><span id="u"><%=twitter.getScreenName()%></span>, click the button below to save your info.<br/><%
			%><span id="t" style="display:none"><%=accessToken.getToken()%></span><%
			%><span id="s" style="display:none"><%=accessToken.getTokenSecret()%></span><%
		%></div><%

		// --- navigation buttons
		%><div style="width:100%; float:left; clear:both; text-align:center; margin-bottom:200px"><%

			// --- functionality for this button in 1stPartyJS.jsp (javascript)
			%><button class="btn" id="save" type="button">link twitter &amp; scenedipity!</button><%

			// --- clicking this doesn't save anything
			%><a class="btn" href="http://scenedipity.com/oauth">cancel</a><%

		%></div><%

	}catch(Exception e){%><br/><br/><%=e.toString()%><%}
}catch(Exception e){%><br/><br/><%=e.toString()%><%}// oauth exceptions%>