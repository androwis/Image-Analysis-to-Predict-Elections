<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %><%
%><%@ attribute name="name" required="true" type="java.lang.String" %><%
%><%@ attribute name="class_attribute" required="true" type="java.lang.String" %>
<div class="clearfix">
	<label for="${class_attribute}">${name}</label>
	<form:input path="${class_attribute}"/>
</div>