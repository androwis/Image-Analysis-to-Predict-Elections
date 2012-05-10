<section id="typography">
  <div class="page-header">
    <h1>Faces <small>Detection, Offloading, Propogation &amp; Performance Gains</small></h1>
  </div>
  <!-- Headings & Paragraph Copy -->
  <div class="row">
    <div class="span4">
      <h3>K-Means Segmentation</h3>
    </div>
    <div class="span4">
      <h3>Composition</h3>
    </div>
    <div class="span4">
      <h3>Emotions</h3>
    </div>
    </div>
    <br/><br/>
    <div class="span11">
      <blockquote class="pull-right">
        <p>There's no art to find the mind's construction in the face.</p>
        <small>William Shakespeare</small>
      </blockquote>
    </div>
</section>
<br/><br/><br/>

<div style="float:left; clear:both" id="output" class="photos"></div>
<img id="Image" src="imgs/obama-r-0.jpg" style="height:10px;visibility:hidden;">

<div style="float:left; clear:both; width:100%">
  <input style="margin-left:16px" type="button" value="Detect" onclick="detect(ImageToLoad)" class="btn" />
  <div class="alert alert-success">
  <div id="DMessages"></div>
  <div id="elapsed_time" style="margin-top:20px"></div>
	</div>
</div>

<script type="text/javascript" src="js/ccv.js"></script>
<script type="text/javascript" src="js/face.js"></script>
<script type="text/javascript">

var ImageToLoad = 0;

function message(msg) {
  document.getElementById("DMessages").innerHTML = msg;
}

function messages(msg) {
  document.getElementById("DMessages").innerHTML = document.getElementById("DMessages").innerHTML + "<br/>"+ msg;
}

function resizeCanvas(image, canvas) {
  document.body.appendChild(image);
  canvas.width = image.offsetWidth;
  canvas.style.width = image.offsetWidth.toString() + "px";
  canvas.height = image.offsetHeight;
  canvas.style.height = image.offsetHeight.toString() + "px";
  document.body.removeChild(image);
}

function getImageName(N) {
	if(N%4 == 0){
		 return "imgs/romney-r-"+N/4+".jpg";
	}else if(N%4 == 1){
		 return "imgs/romney-c-"+(N-1)/4+".jpg";
	 }else if(N%4 == 2){
 		 return "imgs/obama-c-"+(N-2)/4+".jpg";
	 }else{
 		 return "imgs/obama-r-"+(N-3)/4+".jpg";
	 }
}

function detect(ImageNumber) {
  message ("Detecting...");
  var image = new Image();
  image.src = getImageName(ImageNumber);

  var canvas = document.createElement('canvas')
  var ctx = canvas.getContext("2d");

    /* call main detect_objects function */
    var prep = ccv.grayscale(ccv.pre(image));
    var elapsed_time = (new Date()).getTime();  // Time for the detect algorithm only
    var comp = ccv.detect_objects({ "canvas" : prep,
                    "cascade" : cascade,
                    "interval" : 5,
                    "min_neighbors" : 1 });
    message( "Elapsed time : " + ((new Date()).getTime() - elapsed_time).toString() + "ms");
    resizeCanvas(image, canvas);
    ctx.drawImage(image, 0, 0);
    ctx.lineWidth = 3;
    ctx.strokeStyle = "#f00";
    /* draw detected area */
    for (var i in comp) {
      ctx.strokeRect(comp[i].x, comp[i].y, comp[i].width, comp[i].height);
      messages("Found "+(Math.floor(i)+1)+" at ("+Math.floor(comp[i].x)+","+Math.floor(comp[i].y)+
               ") width="+Math.floor(comp[i].width)+" height="+Math.floor(comp[i].height));
    }
    $("#output").append(canvas);
    nextImage();
}

function nextImage() {
  ImageToLoad++;
  ImageToLoad=ImageToLoad%40;
  document.getElementById("Image").src = getImageName(ImageToLoad);
}
</script>