window.onError = function(message, source, lineno, colno, error) {
    console.warn(message);
    console.warn(source);
    console.warn(lineno + " " + colno);
    console.warn(error);

}

var paywall = {
    init: (target) => {
        paywall.working();
        paywall.logon = document.getElementById("exo-paywall-sign-in");
        paywall.logon.remove();
        paywall.setTarget(target);
        var session = paywall.establishSession()
        session.then(paywall.loadContent);

    }, 

    loadContent: () => {
        const f = {
            challenge: () => {
                const cmd = {
                    cmd: "challenge",
                    contentId: paywall.getContentId(),
                    sessionId: paywall.getSessionData("sessionId"), 
                    url: paywall.getUrl()

                }
                post(paywall.target + "paywall", cmd)
                    .then(f.challengeResponse);

            },

            challengeResponse: (r) => {
                if (typeof r === "undefined" || r.error){
                    paywall.errorProcessing(r, paywall.loadContent);

                } else if (r[paywall.getContentId()]){
                    f.tokenRequest(r);

                } else if (r.qr){
                    paywall.addSignOn();
                    f.auth(r);

                } else {
                    console.log(r);

                }
            },
            auth : (r) => {
                const img = document.getElementById("exo-qr-code");
                paywall.qr(img, r.qr);

            }, 
            tokenRequest : (r) => {
                paywall.processReceivedPremiumContent(r);
                const target = paywall.getSessionData("ssiUrl");
                const ssiId = paywall.getSessionData("ssiId");
                post(target, {
                    cmd:"browse", 
                    ssiId:ssiId,
                    c:r.c,
                    url:paywall.getUrl()

                }).then(f.tokenResponse);
            },

            tokenResponse: (r) => {
                if (typeof r === "undefined" || r.error){
                    paywall.errorProcessing(r);

                }
            }
        }

        const premium = paywall.getSessionData(paywall.getContentId());
        if (premium!=null){
            paywall.loadPremiumContent(premium);

        } else {
            f.challenge();

        }
    },

    loadPremiumContent: (premium) => {
        var ele = document.getElementById("exo-paywall-sign-in");
        if (ele!=null){
            ele.parentNode.removeChild(ele);

        }
        var article = document.getElementById("exo-paywall");
        const div = document.createElement("DIV");
        div.innerHTML = premium;
        article.appendChild(div);

    },

    addSignOn: () => {
        var article = document.getElementById("exo-paywall");
        article.appendChild(paywall.logon);
        const signInBtn = document.getElementById("exo-paywall-more"); 
        signInBtn.addEventListener("click", paywall.authenticate);        
        
    },

    authenticate: () => {
        const qrCode = document.getElementById("exo-paywall-qr");
        const loginBtn = document.getElementById("exo-paywall-more");
        const msg = document.getElementById("exo-login-msg");
        msg.classList.remove("exo-remove");
        qrCode.classList.remove("exo-hidden");
        loginBtn.classList.add("exo-remove");
        const sid = paywall.getSessionData("sessionId")
        post(paywall.target + "paywall", {
            cmd: "probe",
            sessionId: sid

        }).then(paywall.probeResponse);
        
        setTimeout(
            paywall.waiter, 1000);

    },

    waiter: () => {
        post(paywall.target + "paywall", {
            cmd: "waiting",
            sessionId: paywall.getSessionData("sessionId")

        }).then(paywall.waitResponse);
    },

    probeResponse: (r) => {

        console.log("Got responsse - switching host");
        console.log(r);

        paywall.working();
        var ssi = r.ssiId.split("^");
        const ssiUrl = ssi[1];
        ssi = ssi[0];
        window.sessionStorage.setItem("ssiUrl", ssiUrl);
        paywall.switchHostId(ssi, ssiUrl);

    },

    waitResponse: (r) => {
        if (!r.error){
            paywall.processReceivedPremiumContent(r);

        } else {
            paywall.errorProcessing(r);

        }
    },

    processReceivedPremiumContent: (r) => {
        const premium = atob(r[paywall.getContentId()]);
        window.sessionStorage.setItem(paywall.getContentId(), premium);
        paywall.loadPremiumContent(premium);

    },

    switchHostId: (ssi, ssiUrl) => {
        post(ssiUrl, {cmd:"switch", ssiId: ssi}).then(
            (r) => {
                if (!r.error){
                    window.sessionStorage.setItem("ssiId", r.ssiId);
                    

                } else {
                    paywall.errorProcessing(r);
                    
                }
            }
        );
    },

    errorProcessing: (r, f, data) => {
        if (typeof r === "undefined"){
            console.log("No response");

        } else {
            if (r.error==="SESSION_EXPIRED"){
                paywall.sessionExpired(f, data);

                // todo - remove this is for testing.
                // window.sessionStorage.clear();
    
            } else if (r.error==="MEMBERSHIP_INVALID"){ 
                paywall.membershipInvalid();
                
            } else if (r.error==="REQUEST_TIMEOUT"){
                paywall.requestTimeout();
            
            } else if (r.error==="FAILED_TO_AUTHORIZE"){
                console.error("Received a authorization failure - ending session");
                window.sessionStorage.clear();

            } else {
                console.error(r);
                
            }
        }
    },    

    sessionExpired: (f, data) => {
        window.sessionStorage.removeItem("sessionId");
        window.sessionStorage.removeItem("ssiId");

        paywall.establishSession().then(() => {
            if (typeof f==='function'){
                f(data);
    
            }
        }); 
    },

    membershipInvalid: () => {
        console.log("Membership invalid");

    },

    requestTimeout: () => {
        const qrCode = document.getElementById("exo-paywall-qr");
        const loginBtn = document.getElementById("exo-paywall-more");
        const msg = document.getElementById("exo-login-msg");
        msg.classList.add("exo-remove");
        qrCode.classList.add("exo-hidden");

        const qrImg = document.getElementById("exo-qr-code");
        paywall.qr(qrImg, paywall.placeholder);

        loginBtn.classList.remove("exo-remove");

    },

    establishSession: () => {
        const sessionId = window.sessionStorage.getItem("sessionId");
        if (sessionId==null){
            console.log("triggered");

            const sessionR = get(paywall.target + "session").then((s)=>{
                window.sessionStorage.setItem("sessionId", s.sessionId);
            }).catch((e)=>{
                console.log(e);
                console.log("Server Unavailable");
                
            });
            return sessionR;

        } else {
            return new Promise((r)=> {r();});

        }
    }, 

    setTarget: (target) => {
        if (typeof target === "undefined"){
            throw "target must be set to the target api end point";

        }  if (!target.endsWith("/")){
            target+= "/";

        } if (target.startsWith("http://")){
            console.warn("Not Encrypted. To be used only for testing!");
            paywall.target = target;

        } 
        if (target.startsWith("https://") || target.startsWith("http://")){
            paywall.target = target;

        } else {
            throw "domain must be fully qualified";

        }
        console.log(paywall.target);            

    },

    qr: (imgTagJ, png) => {
        var source = "data:image/png;base64,--qr-base64--";
        source = source.replace("--qr-base64--", png);
        imgTagJ.setAttribute("src", source);

    },

    // Deprecated
    contentIndex: () => {
        var url = paywall.getUrl();
        const filename = paywall.extractFilename();
        return filename + ":" + url.hashCode();

    },

    getContentId: () => {
        return document.getElementById("exo-paywall")
                .getAttribute("contentid");
    },

    getSessionData: (item) => {
        return window.sessionStorage.getItem(item);

    },

    getUrl: () => {
        return window.location.href.split("?")[0];

    },

    // Deprecated
    extractFilename: () => {
        const path = window.location.pathname;
        const parts = path.split("/");
        const fn = parts[parts.length-1];
        return fn.split(".")[0]

    },

    working: () => {
        const img = document.getElementById("exo-qr-code");
        img.setAttribute("src", paywall.target + "images/working.svg");

    },

    target: null,
    logon: null

}

async function post(url = '', data = {}) {
    const response = await fetch(url, {
      method: 'POST', // *GET, POST, PUT, DELETE, etc.
      mode: 'cors', // no-cors, *cors, same-origin
      cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
      credentials: 'same-origin', // include, *same-origin, omit
      headers: {
        'Content-Type': 'application/json'
      },
      redirect: 'error', // manual, *follow, error
      referrerPolicy: 'strict-origin-when-cross-origin', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      body: JSON.stringify(data) // body data type must match "Content-Type" header
    });
    return response.json().catch((e) => {
        console.error("Caught Exception Follows:");
        console.log(e);

    });
}

async function get(url = '') {
    const response = await fetch(url, {
      method: 'GET', // *GET, POST, PUT, DELETE, etc.
      mode: 'cors', // no-cors, *cors, same-origin
      cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
      credentials: 'same-origin', // include, *same-origin, omit
      headers: {
        'Content-Type': 'application/json'
      },
      redirect: 'error', // manual, *follow, error
      referrerPolicy: 'strict-origin-when-cross-origin', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
    });
    return response.json().catch((e) => {
        console.log(e);
    });

}

class AntiXSS {
    static clean(str) {  	
      return String(str).replace(/&(?!amp;|lt;|gt;)/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }
}

// Deprecated
String.prototype.hashCode = function() {
    var hash = 0, i, chr;
    if (this.length === 0) return hash;
    for (i = 0; i < this.length; i++) {
      chr   = this.charCodeAt(i);
      hash  = ((hash << 5) - hash) + chr;
      hash |= 0; // Convert to 32bit integer
    }
    return hash;
};

