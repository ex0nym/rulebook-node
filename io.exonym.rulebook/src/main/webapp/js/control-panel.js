// JavaScript Document

{ 
	$(document).ready(function(){
		general.bind();
		source.bind();
		sourceSecondaryNetworks.bind();
		sourceNewNetwork.bind();
		sourceNodeManagement.bind();
		nodeSourceAttachment.bind();
		nodeMembership.bind();
		nodeSecondaryNetworks.bind();
		nodeTokenManagement.bind();
		hostTransferSource.bind();
		hostingRecovery.bind();
		hostAdminAccess.bind();
		access.init();		

	});

	var access = {
		init: function(){
			jQuery.get({
				url:"authenticate",
				mimeType: "application/json",
				success: function(json){
					console.log(json);
					if (json.auth === 1 || json.auth === 0){
						access.privileges(json.auth);

					} else {
						window.location = "logon.html";

					}
				}
			});
		},

		// return 1 if primary admin, 0 if admin and -1 if member
		privileges: function(flag){
			if (flag===-1){
				access.member();

			} else if (flag===0){
				access.admin();

			} else if (flag===1){
				access.primaryAdmin();

			} else {
				throw "Server Error - Privileges";

			}
		},

		primaryAdmin: function(){
			$("#sourceTab").addClass("hidden");
			$("#nodeTab").addClass("hidden");
			$("#hostingTab").addClass("active");
			$("#hosting").addClass("active show");
			$("#x-tabs").addClass("hidden");
			hostAdminAccess.populateAdministrators();

		},

		admin: function(){
			$("#nodeTab").addClass("active");
			$("#node").addClass("active show");
			$("#hostUpperRow").addClass("hidden");
			$("#hostLowerRow").addClass("hidden");
			$("#x-tabs").removeClass("hidden");
			general.populateNetworks();

		}, 

		member: function(){
			console.log("Redirect to personal details management page");

		}
	}

	var general = {
		bind: function(){
			$(".close-btn").click(general.close);
			$(".nav-btn").click(general.nav);
			$(".network-source-select").change(general.networkSource);
			$("#trans").click(general.transparentLayer);			
			$(".copy-field").click(general.copyBtn);
			$(".search-btn").click(general.search);
			$(".help-btn").click(general.help);
			// $(".list-line").click(general.multiLineVisualFeatures);
			// $(".list-btn").click(general.bespokeList);

		}, 

		close: function(e){
			var nextTabId = $(".nav-link.active").attr("href");
			var currentTab = $(".active.tab-pane");
			currentTab.removeClass("active");
			currentTab.removeClass("show");
			$(nextTabId).addClass("active");
			$(nextTabId).addClass("show");			

		},

		nav: function(e){
			var button = $(e.target).closest(".nav-btn");
			var show = $(button).attr("href");
			var selectedTab = $(".active.tab-pane");
			console.log(show);

			if (show==="#exit"){
				sessionStorage.clear();
				general.logoff();

			} else {
				$(selectedTab).removeClass("active");
				$(selectedTab).removeClass("show");
				$(show).addClass("active");
				$(show).addClass("show");

			}
		},

		forceNav: function(screen){
			var selectedTab = $(".active.tab-pane");
			console.log(screen);
			$(selectedTab).removeClass("active");
			$(selectedTab).removeClass("show");
			$(screen).addClass("active");
			$(screen).addClass("show");

		},

		logoff: function(){
			$.ajax({
				type: "POST",
				url:"authenticate",
				mimeType: "application/json",
				data: {cmd:"logoff"},
				success: function(json){
					console.log(json);
					if (json.complete){
						window.location = "";

					} else {
						window.location = "";
						throw "Error during logoff";

					}
				}
			});
		},

		search: function(e){
			var cmd = e.target.id;
			var members = JSON.parse(sessionStorage.getItem("members"));

			if (cmd==="searchNodeTokenManagementPresentationToken"){
				nodeTokenManagement.searchByToken();
				
			} else if (cmd==="searchNodeMembershipUsername"){
				var username = $("#nodeMembershipUsername").val();
				nodeMembership.searchByUsername(username, members);

			} else if (cmd==="searchNodeMembershipEmail"){
				var email = $("#nodeMembershipEmail").val();
				nodeMembership.searchByEmail(email, members);

			} else if (cmd==="searchNodeMembershipTelephone"){
				var telephone = $("#nodeMembershipTelephone").val();
				nodeMembership.searchByTelephone(telephone, members);

			} else{
				throw "Unknown Search Command " + cmd;

			}
		},
		searchResults: function(json){
			console.log(json);

		},

		serverRequest: function(cmd, success, failure){
			$("#working").removeClass("hidden");
			console.log("General Server Request " + JSON.stringify(cmd));
			var endPoint = "cp";

			$.ajax({type: "POST", 
				url: endPoint, 
				mimeType: "application/json",
				data: JSON.stringify(cmd),
				success: function(json){
					if (json.forward){
						$("#working").addClass("hidden");
						window.location = json.forward;

					} else if (json.error){
						$("#working").addClass("hidden");
						if (failure){
							failure(json);

						}
					} else {
						$("#working").addClass("hidden");
						if (success){
							success(json);

						}
					}
				},  
				error: failure					
			});
		},

		help: function(e){
			$("#helpPage").removeClass("hidden");
			setTimeout(function(){
				$("#trans").removeClass("grow");
			}, 100);	
			var helpBtn = e.target.id;
			$("#helpTitle").html(general.helpTitle[helpBtn]);
			$("#helpText").html(general.helpText[helpBtn]);

		},

		transparentLayer: function(){
			$("#trans").addClass("grow");
			setTimeout(function(){
				$("#helpPage").addClass("hidden");
			}, 300);
		}, 

		populateList: function(json){
			var items = "";
			var listItems = json.items;
			for (const item in listItems){
				var i = listItems[item]
				var line = "<div class=\"list-line\">";
				line += "<a id=\"" + i.id + "Lbl\" href=\"#\" class=\"list-label-item\">"+ i.name +"</a>";
				line += "<a id=\"" + i.id + "\" href=\"#\" class=\"list-btn " + i.clazz + " hidden\"></a>";
				line += "</div>";
				items += line;
				console.log(line);

			}
			console.log("Complete List is " + json.list);
			console.log(json);
			$(json.list).html(items);
			$(json.list).find(".list-line").click(general.multiLineVisualFeatures);
			$(json.list).find(".list-btn").click(general.bespokeList);
			

		}, 

		appendItemToList: function(json){
			var items = $(json.list).html();
			
			var line = "<div class=\"list-line\">";
			line += "<a id=\"" + json.id + "Lbl\" href=\"#\" class=\"list-label-item\">"+ json.name +"</a>";
			line += "<a id=\"" + json.id + "\" href=\"#\" class=\"list-btn " + json.clazz + " hidden\"></a>";
			line += "</div>";

			items += line; 

			$(json.list).html(items);

			// Complete Bindings for newly added values.
			$("#" + json.id + " > .list-line" ).click(general.multiLineVisualFeatures);
			$("#" + json.id + " > .list-btn").click(general.bespokeList);

		}, 		

		multiLineVisualFeatures: function(e){
			if (e!=null){
				var thisId = e.target.id;
				

				if (thisId!=null && thisId!==""){
					var control = e.target.parentElement.parentElement.id;
					console.log(control, thisId);

					if (control!=null && control!==''){
						
						if ($("#" + thisId).hasClass("selected")){
							$("#" + control + " > .list-line > .list-label-item").removeClass("selected");
							$("#" + control + " > .list-line > .list-btn").addClass("hidden");

						} else if ($(e.target).hasClass("list-btn")!=true){
							// Remove Previously Selected in this Control and Hide any Button
							$("#" + control + " > .list-line > .list-label-item").removeClass("selected");
							$("#" + control + " > .list-line > .list-btn").addClass("hidden");
							
							if ($(e.target).hasClass("list-label-item")){
								// Show the button and mark as selected.
								$(e.target).addClass("selected");
								
							}
							if (e.target.nextElementSibling!=null){
								$(e.target.nextElementSibling).removeClass("hidden");

							}
						} else {
							throw "Multi-Line Button - Not 'selected', 'list-btn', 'list-label-item'";
							// delete or re-issue button selected
							
						}
					}
				}
			}
		},

		copyBtn: function(e){
			var $temp = $("<input>");
			var textField = $(e.target.parentElement).find(".cp-text");
			$("body").append($temp);
			$temp.val($(textField).html()).select();
			document.execCommand("copy");
			$temp.remove();
			ui.toast("Copied to Clipboard");

		},

		populateNetworks(){
			if (!general.networkSourceBusy){
				general.networkSourceBusy = true;
				general.serverRequest({command:"getNetworks"}, general.gotNetworks, general.requestError);

			} 
		},

		networkSource: function(e) {
			if (!general.networkSourceBusy){
				general.networkSourceBusy = true;
				var id = $(this).find("option:selected").attr("value");
				var cmdData = {
					"command":"getNetworks",
					"networkName":id
	
				}
				general.serverRequest(cmdData, general.gotNetworks, general.requestError);
	
			}
		},	
		
		networkSourceBusy: false,

		hideSourceNavIcons: function(){
			$("#sourceIconsUpperRow").addClass("hidden");
			//$("#srcSecondaryNetworksBtn").addClass("hidden");

		},

		hideNodeNavIcons: function(){
			$("#nodeUpperRow").addClass("hidden");
			// $("#nodeMiddleRow").addClass("hidden");
			// $("#nodeSecondaryNetworksBtn").addClass("hidden");

		},

		showSourceNavIcons: function(){
			$("#sourceIconsUpperRow").removeClass("hidden");
			//$("#srcSecondaryNetworksBtn").removeClass("hidden");

		}, 		
		
		showNodeNavIcons: function(){
			// $("#nodeSecondaryNetworksBtn").removeClass("hidden");
			$("#nodeUpperRow").removeClass("hidden");
			// $("#nodeMiddleRow").removeClass("hidden");
	
		}, 

		selectedNetwork: "",

		gotNetworks: function(networks){
			const f = {
				sources: () => {
					general.showSourceNavIcons();
					f.computeSelectedNetwork();
					f.comboContents();

				}, 

				noSources: () => {
					general.hideSourceNavIcons();
					general.hideNodeNavIcons();
					general.networkSourceBusy = false;
					
				},				

				computeSelectedNetwork: () => {
					var network = null; 
					if (networks.selectedNetwork){
						general.selectedNetwork = networks.selectedNetwork;
						network = networks.networkData[networks.selectedNetwork];
						source.networkSelected(network);
	
					} else {
						network = networks.networkData[networks.networkList[0]];
						if (network){
							general.selectedNetwork = network.networkName;
							console.log("1) " + general.selectedNetwork);
							source.networkSelected(network);
		
						} else {
							console.log("1) No Network Selection - No Source");
	
						}
					}					
				}, 

				comboContents: () => {
					var nets = networks.networkList;
					var html = "";
					for (var net in nets){
						html += "<option value=\""+ nets[net] +"\">"+ nets[net] +"</option>";
					
					}
					var networkCombos = $(".network-source-select");
					for (var combo in networkCombos){
						var selector = $("#" + networkCombos[combo].id);
						
						if (selector.attr("class")==="network-source-select"){
							selector.html(html);
						
						}
						selector.val(general.selectedNetwork);

					}					
				}, 

				attachNetwork: () => {
					if (networks.attached){
						nodeSourceAttachment.hideSetupFields(networks.attached);
		
					} else {
						general.hideNodeNavIcons();
		
					}	
				}, 

				completeRequest: () => {
					if (networks.quickList){
						general.serverRequest({command:"fullNetworkDataRequest"}, general.gotSourceDataPackage, general.error);
		
					} else {
						nodeMembership.gotMembers(networks.members);		
						general.networkSourceBusy = false;
		
					}					
				}
			}
			//
			//  DRIVER
			//
			if (networks.noSources){
				f.noSources();

			} else {
				f.sources();

			}
			f.attachNetwork();
			f.completeRequest();

		},

		gotSourceDataPackage: function(networks){
			// sessionStorage.setItem("networks", JSON.stringify(networks));
			if (general.selectedNetwork){
				console.log("2) " + general.selectedNetwork);
				var network = networks.networkData[general.selectedNetwork];
				source.networkSelected(network);
	
			} else {
				console.log("No source");
				general.hideSourceNavIcons();

			}
			if (networks.members){
				nodeMembership.gotMembers(networks.members);

			} else {
				console.log("No members");

			}
			general.networkSourceBusy = false;

		},

		bespokeList: function(e){
			var buttonCmd = {
				"selectedId": e.target.id,
				"multiId": e.target.parentElement.parentElement.id,
				"selectedValue": $(e.target.previousElementSibling).html(),
				"selectedNetwork":general.selectedNetwork
				
			}
			var title = "";
			var message = "";
			if ($(e.target).hasClass("list-button-delete")){
				buttonCmd["command"] = buttonCmd.multiId + "Delete";
				title = "Delete";				
				message = "Are you sure you want to delete " + buttonCmd.selectedValue;
				ui.userConfirm(title, message, function(){
					general.serverRequest(buttonCmd, general.deleteItemSuccess, general.requestError);

				});
			} else if ($(e.target).hasClass("list-button-issue")){
				buttonCmd["command"] = "nodeMemberReissue";
				title = "Re-Issue";				
				message = "Are you sure you want to re-issue to " + buttonCmd.selectedValue;
				ui.userConfirm(title, message, function(){
					general.serverRequest(buttonCmd, general.reissueSuccess, general.requestError);

				});				
			} else if ($(e.target).hasClass("list-button-add")){
				general.moveToRequired(e.target);
			
			} else if ($(e.target).hasClass("list-button-remove")){
				general.moveToAvailable(e.target);
			
			} else {
				throw "Unrecognized Button type " + e.target.classList;
				
			}			
		}, 

		deleteItemSuccess: function(json){
			console.log("deleteItemSuccess" + JSON.stringify(json));
			general.refresh(json.refresh);
			general.networkSource();

		},

		reissueSuccess: function(json){
			console.log(json);
			ui.toast("Reissued Successfully");

		},

		refresh: function(list){
			if (list){
				if (list==="administrators"){
					hostAdminAccess.populateAdministrators();
	
				} else {
					throw "No list implemented to refresh - list=" + list;
	
				}
			} else {
				console.log("There was no list refresh specified in json.refresh");
				
			}
		},

		requestError: function(json){
			if (json.error){
				ui.toast(json.error);

			} else {
				throw JSON.stringify(json);

			}
		},

		moveToRequired: function(item){
			$(item).removeClass("list-button-add");
			$(item).addClass("list-button-remove");
			$(item).addClass("hidden");
			$(item.previousElementSibling).removeClass("selected");
			$(item.parentElement).appendTo("#sourceSecondaryNetworksRequiredNodes");			

		},

		moveToAvailable: function(item){
			$(item).removeClass("list-button-remove");
			$(item).addClass("list-button-add");
			$(item).addClass("hidden");
			$(item.previousElementSibling).removeClass("selected");			
			$(item.parentElement).appendTo("#sourceSecondaryNetworksAvailableNodes");			

		},

		helpTitle: {
			"hlpSourceNewNetworkRulebookURL":"Rulebook URL",
			"hlpAdminAccessAdministratorAccounts":"Administrator Accounts",
			"hlpTransferSourceUrl":"URL",
			"hlpNodeSecondaryNetworksIssuerRecipientUrl":"Issuer / Recipient URL",
			"hlpNodeSourceAttachmentPublicName":"Public Name",
			"hlpNodeSourceAttachmentSourceUrl":"Lead URL",
			"hlpNodeTokenManagementPresentationToken":"Presentation Tokens",
			"hlpNodeMembershipAllTimeMembership":"All-Time Membership",
			"hlpNodeMembershipUsername":"Username", 
			"hlpSourceNewNetworkNetworkName":"Network Name",
			"hlpSourceNodeInformationDefaultPlatform":"Default Platforms",
			"hlpSourceNetworkInformationSourceUrl":"Lead URL",
			"hlpSourceNodeManagementAuthorizedNodes":"Authorized Nodes",
			"hlpSourceNodeManagementNodeUrl":"Node Url",
			"hlpSourceSecondaryNetworksSecondaryNetwork":"Secondary Networks"

		}, 
		
		helpText: {
			  "hlpSourceNewNetworkRulebookURL": "<p>To add a rulebook URL, follow these steps:</p><p>1. Ensure you have a valid rulebook document in JSON format. If you do not have one, you can create it using the guidelines in the documentation.</p><p>2. Check if another Moderator offers the same rulebook. If so, obtain the URL of their rulebook document.</p><p>3. If you authored the rulebook, publish it on your website or any platform that allows file sharing. Ensure it's in JSON format and accessible via a URL.</p><p>4. Once you have the URL, enter it in the 'Rulebook URL' field.</p><p>Note: If the URL is invalid or the rulebook is not in JSON format, it cannot be added. Verify that the URL is correct and the rulebook is in the required format before proceeding.</p>. Once completed, use the steps above to add the rulebook URL.</p>",
			  
		  
			  "hlpAdminAccessAdministratorAccounts": "<p>Adding an Administrator Account grants 'Assign' privileges to a trusted administrator of this Moderator.</p><p>Any user with 'Assign' privileges can grant/revoke membership in this Trust Network.</p><p>To add an administrator, enter the username and click Add.</p><p>The Super Administrator will then email the new Administrator with instructions.</p><p>Upon first login, the Administrator will be prompted to change their password.</p><p>The 'Primary Administrator' (the first added) can only:<ul><li>Add/Remove Secondary Administrators</li><li>Create Recovery Files</li><li>Transfer a Lead</li></ul></p><p>Secondary Administrators* can perform the remaining actions.</p>",
			  
			  "hlpTransferSourceUrl": "<p>Transfers the Lead to/from another Rulebook Node.</p><p>To transfer the Lead out (donor), enter the URL of the Destination Host and click Begin Transfer.</p><p>To transfer the Lead in (recipient), enter the Donor URL and click BEGIN TRANSFER.</p><p>It doesn’t matter which Moderator initiates the transfer first; however, both must request the transfer for it to succeed.</p><ul><li>Transfers must be completed within 24 hours of the first request to succeed.</li><li>The Lead will either succeed or fail without interrupting the Trust Network.</li></ul>",
				  
			  "hlpModeratorSourceAttachmentPublicName": "<p>Moderators are publicly identifiable, and the Public Name will be part of this Moderator’s Public Identifier.</p><p>The Moderator cannot be renamed.</p><p>The Public Name cannot contain spaces or special characters except hyphens.</p>",
			  
			  "hlpModeratorSourceAttachmentSourceUrl": "<p>Enter the current URL of the Lead. If the Lead is later transferred, this will be updated automatically.</p><p>Only one Lead can be attached to a Moderator.</p><p>To disconnect from a Lead, the Moderator must be reinstalled, which wipes all keys and membership.</p>",
			  
			  "hlpModeratorTokenManagementPresentationToken": "<p>Drag & drop or copy & paste the Presentation Token or a link to it.</p><p>Selecting Revoke will invalidate all the claims the peer has made since they joined.</p><p>To find the contact details linked to a token, use the Search button.</p>",
					  
			  "hlpSourceNewNetworkNetworkName": "<p>Define a unique name for this Trust Network on this domain.</p><p>The name only needs to be unique on this Node, and any Node where it may later be transferred.</p><p>The name cannot contain spaces or special characters, except for hyphens.</p><p>When the network is created, the supporting files will be located at:</p><p>[this domain]/static/lead/</p><p>Every Moderator on the Trust Network will include this name in the URL.</p>",
			  
			  "hlpSourceNetworkInformationSourceUrl": "<p>The Lead URL is required to add Moderators to the network.</p><p>To add a Moderator, send them this URL. They will reply with their Moderator URL.</p><p>Confirm the Moderator by pasting their Moderator-URL into the “Lead~Moderator Management” screen.</p><p>Both the Moderator and Lead URLs can be shared publicly.</p>",
			  
			  "hlpSourceModeratorManagementAuthorizedModerators": "<p>Moderators grant and revoke membership within the Trust Network.</p><p>This Lead grants and revokes privileges for Moderators, allowing the formation of the Trust Network.</p><p>If a Moderator has granted membership to Peers and that Moderator is removed from the network, those Peers are no longer part of the Trust Network. It's good practice to migrate Peers before removing Moderators.</p>",
			  
			  "hlpSourceModeratorManagementModeratorUrl": "<p>To add a Moderator, enter the Moderator-URL, optionally name it, and click Add. To add multiple Moderators, separate the URLs with commas.</p><p>To remove a Moderator, enter the Moderator-URL and click Remove.</p>",
			  }	
	}

}