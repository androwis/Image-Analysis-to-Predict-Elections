var average
function process(image, canvas){
    var imageObj = new Image();
    imageObj.src = image;
    imageObj.onload = function(){
        drawImage(this, canvas);
    };
}
function drawImage(imageObj, canvasId){

// Initialize the histograms.
var blues  = new Array(256);
var reds   = new Array(256);
var greens = new Array(256);
 for (var i = 0; i < blues.length; i ++) {
	reds[i]=0; blues[i]=0; greens[i]=0;
}

var canvas = document.getElementById(canvasId);
var context = canvas.getContext("2d");
 
var destX = 0;
var destY = 0;
var sourceWidth = imageObj.width;
var sourceHeight = imageObj.height;
canvas.height = sourceHeight;
canvas.width = sourceWidth;
context.drawImage(imageObj, destX, destY);

var imageData = context.getImageData(0, 0, sourceWidth, sourceHeight);
var data = imageData.data;
 
// to quickly iterate over all pixels, use a for loop like this
for (var i = 0; i < data.length; i += 4) {
	var red = data[i]; // red
    var green = data[i + 1]; // green
    var blue = data[i + 2]; // blue
    blues[(red+green+blue)/3]++; reds[(red-blue)/2]++; greens[(2*green-red-blue)/4]++;	
    // i+3 is alpha (the fourth element)
}
 
// or iterate over all pixels based on x and y coordinates
// like this
for (var y = 0; y < sourceHeight; y++) {
	// loop through each column
    for (var x = 0; x < sourceWidth; x++) {
    	var red = data[((sourceWidth * y) + x) * 4];
        var green = data[((sourceWidth * y) + x) * 4 + 1];
        var blue = data[((sourceWidth * y) + x) * 4 + 2];
    }
}

// draw the altered image if we manipulated the image data
context.putImageData(imageData, 0, 0);
    
    
var margin = {top: 30, right: 10, bottom: 10, left: 10},
    width = 100 - margin.left - margin.right,
    height = 300 - margin.top - margin.bottom;

var x0 = Math.max(-d3.min(blues), d3.max(blues));

var mBlue = x0;
var mRed = d3.max(reds);
var mGreen = d3.max(greens);

var sBlue = 0.0;
var sRed = 0.0;
var sGreen = 0.0;
for(var i=0; i<255; i++){
	sBlue+=blues[i];
	sGreen+=greens[i];
	sRed+=reds[i];
}
$("#green").append(sBlue/mBlue+","+sRed/mRed+","+sGreen/mGreen+"<br/>");

var x = d3.scale.linear()
    .domain([0, x0])
    .range([0, width])
    .nice();

var y = d3.scale.ordinal()
    .domain(d3.range(blues.length))
    .rangeRoundBands([0, height], .2);

var svg = d3.select("."+canvasId).append("svg")
	.attr("class","hist")
	.attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
  .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

svg.selectAll(".bar")
    .data(blues)
  .enter().append("rect")
    .attr("class", function(d, i) { return d < 0 ? "bar negative" : "bar positive blue"; })
    .attr("x", function(d, i) { return x(Math.min(0, d)); })
    .attr("y", function(d, i) { return y(i); })
    .attr("width", function(d, i) { return Math.abs(x(d) - x(0)); })
    .attr("height", y.rangeBand())
    
    .data(greens)
  .enter().append("rect")
    
    .data(greens)
  .enter().append("rect")
    .attr("class", function(d, i) { return d < 0 ? "bar negative" : "bar positive green"; })
    .attr("x", function(d, i) { return x(Math.min(0, d)); })
    .attr("y", function(d, i) { return y(i); })
    .attr("width", function(d, i) { return Math.abs(x(d) - x(0)); })
    .attr("height", y.rangeBand())
    
    .data(greens)
  .enter().append("rect")
  
    .data(reds)
  .enter().append("rect")
    .attr("class", function(d, i) { return d < 0 ? "bar negative" : "bar positive red"; })
    .attr("x", function(d, i) { return x(Math.min(0, d)); })
    .attr("y", function(d, i) { return y(i); })
    .attr("width", function(d, i) { return Math.abs(x(d) - x(0)); })
    .attr("height", y.rangeBand());

svg.append("g")
    .attr("class", "x axis")
    .call(d3.svg.axis().scale(x).orient("top"));

svg.append("g")
    .attr("class", "y axis")
  .append("line")
    .attr("x1", x(0))
    .attr("x2", x(0))
    .attr("y1", 0)
    .attr("y2", height);
}
