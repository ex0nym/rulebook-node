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
			"hlpNodeSourceAttachmentSourceUrl":"Source URL",
			"hlpNodeTokenManagementPresentationToken":"Presentation Tokens",
			"hlpNodeMembershipAllTimeMembership":"All-Time Membership",
			"hlpNodeMembershipUsername":"Username", 
			"hlpSourceNewNetworkNetworkName":"Network Name",
			"hlpSourceNodeInformationDefaultPlatform":"Default Platforms",
			"hlpSourceNetworkInformationSourceUrl":"Source URL",
			"hlpSourceNodeManagementAuthorizedNodes":"Authorized Nodes",
			"hlpSourceNodeManagementNodeUrl":"Node Url",
			"hlpSourceSecondaryNetworksSecondaryNetwork":"Secondary Networks"

		}, 
		helpText: {
			"hlpSourceNewNetworkRulebookURL":"<p>To add a rulebook URL, follow the steps below:</p><p>1. Ensure that you have a valid rulebook document in JSON format. If you do not have a rulebook, you can create one using the guidelines provided at https://docs.exonym.io.</p><p>2. Check if another node offers the same rulebook. If yes, obtain the URL to their rulebook document.</p><p>3. If you have written the rulebook, publish it on your website or any online platform that allows sharing of files. Note that the rulebook must be available in JSON format, and it must be accessible through a URL.</p><p>4. Once you have the URL to the rulebook document, enter it in the 'Rulebook URL' field.</p><p>Note: If the URL is invalid or the rulebook is not in JSON format, you will not be able to add the rulebook URL. Ensure that the URL is correct and the rulebook is in the required format before adding it to the system.</p><p>If you want to create a new rulebook, you can compose one using the guidelines provided at https://docs.exonym.io. Once you have created the new rulebook, refer to the above steps to add the rulebook URL to the system.</p>",
			"hlpAdminAccessAdministratorAccounts":"<p>Adding an Administrator Account grants 'Assign' privileges to a trusted administrator of this Trust Network Node. <p>Any user with 'Assign' privileges can grant/revoke membership to this Trust Network.<p>To add an administrator, type a new email address and click ADD<p>When the Administrator has confirmed their email and set a password, they become active.<p>To edit an email address, select the administrator from the list and click EDIT<p>The 'Primary Administrator' (the first added) can only;<ul><li>Add/Remove Secondary Administrators</li><li>Create Recovery Files</li><li>Transfer a Source</li></ul><p>Secondary Administrators* can only perform the remaining actions.<p class='tiny'> *This approach simplifies administrator privilege management.",
			"hlpTransferSourceUrl":"<p>Transfers the Source to/from another Rulebook Node.<p>To transfer the Source out (donor), enter the URL of the Destination Host and click Begin Transfer. <p>To transfer the Source in (recipient), enter the Donor URL and click BEGIN TRANSFER. <p>It is unimportant which Host requests transfer first; however, both hosts must request transfer to be successful.<ul><li>Transfer must be completed within 24h of the first request to succeed </li><li>The Network Source will succeed or fail  without interruption to the Trust Network</li></ul>",
			"hlpNodeSecondaryNetworksIssuerRecipientUrl":"<p>Enter the URL of a Network Node that this Node wants to join and click JOIN.<p>OR<p>Enter the URL of a Node in another Trust Network that this Node wants to add to this Trust Network and click Add Member.<p>To Add Member, the recipient Node must have requested to join this node before adding membership will succeed.",
			"hlpNodeSourceAttachmentPublicName":"<p>Nodes are Publicly Identifiable and the Public Name will be written as part of this Node’s Public Identifier.<p>The Node cannot be renamed.<p>The public name cannot contain spaces or special characters except the hyphen.",
			"hlpNodeSourceAttachmentSourceUrl":"<p>Enter the current URL of the Source.  If the Source is later transferred, this will be updated automatically.<p>Only one Source can be attached to a Node. <p>To disconnect from a Source this Node must be reinstalled resulting in all keys and membership being wiped.",
			"hlpNodeTokenManagementPresentationToken":"<p>Drag & drop, or copy & paste the Presentation Token or a link to it. <p>Selecting Revoke will invalidate all the claims that the peer has made since they became a member.<p>To find the contact details belonging to a token use the Search button.<p>To Reissue membership to the same Peer find the user in Node~Member Management and click the green Re-Issue widget.",
			"hlpNodeMembershipAllTimeMembership":"<p>When a member is added to the network, the membership is stored regardless of membership revocation.<p>When a member is Added they are emailed set-up instructions.<p>To remove a member from the Trust Network, use Node~Revoke.  <p>To Reissue a previously revoked member search by username, email or contact number then click on the Reissue icon.<p>Email Address & Contact Number are editable.  Depending on this node configuration, email and contact number will need to be confirmed.",
			"hlpNodeMembershipUsername":"<p>The USERNAME is used by the Member when accessing this service.  It must be locally unique and known to the Member. <p>Lower case letters and hyphens without spaces are valid.  All other special characters are invalid.<p>The USERNAME cannot be changed after it has been set.",
			"hlpSourceNewNetworkNetworkName":"<p>Define a unique name on this domain for this network.  <p>The name does not need to be globally unique, only unique on this hosting and any hosting where it may later be transferred.<p>The name cannot contain spaces or special characters with the exception of the hyphen.<p>When the network is created the supporting files will be located at;<p>[this domain]/[network-name]/x-source/<p>Every Node on the Trust Network will have this name in the URL.",
			"hlpSourceNodeInformationDefaultPlatform":"<p>When a Peer claims membership of this Trust Network, they might do so with many services.<p>Each service verifies their own cryptographic pseudonym, which allows  bulk update of claims without peer authentication.<p>Members can add their own services when needed and these defaults only help members with set up.<p>Verifying services will publish their platform name, however it is usually their website address.",
			"hlpSourceNetworkInformationSourceUrl":"<p>The SOURCE URL is needed to Add Nodes to the network.<p>To add a node, send them this URL.  They will reply with their Node-URL.<p>Confirm the Node by pasting the Node-URL into the “Source~Node Management” screen.<p>Both the Node and Source URLs can be shared publicly.",
			"hlpSourceNodeManagementAuthorizedNodes":"<p>Nodes grant and revoke privilege to and from the Trust Network.<p>This Source grants and revokes privilege to and from the Nodes, so that the Trust Network can be formed.<p>If a Node has granted membership to Peers and that Node is removed from the network, these Peers are no longer part of the Trust Network.  It is therefore good practice to migrate Peers before removing Nodes. ",
			"hlpSourceNodeManagementNodeUrl":"<p>To add a node enter the Node-URL, optionally give it a name and click Add.  To add multiple nodes comma separate the URLs.<p>To remove a node enter the Node-URL and click remove.",
			"hlpSourceSecondaryNetworksSecondaryNetwork":"<p>If a node in your network is secondarily controlled by a different network, the requirement is defined here.<p>E.g. This network is global, however in a specific region a local government insisted that issuers follow regional laws, nodes operating in that region have a Secondary Network Requirement (SNR).<p>SNR nodes will be valid if and only if they are a member of all required secondary networks.<p><b>WARNING</b><p>If a node has active members, it is good practice to inform them of the requirement before it is imposed. This will give them time to obtain the privilege before it is formally required by the network."
		}
	}

}