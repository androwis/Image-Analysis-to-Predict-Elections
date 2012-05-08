
function AboutButton(map) {
	// initialize component divs
	this.map = map;
	this.button = document.createElement('div');
	this.window = new AboutWindow(map);
	// style
	this.button.style.width = '98px';
	this.button.style.height = '18px';
	this.button.style.marginLeft = '12px';
	this.button.style.marginRight = '12px';
	this.button.style.marginBottom = '10px';
	this.button.style.textAlign = 'center';
	$(this.button).text('About');
	$(this.button).addClass('ui-selectee');
	
	// event handlers
	var context = this;
	$(this.button).bind('click', function(event) {
		if(!tutorialRunning()) {
			if(!$(context.button).hasClass('ui-selected')) {
				$(context.button).addClass('ui-selected');
				$(context.window.window).show();
				$(context.window.window).effect('size', {
					to: {
						width: $("#map_canvas").width() * 4/5,
						height: $("#map_canvas").height() * 3/4
					},
					origin: ['top', 'left'],
					scale: 'box'
					}, 250);
					context.window.window.style.padding = '8px';
				}
				else {
					$(context.window.window).effect('size', {
						to: {
							width: 0,
							height: 0
						},
						origin: ['right', 'bottom'],
						scale: 'box'
					}, 250, function() {
						$(event.currentTarget).removeClass('ui-selected');
						$(context.window.window).hide();
					});
				}
		}
	});
	
	this.button.index = 1;
}

function AboutWindow(map) {
	// initialize component divs
	this.map = map;
	this.window = document.createElement('div');
	// stylin'
	this.window.style.position = 'absolute';
	$(this.window).hide();
	$(this.window).html("Mission: Empower People with Meaningful Insights Learned from Data" +
	"<p>Given that three of your friends have flu-like symptoms, and that you have recently met eight people, possibly strangers, who complained about having runny noses and headaches, what is the probability that you will soon become ill as well?</p>" +
	"<p>This app enables you to see the spread of infectious diseases, such as flu, throughout a real-life population.</p>" +
	"<p>We apply machine learning and natural language understanding techniques to determine the health state of Twitter users.</p>" +
	"<p>Since a large fraction of tweets is geo-tagged, we can plot them on a map, and observe how sick and healthy people interact. Our model then predicts if and when an individual will fall ill with high accuracy, thereby improving our understanding of the emergence of global epidemics from people's day-to-day interactions.</p>" +
	"<p>The fine-grained epidemiological models we show here are just one instance of the general class of problems that our system solves. Other domains include understanding of the public sentiment around your company or products, the diffusion of information throughout a population, and predicting customer behavior.</p>" +
	"<p>By augmenting existing datasets with real-time insights and cues from social media, we are able to connect the dots, visualize patterns, and refine models based on user feedback.</p>" +
	"<p>To learn more about our models, visit <a href='http://www.cs.rochester.edu/~sadilek/research/'>this website</a>.</p>" +
	"The Team" +
	"<ul><li>Adam Sadilek</li><li>Andrew Abumoussa</li><li>Sean Brennan</li><li>Martin Janda</li><li>Hao Chen</li></ul>" +
	"Contact" +
	"<p>Adam Sadilek: sadilek@cs.rochester.edu</p>");
	this.window.style.width = '0px';
	this.window.style.height = '0px';
	this.window.style.overflow = 'auto';
	this.window.style.marginLeft = $("#map_canvas").width() * 1/10 + 'px';
	$(this.window).addClass('ui-selectee');
}