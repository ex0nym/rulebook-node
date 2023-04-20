$(document).ready(function(){
	waitForEmailConfirmation();

});

function waitForEmailConfirmation(){
	$.ajax({
		method: "POST",
		url: "account",
		mimeType : "application/json",
		data: JSON.stringify({waiting:"waiting"}),
		success: function(json){
			if (json.error){
				ui.toast(json.error);

			} else if (json.forward){
				console.log("forwarding " + json.forward);
				window.location = json.forward;

			} else {
				throw "Unhandled Exception " + json;

			}
		}
	});
}