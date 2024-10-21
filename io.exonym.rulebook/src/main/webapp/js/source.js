var source = {
	bind: function(){
		$("#sourceNodeInformationAddBtn").click(source.addDefaultPlatform);		

	}, 

	addDefaultPlatform: function(){
		var cmdData = {
			"command":"sourceNodeInformationAddDefaultPlatform",
			"networkName":general.selectedNetwork,
			"defaultPlatform":$("#sourceNodeInformationNewDefaultPlatform").val(),

		}
		general.serverRequest(cmdData, general.networkSource, general.requestError);
		ui.toast("Adding Default Platform & Signing Public Data");
		$("#sourceNodeInformationNewDefaultPlatform").val("");

	},

	networkSelected: function(network){
		$(".network-source-select").val(network.networkName);
		$("#sourceNodeInformationSourceUrl").html(network.sourceUrl);
		source.updateDefaultPlatforms(network.defaultPlatforms);
		source.updateAuthorizedNodes(network.authorizedNodes);

	},

	updateDefaultPlatforms: function(platforms){
		var items = [];

		for (p in platforms){
			var platform = platforms[p];
			var item = {
				id: platform,
				name: platform,
				clazz: "list-button-delete"

			}
			items.push(item);

		}
		general.populateList({
			list:"#sourceNodeInformationDefaultPlatform",
			items:items

		});

	},
	updateAuthorizedNodes: function(nodes){
		var items = [];

		for (n in nodes){
			var node = nodes[n];
			if (node.internalName==="" || 
				node.internalName==null || 
				node.internalName=="null"){
					
				var i = node.nodeUid.split(":");				
				node.internalName = i[4];

			}			
			var item = {
				id: node.nodeUid,
				name: node.internalName,
				clazz: "list-button-delete"

			}
			items.push(item);

		}
		general.populateList({
			list:"#sourceNodeManagementAuthorizedNodes",
			items:items

		});		

	},
}

var sourceNodeManagement = {
	bind: function(){
		$("#sourceNodeManagementAddBtn").click(sourceNodeManagement.addNode);
		$("#sourceNodeManagementRemoveBtn").click(sourceNodeManagement.removeNode);

	}, 

	addNode: function(){
		var cmdData = {
			"command":"sourceNodeManagementAdd",
			"networkName":general.selectedNetwork,
			"nodeUrl":$("#sourceNodeManagementNodeUrl").val(),
			"internalName":$("#sourceNodeManagementInternalName").val()

		}
		general.serverRequest(cmdData, general.networkSource, general.requestError);

	},

	removeNode: function(){
		var cmdData = {
			"command":"sourceNodeManagementRemove",
			"networkName":general.selectedNetwork,
			"nodeUrl":$("#sourceNodeManagementNodeUrl").val(),

		}
		general.serverRequest(cmdData, general.networkSource, general.requestError);

	}
}

var sourceSecondaryNetworks = {
	bind: function(){
		$("#sourceSecondaryNetworksSecondaryNetwork").change(sourceSecondaryNetworks.secondaryNetwork);
		$("#sourceSecondaryNetworksEnforceBtn").click(sourceSecondaryNetworks.enforceBtn);

	}, 

	secondaryNetwork: function(e){
		var id = $(this).find("option:selected").attr("value");
		if (id==="sourceSecondaryNetworkAddNetworkOption"){
			ui.takeInput("Enter Network URL", function(url){
				var cmdData = {
					"command":"sourceSecondaryNetworkAddNetwork",
					"networkName":url

				}
				general.serverRequest(cmdData, sourceSecondaryNetworks.addNetwork, general.requestError);
			
			});				
		} else {
			var cmdData = {
				"command":"sourceSecondaryNetworkGetNetwork",
				"networkName":id
				
			}
			general.serverRequest(cmdData, sourceSecondaryNetworks.secondaryNetworkSelected, general.requestError);
		
		}				
	},

	addNetwork: function(json){
		console.log(json);

	},

	secondaryNetworkSelected: function(json){
		console.log(json);

	},

	enforceBtn: function(e){
		var network = $("#sourceSecondaryNetworksNetwork").val();
		var secondaryNetwork = $("#sourceSecondaryNetworksSecondaryNetwork").val();
		var available = sourceSecondaryNetworks.computeListValues("#sourceSecondaryNetworksAvailableNodes");
		var required = sourceSecondaryNetworks.computeListValues("#sourceSecondaryNetworksRequiredNodes");
		
		var cmdData = {
			"command":"sourceSecondaryNetworksEnforce", 
			"network":network,
			"secondaryNetwork":secondaryNetwork,
			"available":available,
			"required":required
		}
		general.serverRequest(cmdData, sourceSecondaryNetworks.enforce, general.requestError);

	},

	enforce: function(json){
		console.log(json);

	},

	computeListValues: function(list){
		if ($(list).hasClass("multi")){
			var items = $(list).children();
			var result = [];
			
			$.each(items, function(i, val){
				var item = $(val).find(".list-label-item").text();
				result.push(item);

			});
			return result;

		} else {
			throw list + " is not a 'multi' class";

		}				
	}
}

var sourceNewNetwork = {
	bind: function(){
		$("#sourceNewNetworkCreateBtn").click(sourceNewNetwork.createBtn);

	}, 

	createBtn: function(){
		var networkName = $("#sourceNewNetworkNetworkName").val();
		var rulebookURL = $("#sourceNewNetworkRulebookURL").val();
		sourceNewNetwork.oldNetwork = general.selectedNetwork;
		general.selectedNetwork = networkName;
		
		if (networkName==="" || networkName===null){
			ui.toast("You must enter a network name");
			
		} else if (rulebookURL==="" || rulebookURL===null){
			ui.toast("You must enter a rulebook URL");

		} else {
			var cmdData = {
				"command":"sourceNewNetworkCreate",
				"networkName":networkName,
				"rulebookUrl":rulebookURL
				
			}
			ui.toast("Creating Network '"+networkName+"'");
			general.serverRequest(cmdData, sourceNewNetwork.create, sourceNewNetwork.revertNetworkSelection);
			
		}
	}, 

	oldNetwork: "",

	revertNetworkSelection: (e) => {
		general.selectedNetwork = sourceNewNetwork.oldNetwork;
		general.requestError(e);

	}, 

	create: function(json){
		general.populateNetworks();		
		general.forceNav("#sourceNodeManagement");
		$("#sourceNewNetworkNetworkName").val("");

	}
}