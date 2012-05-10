<style>
.bar.positive.red{
  fill: red;
}

.bar.positive.blue{
  fill: blue;
}

.bar.positive.green{
  fill: green;
}

.bar{
-moz-opacity: 0.25 !important;
  -webkit-opacity: 0.25!important; 
  -ms-filter:"progid:DXImageTransform.Microsoft.Alpha(Opacity=25)" !important; 
  filter: alpha(opacity=25) !important; 
  opacity: 0.25 !important;
}

.axis text {
  font: 10px sans-serif;
}

.axis path, .axis line {
  fill: none;
  stroke: #000;
  shape-rendering: crispEdges;
}
</style>
<section id="typography">
  <div class="page-header">
    <h1>Histograms <small>Image Analysis and Background Information</small></h1>
  </div>
  <!-- Headings & Paragraph Copy -->
  <div class="row">
    <div class="span4">
      <h3>Real v. Cartoon</h3>
    </div>
    <div class="span4">
      <h3>Image Mood</h3>
    </div>
    <div class="span4">
      <h3>Authenticity</h3>
    </div>
    </div>
    <br/><br/>
    <div class="span11">
      <blockquote class="pull-right">
        <p>Sophisticated image analysis and editing is challenging due to the complexities of image appearance</p>
        <small>Micah Kimo Johnson Ph.D. Dartmouth</small>
      </blockquote>
    </div>
</section>
<br/><br/><br/>

	<section class="photos">
		<div class="myCanvas-r-0">
	    	<canvas id="myCanvas-r-0"></canvas>
	    </div>
	    <div class="myCanvas-r-1">
			<canvas id="myCanvas-r-1"></canvas>
  	  	</div>
	    <div class="myCanvas-r-2">
			<canvas id="myCanvas-r-2"></canvas>
  	  	</div>
	    <div class="myCanvas-r-3">
			<canvas id="myCanvas-r-3"></canvas>
  	  	</div>
	    <div class="myCanvas-r-4">
			<canvas id="myCanvas-r-4"></canvas>
  	  	</div>
	    <div class="myCanvas-r-5">
			<canvas id="myCanvas-r-5"></canvas>
		</div>
	    <div class="myCanvas-r-6">
			<canvas id="myCanvas-r-6"></canvas>
  	  	</div>
	    <div class="myCanvas-r-7">
			<canvas id="myCanvas-r-7"></canvas>
  	  	</div>
	    <div class="myCanvas-r-8">
			<canvas id="myCanvas-r-8"></canvas>
  	  	</div>
	    <div class="myCanvas-r-9">
			<canvas id="myCanvas-r-9"></canvas>
		</div>

		<div class="myCanvas-r-00">
	    	<canvas id="myCanvas-r-00"></canvas>
	    </div>
	    <div class="myCanvas-r-01">
			<canvas id="myCanvas-r-01"></canvas>
  	  	</div>
	    <div class="myCanvas-r-02">
			<canvas id="myCanvas-r-02"></canvas>
  	  	</div>
	    <div class="myCanvas-r-03">
			<canvas id="myCanvas-r-03"></canvas>
  	  	</div>
	    <div class="myCanvas-r-04">
			<canvas id="myCanvas-r-04"></canvas>
  	  	</div>
	    <div class="myCanvas-r-05">
			<canvas id="myCanvas-r-05"></canvas>
		</div>
	    <div class="myCanvas-r-06">
			<canvas id="myCanvas-r-06"></canvas>
  	  	</div>
	    <div class="myCanvas-r-07">
			<canvas id="myCanvas-r-07"></canvas>
  	  	</div>
	    <div class="myCanvas-r-08">
			<canvas id="myCanvas-r-08"></canvas>
  	  	</div>
	    <div class="myCanvas-r-09">
			<canvas id="myCanvas-r-09"></canvas>
		</div>

		<div class="myCanvas-c-00">
	    	<canvas id="myCanvas-c-00"></canvas>
	    </div>
	    <div class="myCanvas-c-01">
			<canvas id="myCanvas-c-01"></canvas>
  	  	</div>
	    <div class="myCanvas-c-02">
			<canvas id="myCanvas-c-02"></canvas>
  	  	</div>
	    <div class="myCanvas-c-03">
			<canvas id="myCanvas-c-03"></canvas>
  	  	</div>
	    <div class="myCanvas-c-04">
			<canvas id="myCanvas-c-04"></canvas>
  	  	</div>
	    <div class="myCanvas-c-05">
			<canvas id="myCanvas-c-05"></canvas>
		</div>
	    <div class="myCanvas-c-06">
			<canvas id="myCanvas-c-06"></canvas>
  	  	</div>
	    <div class="myCanvas-c-07">
			<canvas id="myCanvas-c-07"></canvas>
  	  	</div>
	    <div class="myCanvas-c-08">
			<canvas id="myCanvas-c-08"></canvas>
  	  	</div>
	    <div class="myCanvas-c-09">
			<canvas id="myCanvas-c-09"></canvas>
		</div>


	<div class="myCanvas-c-0">
	    	<canvas id="myCanvas-c-0"></canvas>
	    </div>
	    <div class="myCanvas-c-1">
			<canvas id="myCanvas-c-1"></canvas>
  	  	</div>
	    <div class="myCanvas-c-2">
			<canvas id="myCanvas-c-2"></canvas>
  	  	</div>
	    <div class="myCanvas-c-3">
			<canvas id="myCanvas-c-3"></canvas>
  	  	</div>
	    <div class="myCanvas-c-4">
			<canvas id="myCanvas-c-4"></canvas>
  	  	</div>
	    <div class="myCanvas-c-5">
			<canvas id="myCanvas-c-5"></canvas>
		</div>
	    <div class="myCanvas-c-6">
			<canvas id="myCanvas-c-6"></canvas>
  	  	</div>
	    <div class="myCanvas-c-7">
			<canvas id="myCanvas-c-7"></canvas>
  	  	</div>
	    <div class="myCanvas-c-8">
			<canvas id="myCanvas-c-8"></canvas>
  	  	</div>
	    <div class="myCanvas-c-9">
			<canvas id="myCanvas-c-9"></canvas>
		</div>

	</section>

<script src="http://mbostock.github.com/d3/d3.v2.js?2.8.1"></script>
<script src="js/hist.js"></script>
<script>
window.onload = function(){
    process("imgs/obama-r-0.jpg","myCanvas-r-0");
    process("imgs/obama-r-1.jpg","myCanvas-r-1");
    process("imgs/obama-r-2.jpg","myCanvas-r-2");
    process("imgs/obama-r-3.jpg","myCanvas-r-3");
    process("imgs/obama-r-4.jpg","myCanvas-r-4");
    process("imgs/obama-r-5.jpg","myCanvas-r-5");
    process("imgs/obama-r-6.jpg","myCanvas-r-6");
    process("imgs/obama-r-7.jpg","myCanvas-r-7");
    process("imgs/obama-r-8.jpg","myCanvas-r-8");
    process("imgs/obama-r-9.jpg","myCanvas-r-9");    
    process("imgs/romney-r-0.jpg","myCanvas-r-00");
    process("imgs/romney-r-1.jpg","myCanvas-r-01");
    process("imgs/romney-r-2.jpg","myCanvas-r-02");
    process("imgs/romney-r-3.jpg","myCanvas-r-03");
    process("imgs/romney-r-4.jpg","myCanvas-r-04");
    process("imgs/romney-r-5.jpg","myCanvas-r-05");
    process("imgs/romney-r-6.jpg","myCanvas-r-06");
    process("imgs/romney-r-7.jpg","myCanvas-r-07");
    process("imgs/romney-r-8.jpg","myCanvas-r-08");
    process("imgs/romney-r-9.jpg","myCanvas-r-09");    
    process("imgs/romney-c-0.jpg","myCanvas-c-00");
    process("imgs/romney-c-1.jpg","myCanvas-c-01");
    process("imgs/romney-c-2.jpg","myCanvas-c-02");
    process("imgs/romney-c-3.jpg","myCanvas-c-03");
    process("imgs/romney-c-4.jpg","myCanvas-c-04");
    process("imgs/romney-c-5.jpg","myCanvas-c-05");
    process("imgs/romney-c-6.jpg","myCanvas-c-06");
    process("imgs/romney-c-7.jpg","myCanvas-c-07");
    process("imgs/romney-c-8.jpg","myCanvas-c-08");
    process("imgs/romney-c-9.jpg","myCanvas-c-09");    
    process("imgs/obama-c-0.jpg","myCanvas-c-0");
    process("imgs/obama-c-1.jpg","myCanvas-c-1");
    process("imgs/obama-c-2.jpg","myCanvas-c-2");
    process("imgs/obama-c-3.jpg","myCanvas-c-3");
    process("imgs/obama-c-4.jpg","myCanvas-c-4");
    process("imgs/obama-c-5.jpg","myCanvas-c-5");
    process("imgs/obama-c-6.jpg","myCanvas-c-6");
    process("imgs/obama-c-7.jpg","myCanvas-c-7");
    process("imgs/obama-c-8.jpg","myCanvas-c-8");
    process("imgs/obama-c-9.jpg","myCanvas-c-9");    
}
</script>