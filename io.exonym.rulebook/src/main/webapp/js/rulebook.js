{
	$(document).ready(()=>{
		$("#qrImg").removeClass("normal-size");
		$("#qrImg").addClass("think");				
		$("#qrImg").attr("src", "images/green-brown-3.7s-48px.svg");			

		$.get({
			url:"subscribe/qr",
			mimeType: "application/json",				
			success: rulebook.display
			
		})	
	});

	var rulebook = {
		display: (xnode) => {
			const f = {
				displayQr: () => {					
					var source = "data:image/png;base64,--qr-base64--";
					source = source.replace("--qr-base64--", xnode.challengeB64);
					console.log(source);
					$("#qrImg").attr("src", source);
					$("#qrImg").removeClass("think");				
					$("#qrImg").addClass("normal-size");				
			
				},
				
				addNodeTitle: () => {
					$("#rulebookName").html(f.title(xnode.description.name));
			
				},
				
				rulebookDescription: () => {
					f.description("rulebookDescription", xnode.description.simpleDescriptionEN);
					
					
				},
				
				description: (target, desc) => {
					$("#" + target + " > div > div > .rulebook-description").html(desc);
			
				}, 

				insertRules: (rules) => {
					var result = ""; 
					const paragraphs = "--rbParas--";
					const drill = /--drill--/g;
					const ruleId = /--heading-id--/g;
					const title = "--title--";
					const rulebookUid = "--rulebook-uid--";
					
					for (r in rules){
						var description = rules[r].description;
						var item = f.baseRule()
						.replace(rulebookUid, rules[r].id)
						.replace(ruleId, "rule" + r)
						.replace(drill, "drill" + r)
						.replace(title, description)
						.replace(paragraphs, f.ruleParagraphs(rules[r].interpretations));
						result += item;
						
					}			  
					$("#accordion1").html(result);			  					
				}, 

				testFlag: (isProduction) => {
					if (isProduction){
						$("#testNetwork").html("");

					} else {
						$("#testNetwork").html("Test Network");

					}
				}, 
				
				ruleParagraphs: (paras) => {
					if (paras!=undefined){
						var result = "";
						for (p in paras){
							result += "<p>" + JSON.stringify(paras[p]) + "</p>"
			
						}
						return result;
						
					} else {
						return "";
						
					}			  
				},
				
				title: (advocate) => {
					return advocate;

				},
				
				baseRule: () => "<div class='card'><div class='card-header' role='tab' id='--heading-id--'><h5 class='mb-0'><a data-toggle='collapse' href='#--drill--' role='button' aria-expanded='true' aria-controls='--drill--'>--title--</a></h5></div><div id='--drill--' class='collapse' role='tabpanel' aria-labelledby='--heading-id--' data-parent='#accordion1'><div class='card-body'><p class='rulebookUid'>--rulebook-uid--</p><div class='rbParas'>--rbParas--</div></div></div></div>"
			}
			
			
			const isProduction = xnode.description.production;
			const isNode = xnode.rulebookId!==undefined;
			const isInit = xnode.init!==undefined; 
			const isError = xnode.error!=undefined;
			
			if (isInit){
				window.location = "control-panel.html";
			
			} else if (isError){
				$("#networkNodeTitle").html(xnode.error);
			
			} else if (isNode || isSource){
				f.displayQr();
				f.testFlag(isProduction);
				f.addNodeTitle();
				f.rulebookDescription();				
				f.insertRules(xnode.rules);
						
			} else {
				console.log("Error");
			
			}		  
		}
	}
}