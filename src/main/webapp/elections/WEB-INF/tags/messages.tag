<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%

//process messages
String message = (String)request.getAttribute("message");
String messageType = (String)request.getAttribute("message-type");

// Check for messages/errors
if(message!=null){%>
	<br/><div class="alert-message block-message message red <%=messageType%>">
	<p><%=message%></p>
	</div>
<%}%>