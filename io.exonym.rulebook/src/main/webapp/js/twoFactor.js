var change = false; 

$(document).ready(function(){
	twoFactor.bind();

	jQuery.get({
		method: "GET",
		url: "sms",
		mimeType: "application/json",
		success: function(json){
			console.log(json);
			if (json.code){
				twoFactor.change = true; 
				updateFields();
				setTimeout(twoFactor.fireTimeOut, 30000);
				
			} 				
		}
	});
});


var twoFactor = {

	bind: function(){
		$("#twoFactorContinueBtn").click(twoFactor.continue);
		$("#twoFactorNoSms").click(twoFactor.noSms);

	}, 

	change: false, 

	continue: function(){
		var tel = $("#twoFactorTelephone").val();
		var country = $("#twoFactorCountryCode").val();
		var sms = $("#twoFactorVerificationCode").val();
		if (sms){
			console.log("sms");
			twoFactor.processCode(sms);

		} else if (tel){
			console.log("tel");
			twoFactor.processSend(country, tel);

		}		
	}, 

	processSend: function(country, tel){
		$.ajax({
			method: "POST",
			url: "sms",
			mimeType: "application/json",
			data: JSON.stringify({country:country, tel:tel}),
			success: function(json){
				if (json.error){
					ui.toast(json.error);

				} else {
					twoFactor.updateFields();
					setTimeout(function(){fireTimeOut()}, 30000);

				}
			}
		});					
	}, 

	processCode: function(sms){
		var d;
		if (twoFactor.change){
			d = JSON.stringify({sms:sms, change:"y"});

		} else {
			d = JSON.stringify({sms:sms});

		}
		$.ajax({
			method: "POST",
			url: "sms",
			mimeType: "application/json",
			data: d,
			success: function(json){
				console.log(json)
				if (json.error){
					ui.toast(json.error);

				} else if (json.forward){
					window.location = json.forward;

				}
			}
		});		
	},

	noSms: function(){
		$.ajax({
			method: "POST",
			url: "sms",
			contentType: "application/json",
			data: JSON.stringify({sms:sms, nosms:"nosms"}),
			success: function(data){
				var json = JSON.parse(data);
				if (json.error){
					ui.toast(json.error);

				} else if (json.forward){
					window.location = json.forward;

				}
			}
		});		
	}, 

	updateFields: function(){
		$("#lblTwoFactorVerificationCode").removeClass("hidden");
		$("#twoFactorVerificationCode").removeClass("hidden");
		
		$("#lblTwoFactorTelephone").addClass("hidden");
		$("#twoFactorTelephone").addClass("hidden");

		$("#lblTwoFactorCountryCode").addClass("hidden");
		$("#twoFactorCountryCode").addClass("hidden");


	},
	fireTimeOut: function(){
		$("#resendHide").removeClass("hidden");

	}
}