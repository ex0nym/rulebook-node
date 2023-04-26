$(document).ready(function(){
    menu.bind();
    
});

var ui = {
	toast: function(msg){
		console.log("Toast Message: " + msg);
		$("#toastText").html(msg);
		$("#toastMessage").removeClass("hidden");
		
		setTimeout(function(){
			$("#toastMessage").addClass("low-opacity");
			
			setTimeout(function(){
				$("#toastMessage").addClass("hidden");
				$("#toastMessage").removeClass("low-opacity");
			
			}, 2500);
			
		}, 2500);			
	},

	userConfirm: function(title, msg, callback){
		$("#errorPage").removeClass("hidden");
		$("#errorTitle").html(title);
		$("#errorMessage").html(msg);
		setTimeout(function(){
			$("#errorFrame").removeClass("low-opacity");
			$("#modalOk").click(function(){
				ui.hideMessage();
				callback();
				callback = function(){};
				
			});
			$("#modalCancel").click(function(){
				ui.hideMessage();
				callback=function(){};
			});
		}, 100);		

	}, 

	takeInput: function(title, callback){	
		$("#inputPage").removeClass("hidden");
		$("#inputTitle").html(title);
		setTimeout(function(){
			$("#inputFrame").removeClass("low-opacity");
			$("#inputOk").click(function(){
				ui.hideInput();
				var enteredValue = $("#modalInput").val();
				callback(enteredValue);
				callback = function(){};
				
				
			});
			$("#inputCancel").click(function(){
				ui.hideInput();
				callback=function(){};
				
			});
		}, 100);
	},

	hideMessage: function(){
		$("errorFrame").addClass("low-opacity");
		setTimeout(function(){
			$("#errorPage").addClass("hidden");
			
		}, 100);		
	}, 

	hideInput: function(){
		$("inputFrame").addClass("low-opacity");
		setTimeout(function(){
			$("#inputPage").addClass("hidden");
			
		}, 100);		
	}
}


var crypt = {
	sha256: function(str) {
  		// Transform the string into an arraybuffer.
  		var buffer = new TextEncoder("utf-8").encode(str);
  		return window.crypto.subtle.digest("SHA-256", buffer).then((hash) => crypt.hex(hash));

  	},

  	hex: function(buffer){
		var hexCodes = [];
		var view = new DataView(buffer);
		for (var i = 0; i < view.byteLength; i += 4) {
			var value = view.getUint32(i)
			var stringValue = value.toString(16)
			var padding = '00000000'
			var paddedValue = (padding + stringValue).slice(-padding.length)
			hexCodes.push(paddedValue);
			
		 }
		 return hexCodes.join("");  		

  	}
}

var menu = {
		
    bind: function(){
        $(".x-menu-btn").click(menu.menuSelect);
                                    
    }, 
    
    menuSelect: function(e){
        var id = $(e.target).attr("id");
        console.log(id);
        if (id === "controlPanelBtn"){
            menu.navControlPanel();
            
        } else if (id === "rulebookBtn"){
            window.location = "/";
            
        // } else if (id === "verifyBtn"){
        //     window.location = "manual.html";
            
        } else if (id === "verifiableClaimsBtn"){
            window.location = "https://exonym.io/verifiable-claims.html";
            
        } else {
            throw "Unrecognized id " + id;
            
        }
    },
    
    navControlPanel: function(){
            jQuery.get({
            method: "GET",
            url: "authenticate",
            mimeType: "application/json",
            success: function(json){
                    if (json.auth){
                        window.location = "control-panel.html";

                    } else {
                        window.location = "logon.html"
                        
                    }
                }
        });	
    }
}