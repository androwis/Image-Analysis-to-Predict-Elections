<%@page import="scenedipity.model.User"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%

%><div class="container"><%
%><div class="row"><%
%><div class="span12"><%

// FACEBOOK SERVICES ----------------------------------------------------
String authUrl = "https://facebook.com/dialog/oauth"+
		"?client_id=117564374937567"+
		"&redirect_uri=http://scenedipity.com/oauth/facebook"+
		"&scope=manage_pages,publish_stream,offline_access";

%><div class="oauth_provider" id="facebook"><%
		%><h3><font style="font-weight:bold; color:#222">Scenedipity <=></font><%
		%> <font style="letter-spacing:-1px;font-family:'lucida grande',tahoma,verdana,arial,sans-serif;font-weight:bold;color:#3b5998"> facebook</font></h3><br/><%

		%><c:if test="${empty user.fb_oauth_id}"><%
			%><a class="btn" style="float:none; clear:both;" href="<%=authUrl%>">link <b>scenedipity</b> to <b>facebook</b></a><% 
		%></c:if><c:if test="${not empty user.fb_oauth_id}"><%
			 %><div class="facebookPage well" style="float:none"><%
			   	%><img style="height:100px" src="https://graph.facebook.com/me/picture?access_token=${user.fb_oauth_token}&type=large"/><%
					%><span class="fb_name"></span><%
					%><br/>currently linked facebook account<%
				%><br/><a class="btn btn-info" style="float:none; clear:both;" href="<%=authUrl%>">change connection</a><%
				%></div><%
		 %></c:if><%
%></div><%
%></div><%
%></div><%
%><div class="row"><%
%><div class="span12"><%

//TWITTER SERVICES ---
%><%@ page import="java.io.BufferedReader,java.io.InputStreamReader,java.net.URLEncoder,java.net.*"%><%
%><%@ page import="org.apache.http.HttpResponse,org.apache.http.client.HttpClient,org.apache.http.client.methods.HttpPost,org.apache.http.entity.StringEntity,org.apache.http.impl.client.DefaultHttpClient"%><%
%><%@ page import=" twitter4j.*, twitter4j.auth.*" %><%

//TWITTER SERVICES ---
%><%@ page import=" twitter4j.*" %><%

Twitter twitter = new TwitterFactory().getInstance();
//--- set the consumer info
twitter.setOAuthConsumer("2snlpuNIWEoOFIzFHrTQ", "DGUvu4AOJDDGYgo2ePuDjhZUI5cYFnRlYBmzUO9A");
RequestToken requestToken = twitter.getOAuthRequestToken();
session.setAttribute("token",requestToken);
authUrl=requestToken.getAuthorizationURL();

%><hr/><div class="oauth_provider" id="twttr"><%
	%><h3><font style="font-weight:bold; color:#222">scenedipity <=></font><%
	%><font class="t_name"> twitter</font></h3><br/><%

	%><c:if test="${empty user.twit_oauth_secret}"><%
			
		%><a class="btn" style="float:none; clear:both" href="<%=authUrl%>">link <b>scenedipity</b> to <b>twitter</b>.</a><%
	
	%></c:if><%
%></div><%
%></div><%
%><div class="row"><%
%><div class="span4 offset4"><%

%><hr/><h5><b>Protecting privacy</b>.<small> We strive to do it right. Read our <a href="/legal/privacy">privacy</a> terms.</small></h5><%

%></div><%
%></div>