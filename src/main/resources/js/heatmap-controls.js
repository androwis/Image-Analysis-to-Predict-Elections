TextControl.prototype.setText = function(text) {
	this.textControl_.innerHTML = text;
}
function TextControl(div) {
	var controlUI = document.createElement("div");
	controlUI.style.backgroundColor = 'white';
	controlUI.style.borderStyle = 'solid';
	controlUI.style.borderWidth = '2px';
	controlUI.style.cursor = 'pointer';
	controlUI.style.textAlign = 'center';
	div.appendChild(controlUI);

	var textControl = document.createElement("div");
	textControl.style.fontFamily = 'Arial,sans-serif';
	textControl.style.fontSize = '12px';
	textControl.style.paddingLeft = '4px';
	textControl.style.paddingRight = '4px';
	controlUI.appendChild(textControl);
	this.textControl_ = textControl;
}
