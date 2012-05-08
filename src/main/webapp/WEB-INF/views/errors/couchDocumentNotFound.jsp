<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="scenedipity" tagdir="/WEB-INF/tags" %>
<%@ page import="java.io.*,java.util.*" %>
<!DOCTYPE html>
<html>
<scenedipity:head title="scenedipity Web Client" />
	
<body>
	<scenedipity:toplinks who="no bueno" />
		<div class="container">
		<br/>
			<div class="alert-message error">
  				<p>
  				<strong>Error Page.  Like Biggie with too much stink.</strong>
			 	[couchDB not found] [<%=request.getPathInfo() %>]
				</p>
			</div>
			<div class="alert-message warning">
			
				<%
				 // Print the request headers
   Enumeration enames;
   Map map;
   String title;

   map = new TreeMap();
   enames = request.getHeaderNames();
   while (enames.hasMoreElements()) {
      String name = (String) enames.nextElement();
      String value = request.getHeader(name);
      map.put(name, value);
   }
   out.println(createTable(map, "Request Headers"));
   
   
   
     // Print the session attributes

   map = new TreeMap();
   enames = session.getAttributeNames();
   while (enames.hasMoreElements()) {
      String name = (String) enames.nextElement();
      String value = "" + session.getAttribute(name);
      map.put(name, value);
   }
   out.println(createTable(map, "Session Attributes"));
   %>
			</div>
			<scenedipity:footer year="2011" />
		</div>
</body>
</html>

<%!
   private static String createTable(Map map, String title)
   {
      StringBuffer sb = new StringBuffer();

      // Generate the header lines

      sb.append("<table border='1' cellpadding='3'>");
      sb.append("<tr>");
      sb.append("<th colspan='2'>");
      sb.append(title);
      sb.append("</th>");
      sb.append("</tr>");

      // Generate the table rows

      Iterator imap = map.entrySet().iterator();
      while (imap.hasNext()) {
         Map.Entry entry = (Map.Entry) imap.next();
         String key = (String) entry.getKey();
         String value = (String) entry.getValue();
         sb.append("<tr>");
         sb.append("<td>");
         sb.append(key);
         sb.append("</td>");
         sb.append("<td>");
         sb.append(value);
         sb.append("</td>");
         sb.append("</tr>");
      }

      // Generate the footer lines

      sb.append("</table><p></p>");

      // Return the generated HTML

      return sb.toString();
   }
%>