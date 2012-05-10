<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%
%><%@ attribute name="year" required="true" type="java.lang.String" %><%
%><%@ attribute name="page" required="false" type="java.lang.String" %><%

%><div id="footer"><%
	%><ul><%		
			%><li>&copy; ${year} Andrew Abumoussa &amp; Scenedipity</li><%
			//><li><a> art </a></li><%
			//><li><a> business </a></li><%
		%></ul><%
%></div><%

// google analytics
%><script type="text/javascript">

  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-22485363-3']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();

</script><%

%><jsp:include page="/WEB-INF/forms/login_form.jspf"/>