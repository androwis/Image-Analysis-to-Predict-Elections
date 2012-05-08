
function getPollutantTypeMarker(oldMarker) {
	if(oldMarker == null) return null;
	var newUrl = 'http://labs.google.com/ridefinder/images/mm_20_purple.png';
	return new google.maps.MarkerImage(newUrl, oldMarker.size, oldMarker.origin, oldMarker.anchor);
}

function PollutantTracker(file, map) {
	// initialize
	this.map = map;
	this.file = file;
	this.active = false;
	this.markers = [];
	this.lastMarker = null;
	var cTracker = this;
	this.kmlParser = new geoXML3.parser({
		map: this.map,
		zoom: false,
		createMarker: function(placemark, doc) {
			var betterIcon = getPollutantTypeMarker(placemark.style.icon);
			var marker = new google.maps.Marker({
				map:      map,
				position: new google.maps.LatLng(placemark.point.lat, placemark.point.lng),
				title:    placemark.name,
				zIndex:   Math.round(placemark.point.lat * 100000),
				icon:     betterIcon
			});
			marker.setVisible(false);
			
			// create reduced info window content
			var infoContent = document.createElement('div');
			$(infoContent).attr('id', 'infowindow');
			// add name and address, remove extraneous spacing
			$(infoContent).append('<h3>' + placemark.name + '</h3>');
			$(infoContent).children('h3').children('br').first().remove();
			// reduce size of address
			var headText = $(infoContent).text();
			var locName = headText.split(/:\s/)[0]
			var locAddr = headText.split(/:\s/)[1];
			$(infoContent).children('h3').text(locName);
			$(infoContent).append('<p>Address: ' + locAddr + '</p>');
			// add description, remove chart and "placeholder"
			$(infoContent).append('<div>' + placemark.description + '</div>');
			var descr = $(infoContent).children('div').children('p').detach();
			$(infoContent).children('div').remove();
			$(infoContent).append(descr);
			marker.infoWindow = new google.maps.InfoWindow({
				content: infoContent,
				pixelOffset: new google.maps.Size(0, 2)
			});
			
			google.maps.event.addListener(marker, 'click', function() {
		    	if(!!cTracker.lastMarker) {
					cTracker.lastMarker.infoWindow.close();
				}
				marker.infoWindow.open(map, marker);
				cTracker.lastMarker = marker;
			});
			
			cTracker.markers.push(marker);
			return marker;
		}
	});
	this.kmlParser.parse(this.file);
}

PollutantTracker.prototype.show = function() {
	$.each(this.markers, function(index, marker) {
		marker.setVisible(true);
	});
	this.active = true;
}

PollutantTracker.prototype.hide = function() {
	$.each(this.markers, function(index, marker) {
		marker.setVisible(false);
	});
	if(!!this.lastMarker) {
		this.lastMarker.infoWindow.close();
		this.lastMarker = null;
	}
	this.active = false;
}

PollutantTracker.prototype.isActive = function() {
	return this.active;
}

function PollutantControls(map) {
	// initialize component divs and buttons
	this.map = map;
	this.activeButton = null;
	this.pollutantTrackers = {
		'NOx': new PollutantTracker('/resources/kml/Top8NoxFacilitiesFiltered.xml', map)
	};
	this.pollutantButtons = {};
	this.buttonsContainer = document.createElement('div');
	var context = this;
	$.each(context.pollutantTrackers, function(name, tracker) {
		context.pollutantButtons[name] = document.createElement('div');
		$(context.buttonsContainer).append(context.pollutantButtons[name]);
		$(context.pollutantButtons[name]).text(name);
	});
	// style
	this.buttonsContainer.style.height = '32px';
	this.buttonsContainer.style.padding = '10px';
	$(this.buttonsContainer).children().css('width', '98px');
	$(this.buttonsContainer).children().css('textAlign', 'center');
	$(this.buttonsContainer).children().css('margin-left', '2px');
	$(this.buttonsContainer).children().css('margin-right', '2px');
	$(this.buttonsContainer).children().css('float', 'left');
	$(this.buttonsContainer).children().addClass('ui-selectee');
	
	// event handlers
	$.each(context.pollutantButtons, function(index, button) {
		$(button).bind('click', function(event) {
			if(!tutorialRunning()) {
				// get name
				var bName = $(event.currentTarget).text();

				// toggle active
				if(event.currentTarget != context.activeButton) {
					// show
					context.pollutantTrackers[bName].show();
					$(event.currentTarget).addClass('ui-selected');

					// hide formerly active button
					if(!!context.activeButton) {
						var aName = $(context.activeButton).text();
						context.pollutantTrackers[aName].hide();
						$(context.activeButton).removeClass('ui-selected');
					}

					context.activeButton = event.currentTarget;
				}
				else {
					// hide
					context.pollutantTrackers[bName].hide();
					$(event.currentTarget).removeClass('ui-selected');
					context.activeButton = null;
				}
			}
		});
	});
	
	// not sure if this is really necessary
	this.buttonsContainer.index = 1;
}