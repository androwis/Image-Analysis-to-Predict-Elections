<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %><%
%><%@ attribute name="name" required="true" type="java.lang.String" %><%
%><%@ attribute name="prefix" required="true" type="java.lang.String" %><%
%><%@ attribute name="prefix_help" required="true" type="java.lang.String" %><%
%><%@ attribute name="class_attribute" required="true" type="java.lang.String" %>
<div class="clearfix">
	<label for="${class_attribute}">${name}</label>
	  <div class="input">
              <div class="input-prepend">
                <span class="add-on">${prefix }</span>
                <input class="medium" id="prependedInput" name="prependedInput" size="16" type="text">
              </div>
              <span class="help-block">${prefix_help}</span>
       </div>
</div>
