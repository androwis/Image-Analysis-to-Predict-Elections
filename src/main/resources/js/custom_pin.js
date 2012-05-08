
function TwitterHealthPin(tweet, map) {
	this.tweet_ = tweet.id;
	this.color_ = tweet.color;
	this.time_ = tweet.time;
	this.canvas_ = null;
	this.image_ = null;
	this.position_ = tweet.position;
	this.bounds_ = null;
	this.theMap = map;
	
	this.setMap(this.theMap);
}

TwitterHealthPin.prototype = new google.maps.OverlayView;

TwitterHealthPin.prototype.onAdd = function() {
	// New York City bounds
	// point1: 40°28'3.35"N, 74°24'35.67"W
	// point2: 41° 2'46.16"N, 73°32'56.36"W
	// bail if this is outside NYC
	var psw = new google.maps.LatLng(40.467597, -74.409908);
	var pne = new google.maps.LatLng(41.046156, -73.548989);
	var nybounds = new google.maps.LatLngBounds(psw, pne);
	if(!nybounds.contains(this.position_)) {
		this.setMap(null);
		return;
	}
	
	// init canvas and image element
	this.canvas_ = $('#pin_canvas').append('<canvas width="12px" height="12px"></canvas>').children('canvas').last()[0];
	this.image_ = $('#pin_canvas').append('<div><img width="12px" height="12px"></img></div>').children('div').last()[0];
	$(this.canvas_).attr('id', this.tweet_);
	$(this.image_).attr('id', this.tweet_);
	this.image_.style.position = 'absolute';
	
	// make sure info window appears
	var cmarker = this;
	google.maps.event.addDomListener(this.image_, 'click', function() {
		if(!tutorialRunning()) {
			if(!!activeInfoWindow) {
				activeInfoWindow.close();
			}
			twitterInfoWindows[cmarker.tweet_].open(cmarker.getMap(), cmarker);
			activeInfoWindow = twitterInfoWindows[cmarker.tweet_];
		}
	});
	
	// add it to the overlays
	var panes = this.getPanes();
	panes.overlayMouseTarget.appendChild(this.image_);
}

TwitterHealthPin.prototype.onRemove = function() {
	$(this.canvas_).detach();
	$(this.image_).detach();
}

TwitterHealthPin.prototype.draw = function() {
	// don't draw if this pin was removed
	if(this.getMap() == null) return;
	
	// get time difference ratio, calculate alpha
	var slideVal = $('#sliderVal_hold').html();
	slideVal = parseInt(slideVal);
	var daysMs = slideVal * 24 * 60 * 60 * 1000;
	var todayMidnight = new Date(2010, 5, 20);
	todayMidnight.setHours(23, 59, 59, 999);
	var otherDay = new Date(todayMidnight.getTime() + daysMs);
	var diff = otherDay.getTime() - this.time_;
	// hide this if it's newer than the set date or older than a week
	var svdiff = 604800000; // one week in millisecs
	var imgAlpha = 1 - (diff / svdiff);
	if(diff < 0 || imgAlpha <= 0) {
		this.image_.style.visibility = "hidden";
	}
	else {
		this.image_.style.visibility = "visible";
	}
	
	// draw pin, set alpha
	var pincontext = this.canvas_.getContext('2d');
	pincontext.clearRect(0, 0, 12, 12);
	pincontext.globalAlpha = imgAlpha;
	pincontext.beginPath();
	pincontext.arc(this.canvas_.width/2, this.canvas_.height/2, this.canvas_.width/2 - 1, 0, 2*Math.PI, false);
	pincontext.fillStyle = this.color_;
	pincontext.fill();
	pincontext.lineWidth = 1;
	pincontext.strokeStyle = "black";
	pincontext.stroke();
	
	// set proper bounds
	var projection = this.getProjection();
	var oldPos = projection.fromLatLngToDivPixel(this.position_);
	var swPoint = new google.maps.Point(oldPos.x - 6, oldPos.y + 6);
	var nePoint = new google.maps.Point(oldPos.x + 6, oldPos.y - 6);
	var swBound = projection.fromDivPixelToLatLng(swPoint);
	var neBound = projection.fromDivPixelToLatLng(nePoint);
	this.bounds_ = new google.maps.LatLngBounds(swBound, neBound);
	var theImg = this.image_;
	theImg.style.top = (oldPos.y - 6) + 'px';
	theImg.style.left = (oldPos.x - 6) + 'px';
	theImg.style.width = '12px';
	theImg.style.height = '12px';
	
	// re-render image
	$(this.image_).children('img').last().attr('src', this.canvas_.toDataURL());
}

TwitterHealthPin.prototype.getPosition = function() {
	return this.position_;
}

function PinsLoadingDisplay() {
	// create and style div
	this.loadOverlay = document.createElement('div');
	$(this.loadOverlay).attr('id', 'loadOverlay');
	// style
	this.loadOverlay.style.width = '400px';
	this.loadOverlay.style.paddingRight = '10px';
	this.loadOverlay.style.fontSize = '15px';
	this.loadOverlay.style.textAlign = 'center';
	this.loadOverlay.style.color = '#FFFFFF';
	this.loadOverlay.style.paddingBottom = '20px';
	$(this.loadOverlay).text('Loading Markers for Tweets...');
	$(this.loadOverlay).effect('pulsate', {}, 500);
	
	// event handler
	$(this.loadOverlay).bind('stopLoadAnimation', function() {
		var overlay = this;
		$(overlay).effect('fade', {}, 3000, function() {
			$(overlay).detach();
		});
	});
	
	// not sure if this is necessary
	this.loadOverlay.index = 1;
}