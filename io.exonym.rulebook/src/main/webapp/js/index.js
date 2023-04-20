// JavaScript Document
{
	$(document).ready(function(){
		proof.resolve();
		proof.bind();
		xcheck.bind();
		
	});

	var proof = {
		bind: function(){
			$("#putUrl").bind("paste", proof.pasteUrl);
			$("#tokenSuccessOkBtn").click(proof.closeSuccess);
			$("#tokenFailureCloseBtn").click(proof.closeFailure);

		},

		closeFailure: () => {
			$("#tokenFailure").addClass("hidden");

		},

		closeSuccess: () => {			
			const f = {
				gotProof: (json) => {
					var blob=new Blob([json.token]);
					var link=document.createElement('a');
					link.href=window.URL.createObjectURL(blob);
					link.download=json.fileName;
					link.click();
					proof.resolve();

				}, 
				gotError: (e) => {
					$("#tokenSuccess").addClass("hidden");
					proof.resolve();
					ui.toast(e);

				}
			}

			var report = $("#verifiedClaimEmailReport").attr("value");
			if (report==="true"){
				const cmd = {
					command: "downloadReport",
					uuid: proof.lastUuid

				}
				proof.serverRequest(cmd, f.gotProof, f.gotError);

			} else {
				proof.resolve();

			}
			$("#tokenSuccess").addClass("hidden");

		}, 

		resolve: function(){
			$("#networkName").html("GENERATING CHALLENGE");
			$("#nonce").html("--- ---");
			$("#qrImg").removeClass("normal-size");
			$("#qrImg").attr("src", "images/green-brown-3.7s-48px.svg");			
			
			jQuery.get({
				url:"exonym",
				mimeType: "application/json",				
				data:JSON.stringify({command:"xnode"}),
				success: proof.contentReceived
				
			})
		},			

		contentReceived: function(json){
			console.log(json);
			if (json.nodeSetup===false){
				$("#header").html("Initialize");
				$("#instructions").addClass("hidden");
				$("#output").addClass("hidden");

			} else {
				$("#nonce").html(json.nonce);
				$("#networkName").html(json.network);
				var source = "data:image/png;base64,--qr-base64--";
				source = source.replace("--qr-base64--", json.qr);
				$("#qrImg").attr("src", source);
				$("#qrImg").addClass("normal-size");				
				proof.waitForProof(json.uuid);

	
			}
		}, 

		waitForProof: (uuid) => {
			var cmd = {
				uuid: uuid,
				command: "wait-for-proof",

			}
			$.post({
				url: "exonym",
				mimeType: "application/json",
				data: JSON.stringify(cmd),
				success: (v) => {
					if (v.timeout){
						proof.resolve();
	
					} else {
						console.log(v);
						if (v.verified===true){
							proof.proofSuccess(v);
	
						} else {
							proof.proofFailure(v)
	
						}
					}
				}, failure: (e) => {
					console.error(e);
					ui.toast("Unexpected Error");

				}
			});
		},

		lastUuid: null,

		proofSuccess: (v) => {
			// {networkName, nodeName, shortName}
			proof.lastUuid = v.uuid;
			$("#tokenSuccess").removeClass("hidden");
			$("#tokenSuccessNetwork").html(v.networkName.toUpperCase()); 
			$("#tokenSuccessNetworkNode").html(v.nodeName.toUpperCase()); 				
			$("#tokenSuccessShortName").html(v.shortName.toUpperCase()); 
	
		},
	
		proofFailure: (proof) => {
			console.log("Called Revoked");
			$("#tokenFailure").removeClass("hidden");
	
		},			

		pasteUrl: function(e){
			var url = e.originalEvent.clipboardData.getData('text');
			$("#paste").html("Processing...");
			$("#nonce").html("");
			$("#qrImg").attr("src", "");
			$("#qrImg").attr("alt", "Refresh Page to Cancel");
			proof.serverRequest({command:"verifyFromPasted", url:url}, 
									proof.verified, proof.error);

		}, 

		verified: function(json){
			if(json.error){
				ui.toast(json.error);

			} else {
				console.log(json);

			}
		}, 

		error: function(e){
			console.log("Unhandled Error");
			console.log(e);

		},
		
		serverRequest: function(cmd, success, failure){
			console.log("Proof Server Request " + JSON.stringify(cmd));
			var endPoint = "exonym";
			$("#working").removeClass("hidden");

			$.ajax({type: "POST", 
				url: endPoint, 
				mimeType: "application/json",
				data: JSON.stringify(cmd),
				success: (json) => {
					$("#working").addClass("hidden");
					if (json.error){
						failure(json.error);

					} else {
						success(json);

					}
				},
				error: (e) => {
					$("#working").addClass("hidden");
					failure(e);

				}					
			});
		}
	}

	var xcheck = {
		bind: function(){
			$(".x-check-item").click(xcheck.clicked);
			$(".x-check-label").click(xcheck.clicked);
			
		}, 
		
		checkedClass: "x-check-box-checked",
		uncheckedClass: "x-check-box-unchecked",
	
		clicked: function(e){
			var parent = $(e.target).parent();
			
			var icon = parent.find(".x-check-item");
			var checked = icon.hasClass(xcheck.checkedClass);
			var parentId = "#" + parent.attr("id");
			
			$(parentId).attr("value", !checked);
	
			if (checked){
				icon.removeClass(xcheck.checkedClass);
				icon.addClass(xcheck.uncheckedClass);
				
			} else {
				icon.addClass(xcheck.checkedClass);
				icon.removeClass(xcheck.uncheckedClass);
	
			}
		},	
	}
	
	
}