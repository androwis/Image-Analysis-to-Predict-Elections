
function TutorialPrevButton(map, tutorial) {
	// initialize elements
	this.map = map;
	this.tutorial = tutorial;
	this.button = document.createElement('div');
	// stylin'
	this.button.style.width = '98px';
	this.button.style.textAlign = 'center';
	this.button.style.marginLeft = '12px';
	this.button.style.marginRight = '12px';
	this.button.style.marginBottom = '32px';
	this.button.style.float = 'left';
	$(this.button).text('Previous');
	$(this.button).addClass('ui-selectee');
	
	// event handlers
	var context = this;
	$(this.button).bind('click', function(event) {
		if(context.tutorial.state == 'phase02') {
			context.tutorial.doPhase01();
			$(context.tutorial.phase02).hide('fade', 500);
		}
		else if(context.tutorial.state == 'phase03') {
			context.tutorial.doPhase02();
			$(context.tutorial.phase03).hide('fade', 500);
		}
	});
	
	this.button.index = 1;
}

function TutorialNextButton(map, tutorial) {
	// initialize elements
	this.map = map;
	this.tutorial = tutorial;
	this.button = document.createElement('div');
	// stylin'
	this.button.style.width = '98px';
	this.button.style.textAlign = 'center';
	this.button.style.marginLeft = '12px';
	this.button.style.marginRight = '12px';
	this.button.style.marginBottom = '32px';
	this.button.style.float = 'left';
	$(this.button).text('Next');
	$(this.button).addClass('ui-selectee');
	
	// event handlers
	var context = this;
	$(this.button).bind('click', function(event) {
		if(context.tutorial.state == 'phase01') {
			context.tutorial.doPhase02();
			$(context.tutorial.phase01).hide('fade', 500);
		}
		else if(context.tutorial.state == 'phase02') {
			context.tutorial.doPhase03();
			$(context.tutorial.phase02).hide('fade', 500);
		}
		else if(context.tutorial.state == 'phase03') {
			$(context.tutorial.prevButton.button).hide('fade', 500);
			$(context.button).hide('fade', 500);
			$(context.tutorial.phase03).hide('fade', 500, function(event) {
				context.tutorial.doPhaseEnd();
			});
		}
	});
	
	this.button.index = 1;
}

function TutorialOverlay(map) {
	// overlay parameters
	this.map = map;
	this.wholeOverlay = null;
	this.phase00 = null;
	this.phase01 = null;
	this.phase02 = null;
	this.phase03 = null;
	this.state = 'start';
	this.timeouts = {
		'phase00': null,
		'phase01': null,
		'phase02': null,
		'phase03': null
	}
	this.controlDisableCallback = function(event) {
		event.stopImmediatePropagation();
	}
	this.overgray = new TutorialOvergray(map);
	this.prevButton = new TutorialPrevButton(map, this);
	this.nextButton = new TutorialNextButton(map, this);
	map.controls[google.maps.ControlPosition.BOTTOM_CENTER].clear();
	map.controls[google.maps.ControlPosition.BOTTOM_CENTER].push(this.prevButton.button);
	map.controls[google.maps.ControlPosition.BOTTOM_CENTER].push(this.nextButton.button);
	
	// set the map
	this.setMap(map);
}

TutorialOverlay.prototype = new google.maps.OverlayView();

TutorialOverlay.prototype.onAdd = function() {
	// create container div for entire overlay
	this.wholeOverlay = document.createElement('div');
	this.wholeOverlay.style.width = '100%';
	this.wholeOverlay.style.height = '100%';
	this.wholeOverlay.style.top = '0px';
	this.wholeOverlay.style.left = '0px';
	this.wholeOverlay.style.position = 'absolute';
	
	// marker tut
	this.phase01 = document.createElement('div');
	this.phase01.style.position = 'absolute';
	this.phase01.style.color = '#FFFFFF';
	this.phase01.style.fontSize = '40px';
	this.phase01.style.textAlign = 'left';
	//this.phase01.style.left = '40px';
	this.phase01.style.width = $("#map_canvas").width() * 0.7 + 'px';
	$(this.phase01).append('<p>Each marker is a person. ' +
		'Sick people are colored red, healthy people are colored green. ' +
		'Recency of each tweet is indicated by its opacity: ' +
		'more opaque markers are more recent.<p>');
	var p1arrow = document.createElement('img');
	p1arrow.src = "resources/icons/arrow_right.png";
	p1arrow.style.position = 'relative';
	p1arrow.style.width = '350px';
	p1arrow.style.height = '357px';
	p1arrow.style.left = '300px';
	p1arrow.style.top = '0px';
	$(this.phase01).append(p1arrow);
	$(this.phase01).hide();
	
	// time controls tut
	this.phase02 = document.createElement('div');
	var p2arrow = document.createElement('img');
	p2arrow.src = "resources/icons/arrow_up.png";
	p2arrow.style.position = 'relative';
	p2arrow.style.width = '350px';
	p2arrow.style.height = '357px';
	p2arrow.style.left = '900px';
	p2arrow.style.top = '40px';
	$(this.phase02).append(p2arrow);
	this.phase02.style.position = 'absolute';
	this.phase02.style.color = '#FFFFFF';
	this.phase02.style.fontSize = '40px';
	this.phase02.style.textAlign = 'left';
	//this.phase02.style.left = '40px';
	this.phase02.style.width = $("#map_canvas").width() * 0.7 + 'px';
	$(this.phase02).append('<p>You can select the time you are interested with the slider. ' +
		'Hit the play button to animate.</p>');
	//this.phase02.style.top = '30px';
	$(this.phase02).hide();
	
	// pollution controls tut
	this.phase03 = document.createElement('div');
	$(this.phase03).hide();
	this.phase03.style.position = 'absolute';
	this.phase03.style.color = '#FFFFFF';
	this.phase03.style.fontSize = '40px';
	this.phase03.style.textAlign = 'left';
	//this.phase03.style.left = '40px';
	this.phase03.style.width = $("#map_canvas").width() * 0.7 + 'px';
	$(this.phase03).append('<p>Click the NOx button to see pollution sources.</p>');
	var p3arrow = document.createElement('img');
	p3arrow.src = "resources/icons/arrow_left.png";
	p3arrow.style.position = 'relative';
	p3arrow.style.width = '350px';
	p3arrow.style.height = '357px';
	p3arrow.style.left = '40px';
	p3arrow.style.top = '-60px';
	$(this.phase03).append(p3arrow);
	//this.phase03.style.top = $("#map_canvas").height() - $(this.phase03).height() - 20 + 'px';
	
	// add to container
	$(this.wholeOverlay).append(this.phase01);
	$(this.wholeOverlay).append(this.phase02);
	$(this.wholeOverlay).append(this.phase03);
	
	$(this.phase01).position({
		my: "left top",
		at: "left top",
		of: "#map_canvas",
		offset: "50 20"
	});
	
	$(this.phase02).position({
		my: "left top",
		at: "left top",
		of: "#map_canvas",
		offset: "40 30"
	});
	
	$(this.phase03).position({
		my: "left center",
		at: "left center",
		of: "#map_canvas",
		offset: "50 -70"
	});
	// add overlay to map panes
	var panes = this.getPanes();
	panes.floatPane.appendChild(this.wholeOverlay);
}

TutorialOverlay.prototype.onRemove = function() {
	// remove all divs
	this.wholeOverlay.parentNode.removeChild(this.wholeOverlay);
}

TutorialOverlay.prototype.draw = function() {
	// reposition map
	var psw = new google.maps.LatLng(40.467597, -74.409908);
	var pne = new google.maps.LatLng(41.046156, -73.548989);
	var nybounds = new google.maps.LatLngBounds(psw, pne);
	this.map.fitBounds(nybounds);
	
	// disable controls temporarily
	this.map.setOptions({
		draggable: false,
		scrollwheel: false
	});
	
	$(this.prevButton.button).show();
	$(this.nextButton.button).show();
	this.doPhase01();
}

TutorialOverlay.prototype.doPhase01 = function() {
	// get current map projection
	this.state = 'phase01';
	this.overgray.doPhase01();
	var projection = this.getProjection();
	
	// controls stuff
	if(!$(this.prevButton.button).hasClass('ui-state-disabled')) {
		$(this.prevButton.button).addClass('ui-state-disabled')
	}
	
	// create text, create arrow, click a marker
	
	$(this.phase01).show('fade', {}, 500);
}

TutorialOverlay.prototype.doPhase02 = function() {
	// get current map projection
	this.state = 'phase02';
	this.overgray.doPhase02();
	var projection = this.getProjection();
	
	// controls stuff
	if($(this.prevButton.button).hasClass('ui-state-disabled')) {
		$(this.prevButton.button).removeClass('ui-state-disabled')
	}
	if($(this.nextButton.button).text() == 'End') {
		$(this.nextButton.button).text('Next');
	}
	
	// create text, create arrow, click a marker
	
	$(this.phase02).show('fade', {}, 500);
}

TutorialOverlay.prototype.doPhase03 = function() {
	// get current map projection
	this.state = 'phase03';
	this.overgray.doPhase03();
	var projection = this.getProjection();
	
	// controls stuff
	if($(this.nextButton.button).text() == 'Next') {
		$(this.nextButton.button).text('End');
	}
	
	// create text, create arrow, click a marker
	$(this.phase03).show('fade', {}, 500);
}

TutorialOverlay.prototype.doPhaseEnd = function() {
	this.state = 'end';
	this.overgray.doPhaseEnd();
	
	// reenable controls
	this.map.setOptions({
		draggable: true,
		scrollwheel: true
	});
	
	this.map.controls[google.maps.ControlPosition.BOTTOM_CENTER].clear();
	this.setMap(null);
}

function TutorialOvergray(map) {
	this.map = map;
	this.canvas = null;
	this.image = null;
	
	this.setMap(map);
}

TutorialOvergray.prototype = new google.maps.OverlayView();

TutorialOvergray.prototype.onAdd = function() {
	this.canvas = document.createElement('canvas');
	this.image = document.createElement('image');
	this.canvas.width = $("#map_canvas").width();
	this.canvas.height = $("#map_canvas").height();
	this.image.width = this.canvas.width;
	this.image.height = this.canvas.height;
	$(this.image).hide();
	
	var panes = this.getPanes();
	panes.floatPane.appendChild(this.image);
}

TutorialOvergray.prototype.onRemove = function() {
	this.image.parentNode.removeChild(this.image);
}

TutorialOvergray.prototype.draw = function() {
	$(this.image).show('fade', 250);
}

TutorialOvergray.prototype.doPhase01 = function() {
	var projection = this.getProjection();
	
	// calculate center point of nyc area
	var psw = new google.maps.LatLng(40.467597, -74.409908);
	var pne = new google.maps.LatLng(41.046156, -73.548989);
	var pixsw = projection.fromLatLngToDivPixel(psw);
	var pixne = projection.fromLatLngToDivPixel(pne);
	var pixcen = new google.maps.Point((pixsw.x + pixne.x)/2, (pixsw.y + pixne.y)/2);
	
	// draw grey area with transparent center
	var context = this.canvas.getContext('2d');
	context.clearRect(0, 0, this.canvas.width, this.canvas.height);
	context.globalAlpha = 0.25;
	context.rect(0, 0, this.canvas.width, this.canvas.height);
	context.fillStyle = "gray";
	context.fill();
	context.save();
	context.beginPath();
	context.arc(pixcen.x, pixcen.y, 200, 0, 2*Math.PI, false);
	context.clip();
	context.globalAlpha = 0.00;
	context.beginPath();
	context.rect(0, 0, this.canvas.width, this.canvas.height);
	context.fillStyle = "gray";
	context.fill();
	context.restore();
	
	// fill image with canvas data
	this.image.src = this.canvas.toDataURL();
}

TutorialOvergray.prototype.doPhase02 = function() {
	var projection = this.getProjection();
	
	// draw grey area over entire screen
	var context = this.canvas.getContext('2d');
	context.clearRect(0, 0, this.canvas.width, this.canvas.height);
	context.globalAlpha = 0.25;
	context.rect(0, 0, this.canvas.width, this.canvas.height);
	context.fillStyle = "gray";
	context.fill();
	
	// fill image with canvas data
	this.image.src = this.canvas.toDataURL();
}

TutorialOvergray.prototype.doPhase03 = function() {
	var projection = this.getProjection();
	
	// draw grey area over entire screen
	var context = this.canvas.getContext('2d');
	context.clearRect(0, 0, this.canvas.width, this.canvas.height);
	context.globalAlpha = 0.25;
	context.rect(0, 0, this.canvas.width, this.canvas.height);
	context.fillStyle = "gray";
	context.fill();
	
	// fill image with canvas data
	this.image.src = this.canvas.toDataURL();
}

TutorialOvergray.prototype.doPhaseEnd = function() {
	var context = this;
	$(this.image).hide('fade', 250, function() {
		context.setMap(null);
	});
}

function TutorialButton(map) {
	// initialize component divs and buttons
	this.map = map;
	this.buttonDiv = document.createElement('div');
	this.tutOverlay = null;
	this.timeout = null;
	// style
	this.buttonDiv.style.width = '98px';
	this.buttonDiv.style.textAlign = 'center';
	this.buttonDiv.style.marginLeft = '12px';
	this.buttonDiv.style.marginRight = '12px';
	this.buttonDiv.style.float = 'left';
	$(this.buttonDiv).text('Tutorial');
	$(this.buttonDiv).addClass('ui-selectee');
	
	// event handlers
	var context = this;
	$(this.buttonDiv).bind('click', function(event) {
		if(context.tutOverlay == null || context.tutOverlay.state == 'end') {
			context.tutOverlay = new TutorialOverlay(map);
		}
	});
	
	this.buttonDiv.index = 1;
}