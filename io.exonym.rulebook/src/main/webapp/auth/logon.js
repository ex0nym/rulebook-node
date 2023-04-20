const msgEng = {
	PASS_STRENGTH_MSG: "Password is not strong enough",
	PASS_MATCH_MSG: "Passwords do not match",
	PASS_INVALID: "Password is incorrect"

}

$(document).ready(function(){
	logonPage.bind();
	$("#working").removeClass("hidden");

	jQuery.get({
		url:"authenticate",
		mimeType: "application/json",
		success: function(json){
			console.log(json);
			$("#working").addClass("hidden");
			if (json.auth === 1 || json.auth === 0){
				window.location = "control-panel.html";

			} else if (json.password === "reset"){
				register.bind();				

			} else {
				console.log("Not yet authorized");

			} 
		}, 
		failure: (e) => {
			$("#working").addClass("hidden");

		}});
	//*/
});


var logonPage = {
	bind: function(){
		$("#loginSignInBtn").click(logonPage.signIn);
		
	},

	setup: false,

	signIn: function(){
		var username = $("#loginUsername").val();
		var password = $("#loginPassword").val();
		var passwordConf = $("#loginRepeatPassword").val();

		if (logonPage.setup){
			// register
			var valid = register.isPasswordAcceptable({
					username:username, 
					password:password, 
					passwordConf:passwordConf, 
					strength:register.passwordStrength});
			if (valid){
				crypt.sha256(password).then((pwd)=>{

					logonPage.changePassword({ 
						cmd:"change-password",
						username:username, 
						password:pwd
	
					});

				});
			}
		} else if (password && username) {
			console.log("submit");
			logonPage.computeTokens(username, password);

		} else {
			throw "UNEXPECTED";

		}
	},

	changePassword: (json) => {
		
		$.ajax({
			method: "POST",
			url: "authenticate",
			mimeType: "application/json",
			data: json,
			success: logonPage.changePasswordSuccess,
			failure: (e) => {
			  $("#working").addClass("hidden");
			  console.error(e);

			}
		});
	},

	changePasswordSuccess: (data) => {
		window.location = "control-panel.html";

	},

	computeTokens: function(username, password){
		$("#working").removeClass("hidden");
		var f = {
			hash: (password) => {
				return crypt.sha256(password);

			}, 

			authenticate: (password) => {
				console.log(password);
				$.ajax({
					method: "POST",
					url: "authenticate",
					mimeType: "application/json",
					data:{
						cmd:"session", 
						username:username, 
						password:password
					},
					success: function(json){
						console.log(json);
						if (json.error){
							$("#working").addClass("hidden");
							ui.toast(json.error);
		
						} else if (json.pass){
							register.bind();
		
						} else if (json.success){
							window.location = "control-panel.html";
						
						} else {
							$("#working").addClass("hidden");
							throw "unexpected error: " + JSON.stringify(json);
		
						}
					}, failure: (e) => {
						console.log(e);
						$("#working").addClass("hidden");
		
					}
				});
			}		
		}
		f.hash(password)
			.then(f.authenticate);

	}, 
}

var register = {
	bind: function(){
		logonPage.setup=true;
		$("#working").addClass("hidden");
		var pop = $("#loginUsername").val();
		console.log(pop);
		$("#loginUsername").prop('disabled', true);
		$("#loginPassword").val("");
		$("#loginPassword").password(register.meterConfig);				
		$("#loginPassword").on('password.score', register.strength);
		register.makeVisible();

	},

	resetPrimaryAdmin: function(username){
		$("#loginUsername").val(username);
		$("#loginPassword").val("");
		$("#loginRepeatPassword").val("");
		logonPage.setup=true;
		register.bind();

	},

	passwordStrength: 0, 

	strength: function(e, score){
		$("#strength").val(score);
		register.passwordStrength = score;

		if (score > 50){
			$("#strength").addClass("strong");

		} else if (score > 33){
			$("#strength").addClass("medium");

		}
		if (score < 33){
			$("#strength").removeClass("strong");
			$("#strength").removeClass("medium");

		} else if (score < 50){
			$("#strength").removeClass("strong");

		}				
	}, 

	isPasswordAcceptable: function(c){
		if (c.strength < 50){
			ui.toast(msgEng.PASS_STRENGTH_MSG); 
			return false; 
			
		} else if (c.password===c.passwordConf){
			return true; 
			
		}  else {
			ui.toast(msgEng.PASS_MATCH_MSG); 
			return false; 
			
		}
	},

	meterConfig: {
		  showPercent: false,
		  showText: false, // shows the text tips
		  animate: true, // whether or not to animate the progress bar on input blur/focus
		  username: false, // select the username field (selector or jQuery instance) for better password checks
		  usernamePartialMatch: false, // whether to check for username partials
		  minimumLength: 4 // minimum password length (below this threshold, the score is 0)
	},

	makeVisible: function(){
		$("#strengthText").removeClass("invisible");
		$("#strength").removeClass("invisible");
		$("#loginRepeatPassword").removeClass("hidden");
		$("#loginRepeatPasswordLbl").removeClass("hidden");		
	}
} 