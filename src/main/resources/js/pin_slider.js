
function PinSliderControl(timeDisplay) {
	// initialize slider divs
	this.sliderContainer = document.createElement('div');
	this.sliderTweets = document.createElement('div');
	this.timeDisplay = timeDisplay;
	$(this.sliderContainer).attr('id', 'sliderContainer');
	$(this.sliderTweets).attr('id', 'sliderTweets');
	$(this.sliderContainer).append(this.sliderTweets);
	// style
	this.sliderContainer.style.width = '400px';
	this.sliderContainer.style.paddingRight = '10px';
	this.sliderContainer.style.paddingTop = '10px';
	
	// create slider, set events
	$(this.sliderTweets).slider({
		max: 0,
		min: -32
	});
	$(this.sliderTweets).slider('option', 'value', -32);
	$("#sliderVal_hold").html('' + -32);
	
	$(this.sliderContainer).bind('click', function(event) {
		if(tutorialRunning()) {
			event.stopImmediatePropagation();
		}
	})
	
	$(this.sliderTweets).bind('slide', function(event, ui) {
		// get value,  make sure time is shown 
		var val = ui.value;
		$(timeDisplay.timeOverlay).show();
		
		// for today: live tweets
		/*if(val == 0) {
			$(timeDisplay.timeOverlay).html('Live Tweets');
		}
		// any other day: calculate the actual other day
		else {*/
		var daysMs = val * 24 * 60 * 60 * 1000;
		var todayMidnight = new Date(2010, 5, 20);
		todayMidnight.setHours(23, 59, 59, 999);
		var otherDay = new Date(todayMidnight.getTime() + daysMs);
		$(timeDisplay.timeOverlay).html('' + otherDay.toDateString());
		/*}*/
	});
	
	$(this.sliderTweets).bind('slidechange', function(event, ui) {
		// trigger draw for all pins
		$("#sliderVal_hold").html('' + ui.value);
		for(var tweet in twitterHealthPins) {
			if(twitterHealthPins.hasOwnProperty(tweet)) {
				twitterHealthPins[tweet].draw();
			}
		}
		
		// fade out date display
		setTimeout(function() {
			$(timeDisplay.timeOverlay).effect('fade', {}, 1000);
		}, 500);
	});
	
	// not sure that this is necessary really
	this.sliderContainer.index = 1;
}

function TimeDisplay() {
	// add time display overlay
	this.timeOverlay = document.createElement('div');
	$(this.timeOverlay).attr('id', 'timeOverlay');
	// style
	this.timeOverlay.style.width = '400px';
	this.timeOverlay.style.paddingRight = '10px';
	this.timeOverlay.style.fontSize = '20px';
	this.timeOverlay.style.textAlign = 'center';
	this.timeOverlay.style.color = '#FFFFFF';
	
	// not sure if this is necessary
	this.timeOverlay.index = 1;
}

function TimeControls(pinSlider, timeDisplay) {
	// initialize component divs
	this.pinSlider = pinSlider;
	this.timeDisplay = timeDisplay;
	this.state = 'pause';
	this.playTimeout = null;
	this.timeControlsContainer = document.createElement('div');
	this.backControl = document.createElement('div');
	this.pauseControl = document.createElement('div');
	this.playControl = document.createElement('div');
	this.forwardControl = document.createElement('div');
	$(this.timeControlsContainer).append(this.backControl);
	$(this.timeControlsContainer).append(this.pauseControl);
	$(this.timeControlsContainer).append(this.playControl);
	$(this.timeControlsContainer).append(this.forwardControl);
	// style
	this.timeControlsContainer.style.width = '400px';
	$(this.timeControlsContainer).children().css('float', 'left');
	$(this.timeControlsContainer).children().css('margin-left', '42px');
	$(this.timeControlsContainer).children().css('margin-right', '42px');
	$(this.timeControlsContainer).children().css('margin-top', '8px');
	$(this.timeControlsContainer).children().css('margin-bottom', '8px');
	$(this.timeControlsContainer).children().addClass('ui-icon');
	$(this.backControl).addClass('ui-icon-seek-prev');
	$(this.pauseControl).addClass('ui-icon-pause');
	$(this.playControl).addClass('ui-icon-play');
	$(this.forwardControl).addClass('ui-icon-seek-next');
	
	// event handlers
	var timeControl = this; // for closure purposes
	
	google.maps.event.addDomListener(this.backControl, 'click', function() {
		if(!tutorialRunning()) {
			// get current value and min
			var slideVal = parseInt($(timeControl.pinSlider.sliderTweets).slider('value'));
			var slideMin = parseInt($(timeControl.pinSlider.sliderTweets).slider('option', 'min'));

			// if we can go backward, do it
			if(slideVal > slideMin) {
				$(timeControl.pinSlider.sliderTweets).slider('value', --slideVal);
			}

			// display new time
			$(timeControl.timeDisplay.timeOverlay).show();
			/*if(slideVal == 0) {
			$(timeControl.timeDisplay.timeOverlay).html('Live Tweets');
			}
			else {*/
			var daysMs = slideVal * 24 * 60 * 60 * 1000;
			var todayMidnight = new Date(2010, 5, 20);
			todayMidnight.setHours(23, 59, 59, 999);
			var otherDay = new Date(todayMidnight.getTime() + daysMs);
			$(timeControl.timeDisplay.timeOverlay).html('' + otherDay.toDateString());
			/*}*/
		}
	});
	
	google.maps.event.addDomListener(this.pauseControl, 'click', function() {
		if(!tutorialRunning()) {
			// if it is playing, stop it
			if(timeControl.state == 'play') {
				timeControl.state = 'pause';
				if(!!timeControl.playTimeout) {
					clearTimeout(timeControl.playTimeout);
					timeControl.playTimeout = null;
				}
			}
		}
	});
	
	google.maps.event.addDomListener(this.playControl, 'click', function() {
		if(!tutorialRunning()) {
			var slideVal = parseInt($(timeControl.pinSlider.sliderTweets).slider('value'));
			if(timeControl.state != 'play' && slideVal < 0) {
				timeControl.state = 'play';
				$(timeControl.timeDisplay.timeOverlay).show();
				setTimeout(function() {
					$(timeControl.timeDisplay.timeOverlay).effect('fade', {}, 1000);
				}, 500);
				
				timeControl.playTimeout = setTimeout(function() {
					playLoop(timeControl);
				}, 2000);
			}
		}
	});
	
	google.maps.event.addDomListener(this.forwardControl, 'click', function() {
		if(!tutorialRunning()) {
			// get current value and max
			var slideVal = parseInt($(timeControl.pinSlider.sliderTweets).slider('value'));
			var slideMax = parseInt($(timeControl.pinSlider.sliderTweets).slider('option', 'max'));

			// if we can go forward, do it
			if(slideVal < slideMax) {
				$(timeControl.pinSlider.sliderTweets).slider('value', ++slideVal);
			}

			// display new time
			/*$(timeControl.timeDisplay.timeOverlay).show();
			if(slideVal == 0) {
			$(timeControl.timeDisplay.timeOverlay).html('Live Tweets');
			}
			else {*/
			var daysMs = slideVal * 24 * 60 * 60 * 1000;
			var todayMidnight = new Date(2010, 5, 20);
			todayMidnight.setHours(23, 59, 59, 999);
			var otherDay = new Date(todayMidnight.getTime() + daysMs);
			$(timeControl.timeDisplay.timeOverlay).html('' + otherDay.toDateString());
			/*}*/
		}
	});
	
	// not sure if this is really necessary
	this.timeControlsContainer.index = 1;
}

function playLoop(timeControl) {
	var slideVal = parseInt($(timeControl.pinSlider.sliderTweets).slider('value'));
	if(slideVal >= 0) {
		timeControl.state = 'pause';
		timeControl.playTimeout = null;
		return;
	}
	
	google.maps.event.trigger(timeControl.forwardControl, 'click');
	$(timeControl.timeDisplay.timeOverlay).show();
	setTimeout(function() {
		$(timeControl.timeDisplay.timeOverlay).effect('fade', {}, 1000);
	}, 500);
	
	timeControl.playTimeout = setTimeout(function() {
		playLoop(timeControl);
	}, 2000);
}