<%-- 
				SCNDP.IT
				by: Andrew AbuMoussa 
				best practices : http://www.precisejava.com/javaperf/j2ee/JSP.htm

--- http://tantek.pbworks.com/w/page/21743973/Whistle ---
a - audio recording, speech, talk, session, sound 
b - blog post, article (structured, with headings), essay: http://activitystrea.ms/schema/1.0/article 
c - code, sample code, library, open source, code example
d - diff, edit, change
e - event - hCalendar
f - favorited - primarily just a URL, often to someone else's content. for more, see 'r' below 
g - geolocation, location, checkin, venue checkin, dodgeball, foursquare
h - hyperlink - e(x)ternal reference, link, etc. use of short URL to link to things that I expect to die or move, untrustworthy permalinks. 
i - identifier - on another system using subdirectory as system id space
	i/i/ - ISBN (compressed via NewBase60)
	i/a/ - ASIN (compressed via NewBase60)
j - reserved
k - reserved
l . (skipping due to resemblance to 1, per print-safety design principle, related: ShortURLPrintExample)
m - (message like email, permalink to external list archive, or private blog archive, or a sender-hosted message)
n - reserved
o - physical objects (e.g. stuff from Amazon, or URLs attached to actual specific physical objects) 
p - photo (re-using Flickr's design choice of flic.kr/p/ for photo short URLs)
q - reserved
r - review, recommendation, comment regarding/response/rebuttal - hReview/xfolk
s - slides, session presentation, S5 
t - text, (plain) text, tweet, thought, note, unstructured, untitled 
http://activitystrea.ms/schema/1.0/note 
u - (update, could be used for status updates of various types, profile updates)
v - video recording 
w - work, work in progress, wiki, project, draft, task list, to-do, do, gtd
x - XMDP Profile 
y - reserved
z - reserved 
 --%><%
 
%><%@ page import="java.util.*"%><%

String url = "";
String id = "";
// --- begin translation---------------------------------------------------------------------------------------
if(url.length()>0){

	%><%! // --- Functions ---------------------------------------------------v
	public boolean check(String s, String id){
		try{ return s.equalsIgnoreCase(id);}catch(Exception e){return false;}
	}

	public final String S = "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";

	public String expand(String toExpand){
		long i=0;
		long mult =1;
		for(int j=0; j<toExpand.length(); j++){
			i+=mult*S.indexOf(toExpand.charAt(toExpand.length()-j-1));
			mult=mult*S.length();
		}
		return i+"";
	}

	public String encode(long toEncode){
		int base = S.length();
		String encoded = "";
		while (toEncode >= base) {
		double div = (double)toEncode/(double)base;
		int mod = (int) (toEncode-(base*Math.floor(div)));
		encoded= S.charAt(mod)+encoded;
		toEncode = (long)Math.floor(div);
	}
		encoded=S.charAt((int)toEncode)+encoded;
		return encoded;
	}

	public long calculateDays(){
	    long days = System.currentTimeMillis()/1000/60/60/24;
       return days;
	}

	public String translate(long days){

		Calendar temp2 = Calendar.getInstance();
		temp2.clear();
		temp2.add(Calendar.DAY_OF_YEAR,(int)(days));
		Date temp = temp2.getTime();
		String toReturn="";
		if(temp.getMonth()+1<10) toReturn="0";
		toReturn+=(temp.getMonth()+1)+"-";		
		if(temp.getDate()<10)toReturn+="0";
		return toReturn + temp.getDate()+"-"+(temp.getYear()-100);		
			}
	%><%//--------------------------------------------------------------------^

	response.setStatus(301);
	if(check(url, "foodtrucks") || check(url,"streets")){
		response.setHeader( "Location", "http://www.scenedipity.com/foodtrucks" );
	}else if(check(url, "g")){
		String urlEnd = "where-is/"
			+expand(id.substring(3,6))+
		"/"+translate(
			Long.parseLong(
				expand(
					id.substring(0,3)
				)
			)
		);
		response.setHeader( "Location", "http://www.scenedipity.com/"+urlEnd);	
	}else if(check(url,"b")){
		response.setHeader( "Location", "http://blog.scenedipity.com/?p="+id);
	}else{
		response.setHeader( "Location", "http://www.scenedipity.com/"+url);
	}
	response.setHeader( "Connection", "close" );

// --- display splash page -----------------------------------------------------------------------------------------------
}else{
	%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"><%
		%><html xmlns="http://www.w3.org/1999/xhtml"><%
			%><head><%
				%><script type="text/javascript"><%
	 				 %>var _gaq = _gaq || [];<%
	 				 %>_gaq.push(['_setAccount', 'UA-22485363-2']);<%
	 				 %>(function() {<%
	 					 %>var ga = document.createElement('script');<%
	 					 %>ga.type = 'text/javascript'; ga.async = true;<%
	 					 %>ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';<%
	 					 %>var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);<%
					 %>})();<%

					 %>function forward(url){<%
						 %>_gaq.push(['_trackPageview', url]);<%
						 %>window.location = "http://www.scenedipity.com"+url;<%
					 %>}<%

					 %>function forwardBlog(url){<%
						 %>_gaq.push(['_trackPageview', "/blog"+url]);<%
						 %>window.location = "http://blog.scenedipity.com"+url;<%
					 %>}<%
	 	%></script><%

%></head><%
%><body><%
 	%><link href="http://scenedipity.com/css/bootstrap.css" rel="stylesheet"><%
 	%><link href="http://scenedipity.com/css/docs.css" rel="stylesheet"><%
 	%><link href="http://scenedipity.com/css/bootstrap-responsive.css" rel="stylesheet"><%
 	%><link href="http://scenedipity.com/css/style.css" rel="stylesheet" type="text/css" /><%
	%><title>scenedipity's url shortener</title><%
%></head><%

%><body><%

	%><div class="navbar navbar-fixed-top">
		<div class="navbar-inner" style="margin-left:0;height: 24px;border-top:solid 3px #0FA6C5">
		<div class="container">
		<div class="span2"><a href="/" class="brand" style="color:white; font-size:xx-large">scen<span style="color:#0fa6c5">e</span>dipity</a></div>
		</div></div></div></div><%
 	
	 %><span class="message yellow"><%
	 	%><font style="font-weight:bold">coming soon:</font><%
	 	%> we plan on publishing an api, exposing our translation tables &amp; releasing <%
	 	%><a href="http://scndp.it" alt="Scenedipity's URL shortener" style="font-size:inherit">scndp.it</a><%
	 	%> for public use... <br/> <%
 	%></span><%

 	%><p><%
 		%>Scenedipity provides an algorithmic <%
 		%><a href="http://en.wikipedia.org/wiki/URL_shortening">url shortening</a><%
 		%> service for checkins, menus, and profiles.<%
 		%> Short urls can be useful in a variety of contexts including: email, on business cards, IM, text messages, or short status updates.<%
	%></p><%

	%><p><%
		%>Menus have a direct short url of the form:<%
	%></p><%

	%><pre>http://scndp.it/{restaurant-username}</pre><%

 	%><p><%
		%>Our blog entries have a short url of the form:<%
	%></p><%

	%><pre>http://scndp.it/b/{blog-entry-id}</pre><%

	%><p>Checkins are a little trickier.  The general form always begins with : </p><%

	%><pre> http://scndp.it/g <br/> </pre><%

	long epocDays=calculateDays();
	String cEpocDays=encode(epocDays);
	String date = translate(epocDays);
	String date0 = translate(epocDays-7);

	%><p>More importantly, these links have a mathematically calculated short url of the forms:</p><%

	%><pre><%
		%>http://scndp.it/g/{user-id} <br/><%
		%><font color="blue">http://scenedipity.com/streets/username</font><%
	%></pre><%

	%><p>This form returns all of the checkins for a given day:</p><%

	%><pre><%
		%>http://scndp.it/g/{checkin-date} <br/><%
		%>http://scndp.it/g/</font><font color="blue"><%=cEpocDays %></font><%
		%><br/><font color="#777"> resolves to </font><br/><%
		%>http://scenedipity.com/streets/<font color="blue"><%=date%></font><%
	%></pre><%

	%><p>This form returns all of the checkins for a given day:</p><%
		%><pre><%
			%>http://scndp.it/g-{checkin-date}{user-id}<br/><%
			%><font color="blue">http://scndp.it/streets/username/<%=date%> </font><br/><br/><%

			%>http://scndp.it/g-{checkin-date}{checkout-date}<br/><%
			%><font color="blue">http://scndp.it/streets/<%=date0%>/<%=date%> </font><br/><br/><%

			%>current number of days since epoch: <font color="red"><%=epocDays%></font><%
			%><br/>Base58 representation  of current number of days since epoch: <font color="red"><%=cEpocDays%></font><%				
		%></pre><%
	%></p><%		

	%><p><%
		%><font color="red">* </font>Base58 is used to compress the checkin-date and the checkin-times using a mix of letters and numbers.<%
	%></p><%

	%><div class="span12 footer"><%
		%><ul><%
		%><li>&copy; 2012 Scenedipity</li><%
		%><li><a href="/about-us" rel="me"> about </a></li><%
		%><li><a href="http://blog.scenedipity.com" rel="me">blog</a></li><%
		%><li><a href="http://data.scenedipity.com" rel="me"> developers </a></li><%
		%><li><a href="http://github.scenedipity.com" rel="me"> open-source </a></li><%
		%><li><a href="/legal/privacy" rel="me"> privacy</a></li><%
		%><li><a href="/services" rel="me"> services</a></li><%
		%><li><a href="/" rel="me"> scndp.it </a></li><%
		%><li><a href="/legal/terms-of-service" rel="me"> terms </a></li><%
		%></ul><%
	%></div><%

%></body><%
%></html><%
}%>