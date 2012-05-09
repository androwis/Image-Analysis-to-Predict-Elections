<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%
%><%@ taglib prefix="elections" tagdir="/WEB-INF/tags" %><%
%>
<div style="float:left; text-align:center; width:100%; position:absolute; top:5%; background:white; opacity:.90; padding:5% 0">
    <h1 style="font-size:60px; font-height:100px"> State of States.</h1><br/>
      <h3>Predicting Elections Through Image Analysis and Forensics<br/></h3><br/>by <a href="http://androw.is">Andrew Abumoussa</a>
<div class="bs-links">
        <iframe class="github-btn" src="http://markdotto.github.com/github-buttons/github-btn.html?user=androwis&amp;repo=elections&amp;type=fork&amp;count=true" allowtransparency="true" frameborder="0" scrolling="0" width="98px" height="20px"></iframe>
        <iframe allowtransparency="true" frameborder="0" scrolling="no" src="http://platform.twitter.com/widgets/follow_button.1335513764.html#_=1336004362653&amp;id=twitter-widget-2&amp;lang=en&amp;screen_name=androwis&amp;show_count=true&amp;show_screen_name=true&amp;size=m" class="twitter-follow-button" style="width: 236px; height: 20px; " title="Twitter Follow Button"></iframe>
</div>
  
<div id="chart" style="width:100%; float:left;clear:both;text-align:center"></div>
</div>


<script type="text/javascript" src="js/d3.v2.js"></script>
<script type="text/javascript" src="js/choropleth.js"></script>
<link type="text/css" rel="stylesheet" href="css/choropleth.css"/>
<link type="text/css" rel="stylesheet" href="js/lib/colorbrewer/colorbrewer.css"/>

 <section class="photos2">
	<elections:image img="obama-r-0.jpg" alt="real"/>
	<elections:image img="obama-r-1.jpg" alt="real"/>
	<elections:image img="obama-r-2.jpg" alt="real"/>
	<elections:image img="obama-r-3.jpg" alt="real"/>
	<elections:image img="obama-r-4.jpg" alt="real"/>
	<elections:image img="obama-r-5.jpg" alt="real"/>
	<elections:image img="obama-r-6.jpg" alt="real"/>
	<elections:image img="obama-r-7.jpg" alt="real"/>
	<elections:image img="obama-r-8.jpg" alt="real"/>
	<elections:image img="obama-r-9.jpg" alt="real"/>
	
	<elections:image img="romney-c-0.jpg" alt="fake"/>
	<elections:image img="romney-c-1.jpg" alt="fake"/>
	<elections:image img="romney-c-2.jpg" alt="fake"/>
	<elections:image img="romney-c-3.jpg" alt="fake"/>
	<elections:image img="romney-c-4.jpg" alt="fake"/>
	<elections:image img="romney-c-5.jpg" alt="fake"/>
	<elections:image img="romney-c-6.jpg" alt="fake"/>
	<elections:image img="romney-c-7.jpg" alt="fake"/>
	<elections:image img="romney-c-8.jpg" alt="fake"/>
	<elections:image img="romney-c-9.jpg" alt="fake"/>
	
	<elections:image img="obama-c-0.jpg" alt="fake"/>
	<elections:image img="obama-c-1.jpg" alt="fake"/>
	<elections:image img="obama-c-2.jpg" alt="fake"/>
	<elections:image img="obama-c-3.jpg" alt="fake"/>
	<elections:image img="obama-c-4.jpg" alt="fake"/>
	<elections:image img="obama-c-5.jpg" alt="fake"/>
	<elections:image img="obama-c-6.jpg" alt="fake"/>
	<elections:image img="obama-c-7.jpg" alt="fake"/>
	<elections:image img="obama-c-8.jpg" alt="fake"/>
	<elections:image img="obama-c-9.jpg" alt="fake"/>
	
	<elections:image img="romney-r-0.jpg" alt="real"/>
	<elections:image img="romney-r-1.jpg" alt="real"/>
	<elections:image img="romney-r-2.jpg" alt="real"/>
	<elections:image img="romney-r-3.jpg" alt="real"/>
	<elections:image img="romney-r-4.jpg" alt="real"/>
	<elections:image img="romney-r-5.jpg" alt="real"/>
	<elections:image img="romney-r-6.jpg" alt="real"/>
	<elections:image img="romney-r-7.jpg" alt="real"/>
	<elections:image img="romney-r-8.jpg" alt="real"/>
	<elections:image img="romney-r-9.jpg" alt="real"/>
	
	<c:forEach var="i" begin="1" end="41" step="1">
		<elections:image img="data/obamanegative/obama-n-${i}.jpg" alt="real"/>
		<elections:image img="data/obamapostive/obama-p-${i}.jpg" alt="real"/>
		<elections:image img="data/romneypositive/romney-p-${i}.jpg" alt="real"/>
		<elections:image img="data/romneynegative/romney-n-${i}.jpg" alt="real"/>
	</c:forEach>
	<c:forEach var="i" begin="41" end="50" step="1">
		<elections:image img="data/obamanegative/obama-n-${i}.jpg" alt="real"/>
		<elections:image img="data/obamapostive/obama-p-${i}.jpg" alt="real"/>
		<elections:image img="data/romneypositive/romney-p-${i}.jpg" alt="real"/>
	</c:forEach>
	<c:forEach var="i" begin="51" end="63" step="1">
		<elections:image img="data/romneypositive/romney-p-${i}.jpg" alt="real"/>
		<elections:image img="data/obamanegative/obama-n-${i}.jpg" alt="real"/>
	</c:forEach>
</section>