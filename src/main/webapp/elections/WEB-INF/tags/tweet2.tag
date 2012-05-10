<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ attribute name="tweet" required="true" type="scenedipity.model.Tweet"%>
<% String temp = tweet.getFrom_user_name(); %>
<option value="${tweet.from_user_name}"
<% if(request.getParameter("name")!=null&&request.getParameter("name").equals(temp)){ %>selected = 'selected'<% }%>
>${tweet.from_user_name}</option>




