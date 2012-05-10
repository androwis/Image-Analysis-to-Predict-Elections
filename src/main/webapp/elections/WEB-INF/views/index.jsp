<%@ taglib prefix="elections" tagdir="/WEB-INF/tags" %><%

%><!DOCTYPE html><%

%><html><%
	%><elections:head/><%
	%><body><%
		%><elections:toplinks/><%
		%><br/><br/><%
		%><jsp:include page="${include}"/><%
		%><elections:footer year="2012" /><%
		%><jsp:include page="js/ready.jsp"/><%
%></body><%
%></html>