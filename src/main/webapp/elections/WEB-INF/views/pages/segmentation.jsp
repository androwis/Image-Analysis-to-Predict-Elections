<%@page contentType="text/html;charset=UTF-8" %><%
%><%@page pageEncoding="UTF-8" %><%
%><%@ page session="false" %><%
%><%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %><%
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%
%><%@ taglib prefix="elections" tagdir="/WEB-INF/tags" %><%

%><div>
     <section class="photos">
     	<img id="origin" src="http://placehold.it/260x180"/>
     	<img id="0" src="http://placehold.it/260x180"/>
     	<img id="5" src="http://placehold.it/260x180"/>
     	<img id="10" src="http://placehold.it/260x180"/>
     	<img id="15" src="http://placehold.it/260x180"/>
     	<img id="20" src="http://placehold.it/260x180"/>
     </section>
     	<br/>
     		<div class="well" style="float:left; clear:both; width:100%">
	<form:form modelAttribute="uploadItem" method="post" enctype="multipart/form-data">
            <fieldset>
            	<form:label for="fileData" path="fileData"></form:label>
                    <form:input path="fileData" type="file"/>
                    <input class="btn" type="submit" />
            </fieldset>
        </form:form>
        </div>
     	
     </div>
        
     <section class="photos">
    	<elections:image-segmentation img="obama-r-0.jpg" alt="real"/>
    	<elections:image-segmentation img="obama-r-1.jpg" alt="real"/>
    	<elections:image-segmentation img="obama-r-2.jpg" alt="real"/>
    	<elections:image-segmentation img="obama-r-3.jpg" alt="real"/>
    	<elections:image-segmentation img="obama-r-4.jpg" alt="real"/>
    	<elections:image-segmentation img="obama-r-5.jpg" alt="real"/>
    	<elections:image-segmentation img="obama-r-6.jpg" alt="real"/>
    	<elections:image-segmentation img="obama-r-7.jpg" alt="real"/>
    	<elections:image-segmentation img="obama-r-8.jpg" alt="real"/>
    	<elections:image-segmentation img="obama-r-9.jpg" alt="real"/>
    	
	   	<elections:image-segmentation img="romney-c-0.jpg" alt="fake"/>
    	<elections:image-segmentation img="romney-c-1.jpg" alt="fake"/>
    	<elections:image-segmentation img="romney-c-2.jpg" alt="fake"/>
    	<elections:image-segmentation img="romney-c-3.jpg" alt="fake"/>
    	<elections:image-segmentation img="romney-c-4.jpg" alt="fake"/>
    	<elections:image-segmentation img="romney-c-5.jpg" alt="fake"/>
    	<elections:image-segmentation img="romney-c-6.jpg" alt="fake"/>
    	<elections:image-segmentation img="romney-c-7.jpg" alt="fake"/>
    	<elections:image-segmentation img="romney-c-8.jpg" alt="fake"/>
    	<elections:image-segmentation img="romney-c-9.jpg" alt="fake"/>
    		
	  	<elections:image-segmentation img="obama-c-0.jpg" alt="fake"/>
    	<elections:image-segmentation img="obama-c-1.jpg" alt="fake"/>
    	<elections:image-segmentation img="obama-c-2.jpg" alt="fake"/>
    	<elections:image-segmentation img="obama-c-3.jpg" alt="fake"/>
    	<elections:image-segmentation img="obama-c-4.jpg" alt="fake"/>
    	<elections:image-segmentation img="obama-c-5.jpg" alt="fake"/>
    	<elections:image-segmentation img="obama-c-6.jpg" alt="fake"/>
    	<elections:image-segmentation img="obama-c-7.jpg" alt="fake"/>
    	<elections:image-segmentation img="obama-c-8.jpg" alt="fake"/>
    	<elections:image-segmentation img="obama-c-9.jpg" alt="fake"/>
    	
		<elections:image-segmentation img="romney-r-0.jpg" alt="real"/>
    	<elections:image-segmentation img="romney-r-1.jpg" alt="real"/>
    	<elections:image-segmentation img="romney-r-2.jpg" alt="real"/>
    	<elections:image-segmentation img="romney-r-3.jpg" alt="real"/>
    	<elections:image-segmentation img="romney-r-4.jpg" alt="real"/>
    	<elections:image-segmentation img="romney-r-5.jpg" alt="real"/>
    	<elections:image-segmentation img="romney-r-6.jpg" alt="real"/>
    	<elections:image-segmentation img="romney-r-7.jpg" alt="real"/>
    	<elections:image-segmentation img="romney-r-8.jpg" alt="real"/>
    	<elections:image-segmentation img="romney-r-9.jpg" alt="real"/>
  	</section>
  	
  	<script type="text/javascript">
  	$(".btn").click(function(){
  		var button = $(this);
  		var src = $(this).prev().attr('src').substring(5);
  		
  		$.ajax({
  		  url: 'image-segmentation/'+src,
  		  type: 'POST',
  		  success: function(data){
   			 button.replaceWith("<img src='/segmented/0-"+src+"'/>");
  			 $("#origin").replaceWith("<img id='origin' src='/imgs/"+src+"'/>");
   			 $("#0").replaceWith("<img id='0'src='/segmented/0-"+src+"'/>");
  			 $("#5").replaceWith("<img id='5' src='/segmented/5-"+src+"'/>");
  			 $("#10").replaceWith("<img id='10' src='/segmented/10-"+src+"'/>");
  			 $("#15").replaceWith("<img id='15' src='/segmented/15-"+src+"'/>");
  			 $("#20").replaceWith("<img id='20' src='/segmented/final-"+src+"'/>");
  		
   		  },
		  error: function(data){
	  			 $(this).replaceWith("<img src='/segmented/20-obama-r-0.jpg'/>");
	  			 $("#0").replaceWith("<img src='/segmented/0-obama-r-0.jpg'/>");
	  			 $("#5").replaceWith("<img src='/segmented/5-obama-r-0.jpg'/>");
	  			 $("#10").replaceWith("<img src='/segmented/10-obama-r-0.jpg'/>");
	  			 $("#15").replaceWith("<img src='/segmented/15-obama-r-0.jpg'/>");
	  			 $("#20").replaceWith("<img src='/segmented/20-obama-r-0.jpg'/>");
	  			 

	  		  }
  		});
  	});
  	</script>