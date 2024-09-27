var nodeSourceAttachment = {
	bind: function(){
		$("#nodeSourceAttachBtn").click(nodeSourceAttachment.attachBtn);

	}, 

	attachBtn: function(){
		var publicName = $("#nodeSourceAttachmentPublicName").val();
		var sourceUrl = $("#nodeSourceAttachmentSourceUrl").val();
		var cmdData = {
			"command":"nodeSourceAttachmentAttach",
			"publicName":publicName,
			"sourceUrl":sourceUrl
			
		}
		general.serverRequest(cmdData, nodeSourceAttachment.attachResult, general.requestError);
		$("#progress").removeClass("hidden");

	},

	attachResult: function(json){
		console.log(json.update);

		if (json.update){
			$("#progressTxt").html(json.update);
			general.serverRequest({command:"progress"}, nodeSourceAttachment.attachResult, nodeSourceAttachment.attachResult);

		} else {
			if (json.complete){
				console.log(json.complete);
				nodeSourceAttachment.hideSetupFields(json.complete);
				general.showNodeNavIcons();
				$("#progress").addClass("hidden");
				$("#working").addClass("hidden");
	
			} else if (json.error){
				ui.toast(json.error);				
				$("#progress").addClass("hidden");
				$("#working").addClass("hidden");

			} else {
				$("#progress").addClass("hidden");
				$("#working").addClass("hidden");				
				ui.toast("Lost contact with Rulebook Node");
				throw "Error: Expected a nodeUrl " + json; 
	
			}
		}
	},

	hideSetupFields: function(nodeUrl){
		if (nodeUrl){
			$("#nodeSourceAttachmentNodeAddress").html(nodeUrl);
			$("#nodeSourceAttachmentPublicName").addClass("hidden");
			$("#hlpNodeSourceAttachmentPublicName").addClass("hidden");
			$("#nodeSourceAttachmentSourceUrl").addClass("hidden");
			$("#hlpNodeSourceAttachmentSourceUrl").addClass("hidden");
			$("#nodeSourceAttachmentForm > .box > .x-form > .help > .lbl").addClass("hidden");
			$("#nodeSourceAttachmentForm > .box > .x-form > input").addClass("hidden");
			$("#nodeSourceAttachmentForm > .box > .btn-footer").addClass("hidden");
	
		} else {
			throw "No Node URL " + nodeUrl;

		}
	}
}

var nodeTokenManagement = {
	bind: function(){
		$("#nodeTokenManagementRevokeBtn").click(nodeTokenManagement.revoke);
		$("#nodeTokenManagementPresentationToken").on('dragenter', nodeTokenManagement.preventDefaults);
		$("#nodeTokenManagementPresentationToken").on('dragover', nodeTokenManagement.preventDefaults);
		$("#nodeTokenManagementPresentationToken").on('drop', nodeTokenManagement.drop);
		$(document).on('dragenter', nodeTokenManagement.preventDefaults);
		$(document).on('dragover', nodeTokenManagement.preventDefaults);
		$(document).on('drop', nodeTokenManagement.preventDefaults);

	},

	revoke: () => {
		const f = {
			revokeSuccess: (json) => {
				if (json.success){
					ui.toast("The web user is no longer honest under this rulebook.");

				} else {
					console.log(json);

				}
			}, 

			revokeError: (json) => {
				ui.toast(json.error);
				console.log(json);

			}
		} 

		const value = $("#nodeTokenManagementPresentationToken").val();
		var command = {
			command:"nodeMemberRevoke"

		}
		if (value.length > 2){
			command["token"]=btoa(value);

		}
		general.serverRequest(command, f.revokeSuccess, f.revokeError);

	},

	searchByToken: () => {
		const f = {
			memberDetails: (json) => { 
				$("#nodeTokenManagementVerificationMessage").addClass("hidden");
				$("#nodeTokenManagementContact").removeClass("invisible");
				$("#nodeTokenManagementHint").addClass("hidden");
				$("#nodeTokenManagementEmail").html(json.email);
				$("#nodeTokenManagementTel").html(json.tel);

			}, 

			memberError: (json) => {
				ui.toast(json.error);

			}
		}

		const value = $("#nodeTokenManagementPresentationToken").val();
		var command = {
			command:"nodeMemberSearchFromToken"

		}
		if (value.length > 2){
			command["token"]=btoa(value);

		}
		general.serverRequest(command, f.memberDetails, f.memberError);

	},

	drop: function(e){		
		e.preventDefault();
		$("#nodeTokenManagementVerificationMessage").addClass("hidden");
		$("#nodeTokenManagementContact").addClass("invisible");		
		var files = e.originalEvent.dataTransfer.files;
		
		for (var i = 0; i < files.length; i++) {
			 var fd = new FormData();
			 fd.append('file', files[i]);	  
			 nodeTokenManagement.sendFileToServer(fd, status);
	  
		}		
	}, 

	sendFileToServer: function(formData, status){
		var jqXHR=$.ajax({
				xhr: function() {
				var xhrobj = $.ajaxSettings.xhr();
				if (xhrobj.upload) {
					xhrobj.upload.addEventListener('progress', function(event) {
						var percent = 0;
						var position = event.loaded || event.position;
						var total = event.total;
						if (event.lengthComputable) {
							percent = Math.ceil(position / total * 100);

						}
						//Set progress
						status.setProgress(percent);
					}, false);
				}
				return xhrobj;
			},
			url: "cp",
			type: "POST",
			contentType:false,
			processData: false,
			cache: false,
			data: formData,
			success: function(data){
				$("#nodeTokenManagementVerificationMessage").removeClass("hidden");				
				status.setProgress(100);

			}
		}); 
	},

	preventDefaults: function(e){
		e.stopPropagation();
		e.preventDefault();

	}
}

var nodeSecondaryNetworks = {

	bind: function(){
		$("#nodeSecondaryNetworksJoinBtn").click(nodeSecondaryNetworks.joinBtn);
		$("#nodeSecondaryNetworksAddBtn").click(nodeSecondaryNetworks.addBtn);

	},

	addBtn: function(){
		var url = $("#nodeSecondaryNetworksIssuerRecipientUrl").val();
		var cmdData = {
			"url":url,
			"command":"secondary-add"
		}
		general.serverRequest(cmdData, nodeSecondaryNetworks.addMember, general.requestError);
		
	},

	addMember: function(json){
		console.log(json);

	}, 

	joinBtn: function(){
		var url = $("#nodeSecondaryNetworksIssuerRecipientUrl").val();
		var cmdData = {
			"url":url,
			"command":"secondary-join"

		}
		general.serverRequest(cmdData, nodeSecondaryNetworks.joinNetwork, general.requestError);
			
	},

	joinNetwork: function(json){
		console.log(json);
		ui.toast("The Issuer has 24H to issue the Credential");

	}
}

var nodeMembership = {
	bind: function(){
		$("#nodeMembershipAllTimeMembership > .list-line").click(nodeMembership.allTimeMembershipList);
		$("#nodeMembershipAddEditBtn").click(nodeMembership.addEditBtn);

	},

	gotMembers: function(members){
		var items = [];
		var mString = JSON.stringify(members);
		sessionStorage.setItem("members", mString);
		console.log("Stored Members List=" + mString);

		for (m in members){
			var member = members[m];
			var item = {
				id: member.id,
				name: member.username,
				clazz: "list-button-issue"

			}
			items.push(item);

		}
		general.populateList({
			list:"#nodeMembershipAllTimeMembership",
			items:items

		});				

	}, 

	allTimeMembershipList: function(e){
		var select = $(e.target);
		if (select.hasClass("selected")){
			$("#nodeMembershipAddEditBtn").html("add member");	
			$("#nodeMembershipUsername").val("");
			$("#nodeMembershipEmail").val("");
			$("#nodeMembershipTelephone").val("");
			
		} else {
			var memberData = select.html().split(", ");
			$("#nodeMembershipAddEditBtn").html("edit member");
			$("#nodeMembershipUsername").val(memberData[0]);
			$("#nodeMembershipEmail").val(memberData[1]);
			$("#nodeMembershipTelephone").val(memberData[2]);
			
		}
	},

	addEditBtn: function(e) {
		var btnText = $(e.target).html();
		if (btnText==="add member"){
			nodeMembership.addBtn();
			
		} else if (btnText==="edit member"){
			nodeMembership.editBtn();

		} else {
			throw "Unknown - button status " + btnText;
			
		}		
	},

	addBtn: function() {
		var username = $("#nodeMembershipUsername").val();
		var email = $("#nodeMembershipEmail").val();
		var tel = $("#nodeMembershipTelephone").val();
		
		if (username==="" || username===null){
			ui.toast("Username must be set");
			
		} else if (email==="" || email===null){
			ui.toast("You must enter a valid email addresss");
			
		} else {
			var cmdData = {
				"command":"nodeMembershipAdd",
				"username":username,
				"email":email,
				"tel":tel
				
			}
			general.serverRequest(cmdData, nodeMembership.addMember, general.requestError);
			$("#nodeMembershipUsername").val("");
			$("#nodeMembershipEmail").val("");
			$("#nodeMembershipTelephone").val("");

		}		
	}, 

	searchByTelephone: function(t, members){
		var result = {message:"Not Found"};
		for (m in members){
			if (members[m].tel===t){
				result = members[m];
				break; 

			}
		}
		nodeMembership.membersSearchResult(result);
				
	},

	searchByUsername: function(u, members){
		var result = {message:"Not Found"};
		for (m in members){
			if (members[m].username===u){
				result = members[m];
				break; 

			}
		}
		nodeMembership.membersSearchResult(result);

	}, 

	searchByEmail: function(e, members){
		var result = {message:"Not Found"};
		for (m in members){
			if (members[m].email===e){
				result = members[m];
				break; 

			}
		}
		nodeMembership.membersSearchResult(result);

	},

	addMember: function(json){
		ui.toast(json.message);
		general.populateNetworks();

	},

	membersSearchResult: function(result){
		if (result.message || "" + result === "undefined"){
			ui.toast("Not Found");

		} else {
			$("#nodeMembershipUsername").val(result.username);
			$("#nodeMembershipEmail").val(result.email);
			if (result.tel!==null){
				$("#nodeMembershipTelephone").val(result.tel);

			} else {
				console.log(result.tel + "=result.tel");

			}
		}
	},

	editBtn: function() {
		var originalLine = $("#nodeMembershipAllTimeMembership > .list-line .selected").html();
		var memberData = originalLine.split(", ");
		var username = $("#nodeMembershipUsername").val();
		var email = $("#nodeMembershipEmail").val();
		var tel = $("#nodeMembershipTelephone").val();
		
		var usernameChange = username !== memberData[0];
		var emailChange = email !== memberData[1];
		var telChange = tel !== memberData[2];
		
		if (usernameChange){
			$("#nodeMembershipUsername").val(memberData[0]);
			ui.toast("Username cannot be edited");
		
		} else if (emailChange || telChange){
			var msg = "<p>Are you sure you want to make the update(s);";
			
			if (emailChange){
				msg += "<p> '" + memberData[1]  + "' to '" + email + "'";
				
			} if (telChange){
				msg += "<p> '" + memberData[2]  + "' to '" + tel + "'";
				
			}
			
			userConfirm("Edit Member", msg, function(){
				var cmdData = {
					"command":"nodeMembershipEdit",
					"username":memberData[0],
					"emailPrevious":memberData[1],
					"telPrevious":memberData[2],
					"emailNew":email,
					"telNew":tel,
					"emailChange":emailChange,
					"telChange":telChange
				}
				general.serverRequest(cmdData, nodeMembership.editMember, general.requestError);
				
			});
		} else {
			ui.toast("To edit, you must update either Email or Telephone Number");
		
		}		
	},

	editMember: function(json){
		console.log(json);

	}
}