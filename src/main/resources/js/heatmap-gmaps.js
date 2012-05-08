/* 
 * heatmap.js GMaps overlay
 *
 * Copyright (c) 2011, Patrick Wied (http://www.patrick-wied.at)
 * Dual-licensed under the MIT (http://www.opensource.org/licenses/mit-license.php)
 * and the Beerware (http://en.wikipedia.org/wiki/Beerware) license.
 */

function HeatmapOverlay(map, cfg) {
	var me = this;

	me.heatmap = null;
	me.conf = cfg;
	me.latlngs = [];
	me.bounds = null;
	me.setMap(map);

};

HeatmapOverlay.prototype = new google.maps.OverlayView();

HeatmapOverlay.prototype.onAdd = function() {

	var panes = this.getPanes(),
	w = this.getMap().getDiv().clientWidth,
	h = this.getMap().getDiv().clientHeight,
	el = document.createElement("div");

	el.style.position = "absolute";
	el.style.top = 0;
	el.style.left = 0;
	el.style.width = w + "px";
	el.style.height = h + "px";
	el.style.border = 0;

	this.conf.element = el;
	panes.overlayLayer.appendChild(el);

	var overlay = this;
	google.maps.event.addListener(overlay.getMap(), 'center_changed',
			function() {
				overlay.draw();
			});

	this.heatmap = h337.create(this.conf);
};

HeatmapOverlay.prototype.onRemove = function() {
	// Empty for now.
};

HeatmapOverlay.prototype.draw = function() {
    
    console.log("HeatmapOverlay.draw");

	var overlayProjection = this.getProjection(),
	currentBounds = this.map.getBounds();

	// bounds didn't change -> don't draw
	if (currentBounds.equals(this.bounds)) {
		return;
	}
	this.bounds = currentBounds;

	// ne & sw on the huge overlay div
	var ne = overlayProjection.fromLatLngToDivPixel(currentBounds.getNorthEast()),
	sw = overlayProjection.fromLatLngToDivPixel(currentBounds.getSouthWest()),
	topY = ne.y,
	leftX = sw.x,
	h = sw.y - ne.y,
	w = ne.x - sw.x,
	heatmap = this.heatmap;

	this.conf.element.style.left = leftX + 'px';
	this.conf.element.style.top = topY + 'px';
	this.conf.element.style.width = w + 'px';
	this.conf.element.style.height = h + 'px';
	heatmap.store.get("heatmap").resize();

	this.stopAnimation();
	this.setDataSet(this.dataSet);
    this.startAnimation();
};

HeatmapOverlay.prototype.setDataSet = function(data) {
    
	var mapdata = {
		max : 1.0,
		data : []
	    },
	    d = data.data,
	    dlen = d.length,
	    projection = this.getProjection(),
	    heatmap = this.heatmap,
	    currentBounds = this.map.getBounds(),
	    latlng,
	    point, dataPoint;

	this.latlngs = [];
	
	for ( var i = 0; i < dlen; i += 1 ) {
	    dataPoint = d[i];
		latlng = new google.maps.LatLng(dataPoint.lat, dataPoint.lng);
		
		// TODO remove in future
		if (!nybounds.contains(latlng))
			continue;

		this.latlngs.push({
			latlng : latlng,
			c : dataPoint.count
		});
		var point = projection.fromLatLngToContainerPixel(latlng);
		mapdata.data.push({
			x : point.x,
			y : point.y,
			count : dataPoint.count,
			time : dataPoint.createdAt
		});
	}

	heatmap.clear();
	heatmap.store.setDataSet(mapdata);

};

HeatmapOverlay.prototype.addDataPoint = function(lat, lng, count) {

	var projection = this.getProjection(), latlng = new google.maps.LatLng(lat,
			lng), point = projection.fromLatLngToContainerPixel(latlng);

	this.heatmap.store.addDataPoint(point.x, point.y, count);
	this.latlngs.push({
		latlng : latlng,
		c : count
	});
};

HeatmapOverlay.prototype.toggle = function() {
	this.heatmap.toggleDisplay();
};

HeatmapOverlay.prototype.startAnimation = function() {
    this.heatmap.startAnimation();
}

HeatmapOverlay.prototype.stopAnimation = function() {
    this.heatmap.stopAnimation();
}