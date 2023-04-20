
var hostTransferSource = {
	bind: function(){
		$("#transferSourceBeginTransferBtn").click(hostTransferSource.beginTrasferBtn);

	},

	beginTrasferBtn: function(){
		var transferUrl = $("#transferSourceUrl").val();
		var network = $("#transferSourceNetwork").val();
		var msg = "Are you sure you want to transfer " + network + " using " + transferUrl + "?";
		ui.userConfirm("Transfer Source", msg, function(){
		
			var cmdData = {
				"network":network,
				"url":transferUrl,
				"command":"hostTransferSource"
			};
			general.serverRequest(cmdData, hostTransferSource.beginTrasfer, general.requestError);
			
		});
	},

	beginTrasfer: function(json){
		console.log(json);

	}
}

var hostingRecovery = {
	bind: function(){
		$("#hostingRecoverySetupRecoveryBtn").click(hostingRecovery.setup);
		$("#hostingRecoveryReset").click(hostingRecovery.reset);

	},

	reset: () => {
		var msg = "Resetting this XNode cannot be undone and will invalidate all Membership and any Tokens created under this Membership";
		var p = $("#hostingRecoveryPassword").val();
		if (p!=undefined && p.length > 6){
			ui.userConfirm("RESET XNODE", msg, function(){		
				var cmdData = {
					command:"reset",
	
				};
				general.serverRequest(cmdData, hostingRecovery.resetSalt, general.requestError);
				
			});
		} else {
			ui.toast("You must enter a password");

		}
	},

	resetSalt: (json) => {
		const f = {
			resetSuccess: (event) => {
				ui.toast("XNode Reset - you must restart the server");

			}, 

			resetFail: (error) => {
				ui.toast(error.error);

			}
		}
		if (json.salt){
			var p = $("#hostingRecoveryPassword").val();
			$("#hostingRecoveryPassword").val("");
			var raw = p + json.salt;
			crypt.sha256(raw).then(function(password){
				
				$.ajax({
					method: "POST", 
					url: "cp",
					mimeType: "text/html",
					data: JSON.stringify({command:"reset1",password:password}),
					success: f.resetSuccess, 
					error: f.resetFail

				});
			});			
		} else {
			throw "No salt failure";

		}
	}, 

	setup: function(){
		general.serverRequest({command:"setupRecovery0"}, hostingRecovery.gotSalt, general.requestError);

	}, 

	gotSalt: function(json){
		if (json.salt){
			var p = $("#hostingRecoveryPassword").val();
			$("#hostingRecoveryPassword").val("");
			var raw = p + json.salt;
			crypt.sha256(raw).then(function(password){
				
				$.ajax({
					method: "POST", 
					url: "cp",
					mimeType: "text/html",
					data: JSON.stringify({command:"setupRecovery1",password:password}),
					success: hostingRecovery.pdf, 
					error: hostingRecovery.pdfFail


				});
			});			
		} else {
			throw "No salt failure";

		}
	}, 

	pdf: function(html){
		var newDoc = document.open("text/html", "replace");
		newDoc.write(html);
		newDoc.close();
		

	},

	pdfFail: function(data){
		console.log(data);
		ui.toast(data.responseText);

	}
}

var hostAdminAccess = {
	bind: function(){
		$("#adminAccessAddEditBtn").click(hostAdminAccess.addEditBtn);
		$("#adminAccessAddApiKey").click(hostAdminAccess.createApiKey);

	},

	createApiKey: () => {
		general.serverRequest({command:"addApiKey"}, 
			hostAdminAccess.apiKeyCreated, general.requestError);

	}, 

	apiKeyCreated: (json) => {
		$("#adminAccessApiKeyOutput").removeClass("invisible");
		$("#adminAccessApiKeyOutputCopyField").html(JSON.stringify(json));
		hostAdminAccess.populateAdministrators();
		ui.toast("API Keys are not stored and cannot be recalled");

	},

	populateAdministrators: function(){
		console.log("Calling for Administrators");
		general.serverRequest({command:"getAdministrators"}, 
			hostAdminAccess.gotAdministrators, general.requestError);

	}, 

	gotAdministrators: function(json){
		var admins = json.administrators
    	console.log(admins);
		var items = [];
		var administratorNumbers = {};
    
	    for (var a in admins){
	    	administratorNumbers[admins[a].id + "Lbl"] = admins[a].tel;
	      
				var item = {
					id: admins[a].id,
					name: admins[a].username,
					clazz: admins[a].clazz

				}
				items.push(item);      
    	
		}
		console.log(JSON.stringify(administratorNumbers));
		sessionStorage.setItem("administrators", JSON.stringify(administratorNumbers));

		general.populateList({
			list:"#adminAccessAdministratorAccounts",
			items:items

		});
		// $("#adminAccessAdministratorAccounts > .list-line" ).click(general.multiLineVisualFeatures);
		// $("#adminAccessAdministratorAccounts > .list-line > .list-btn").click(general.bespokeList);

	},

	administratorListItemSelect: function(e){
		var select = $(e.target);
		
		var admins = JSON.parse(sessionStorage.getItem("administrators"));
		console.log(admins);
		console.log("Item Selected " + e.target.id + " "+  admins[e.target.id]);
		var tel = admins[e.target.id];
		
		if (select.hasClass("selected")){
			$("#adminAccessAddEditBtn").html("edit");
			$("#adminAccessUsername").val(select.html());
			$("#adminAccessTel").val((tel===undefined ? "" : tel));
			
		} else {
			$("#adminAccessAddEditBtn").html("add");	
			$("#adminAccessUsername").val("");
			$("#adminAccessTel").val("");
			
		}		
	},

	addEditBtn: function(e){
		
		if ($(e.target).html()==="Add"){
			hostAdminAccess.addBtn();
			
		} else if ($(e.target).html()==="Edit"){
			hostAdminAccess.editBtn();

		} else {
			throw "Unknown - button status "  + $(e.target).html();
			
		}		
	}, 

	addBtn: function(){
		var username = $("#adminAccessUsername").val();
		console.log(username);
		var cmdData = {
			"username":username,
			"command":"adminAccessAdministratorAccountsAdd"

		};
		general.serverRequest(cmdData, hostAdminAccess.add, general.requestError);
		$("#adminAccessUsername").val("");

	}, 
	
	

	add: function(json){
		if (json.message!==undefined){
			ui.toast(json.message);

		}
		$("#adminAccessApiKeyOutput").removeClass("invisible");
		$("#adminAccessApiKeyOutputCopyField").html(JSON.stringify(json));
		hostAdminAccess.populateAdministrators();

	},

	editBtn: function(){
		var originalEmailLine = $("#adminAccessAdministratorAccounts > .list-line .selected");

		var cmdData = {
			"originalEmail":originalEmailLine.html(),
			"email":$("#adminAccessUsername").val(),
			"command":"adminAccessAdministratorAccountsEdit"

		};
		if (cmdData.originalEmail===cmdData.email){
			ui.toast("The email address has not changed");

		} else {
			general.serverRequest(cmdData, hostAdminAccess.edit, general.requestError);

		}
	},

	edit: function(json){
		console.log(json);
		
	}
}