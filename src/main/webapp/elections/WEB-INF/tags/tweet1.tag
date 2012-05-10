<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ attribute name="tweet" required="true" type="scenedipity.model.Tweet"%>
<%
String temp = tweet.getFrom_user_name();

%><div	class='tweet' health = '${tweet.health}' lat='${tweet.geo.coordinates[0]}' lng='${tweet.geo.coordinates[1]}'<% 

if(request.getParameter("name").equals(temp)){ %>core = 'yes'<% }else{ %> core='no'<%}%>

		style="display:none;width:31%; margin:1%; float:left" ><%

%><div class="container">
	<div class="snippet">
		<a href="/dev1?name=${tweet.from_user_name}" class="map-link"
				alt="show ${tweet.from_user_name} on the map">
				<img class='photo' src='${tweet.profile_image_url}' />
		</a>

			<a	href="/dev1?name=${tweet.from_user_name}" class="map-link"
				alt="show  ${tweet.from_user_name} on the map">
				<span class="fn org"> ${tweet.from_user_name} </span>
				<font style="color:red; font-size:large">[${tweet.health }]</font>
			</a>
 	 		<div class='announcement'>${tweet.text}</div>
		</div><%// --- end snippet

%></div><% // --- end of the container div
					
%></div><% // --- end of the foodtruck div%>
