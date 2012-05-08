
// for everything you want to set after jMapping is done
// I put this in its own file so it's easier to manipulate
function onLoad_mapstyle(event) {
	// NYC bounds (again)
	var psw = new google.maps.LatLng(40.467597, -74.409908);
	var pne = new google.maps.LatLng(41.046156, -73.548989);
	var nybounds = new google.maps.LatLngBounds(psw, pne);
	map.fitBounds(nybounds);
	// set to high-contrastish style
	map.setOptions({ styles: [
		{
			stylers: [
				{ visibility: "simplified" },
				{ invert_lightness: true },
				{ saturation: -85 },
				{ lightness: -18 }
			]
		}
	]});
}