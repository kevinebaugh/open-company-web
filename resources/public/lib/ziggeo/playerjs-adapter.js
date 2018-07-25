/*!
ziggeo-client-sdk - v2.32.4 - 2018-07-25
Copyright (c) 
Proprietary Software License.
*/

!function(a,b){var c={};c.DEBUG=!1,c.VERSION="0.0.11",c.CONTEXT="player.js",c.POST_MESSAGE=!!a.postMessage,c.origin=function(b){return"//"===b.substr(0,2)&&(b=a.location.protocol+b),b.split("/").slice(0,3).join("/")},c.addEvent=function(a,b,c){a&&(a.addEventListener?a.addEventListener(b,c,!1):a.attachEvent?a.attachEvent("on"+b,c):a["on"+b]=c)},c.log=function(){c.log.history=c.log.history||[],c.log.history.push(arguments),a.console&&c.DEBUG&&a.console.log(Array.prototype.slice.call(arguments))},c.isString=function(a){return"[object String]"===Object.prototype.toString.call(a)},c.isObject=function(a){return"[object Object]"===Object.prototype.toString.call(a)},c.isArray=function(a){return"[object Array]"===Object.prototype.toString.call(a)},c.isNone=function(a){return null===a||a===undefined},c.has=function(a,b){return Object.prototype.hasOwnProperty.call(a,b)},c.indexOf=function(a,b){if(null==a)return-1;var c=0,d=a.length;if(Array.prototype.IndexOf&&a.indexOf===Array.prototype.IndexOf)return a.indexOf(b);for(;c<d;c++)if(a[c]===b)return c;return-1},c.assert=function(a,b){if(!a)throw b||"Player.js Assert Failed"},c.Keeper=function(){this.init()},c.Keeper.prototype.init=function(){this.data={}},c.Keeper.prototype.getUUID=function(){return"listener-xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx".replace(/[xy]/g,function(a){var b=16*Math.random()|0;return("x"===a?b:3&b|8).toString(16)})},c.Keeper.prototype.has=function(a,b){if(!this.data.hasOwnProperty(a))return!1;if(c.isNone(b))return!0;for(var d=this.data[a],e=0;e<d.length;e++)if(d[e].id===b)return!0;return!1},c.Keeper.prototype.add=function(a,b,c,d,e){var f={id:a,event:b,cb:c,ctx:d,one:e};this.has(b)?this.data[b].push(f):this.data[b]=[f]},c.Keeper.prototype.execute=function(a,b,d,e){if(!this.has(a,b))return!1;for(var f=[],g=[],h=0;h<this.data[a].length;h++){var i=this.data[a][h];c.isNone(b)||!c.isNone(b)&&i.id===b?(g.push({cb:i.cb,ctx:i.ctx?i.ctx:e,data:d}),!1===i.one&&f.push(i)):f.push(i)}0===f.length?delete this.data[a]:this.data[a]=f;for(var j=0;j<g.length;j++){var k=g[j];k.cb.call(k.ctx,k.data)}},c.Keeper.prototype.on=function(a,b,c,d){this.add(a,b,c,d,!1)},c.Keeper.prototype.one=function(a,b,c,d){this.add(a,b,c,d,!0)},c.Keeper.prototype.off=function(a,b){var d=[];if(!this.data.hasOwnProperty(a))return d;for(var e=[],f=0;f<this.data[a].length;f++){var g=this.data[a][f];c.isNone(b)||g.cb===b?c.isNone(g.id)||d.push(g.id):e.push(g)}return 0===e.length?delete this.data[a]:this.data[a]=e,d},c.Player=function(a,b){if(!(this instanceof c.Player))return new c.Player(a,b);this.init(a,b)},c.EVENTS={READY:"ready",PLAY:"play",PAUSE:"pause",ENDED:"ended",TIMEUPDATE:"timeupdate",PROGRESS:"progress",ERROR:"error"},c.EVENTS.all=function(){var a=[];for(var b in c.EVENTS)c.has(c.EVENTS,b)&&c.isString(c.EVENTS[b])&&a.push(c.EVENTS[b]);return a},c.METHODS={PLAY:"play",PAUSE:"pause",GETPAUSED:"getPaused",MUTE:"mute",UNMUTE:"unmute",GETMUTED:"getMuted",SETVOLUME:"setVolume",GETVOLUME:"getVolume",GETDURATION:"getDuration",SETCURRENTTIME:"setCurrentTime",GETCURRENTTIME:"getCurrentTime",SETLOOP:"setLoop",GETLOOP:"getLoop",REMOVEEVENTLISTENER:"removeEventListener",ADDEVENTLISTENER:"addEventListener"},c.METHODS.all=function(){var a=[];for(var b in c.METHODS)c.has(c.METHODS,b)&&c.isString(c.METHODS[b])&&a.push(c.METHODS[b]);return a},c.READIED=[],c.Player.prototype.init=function(d,e){var f=this;c.isString(d)&&(d=b.getElementById(d)),this.elem=d,c.assert("IFRAME"===d.nodeName,'playerjs.Player constructor requires an Iframe, got "'+d.nodeName+'"'),c.assert(d.src,"playerjs.Player constructor requires a Iframe with a 'src' attribute."),this.origin=c.origin(d.src),this.keeper=new c.Keeper,this.isReady=!1,this.queue=[],this.events=c.EVENTS.all(),this.methods=c.METHODS.all(),c.POST_MESSAGE?c.addEvent(a,"message",function(a){f.receive(a)}):c.log("Post Message is not Available."),c.indexOf(c.READIED,d.src)>-1?f.loaded=!0:this.elem.onload=function(){f.loaded=!0}},c.Player.prototype.send=function(a,b,d){if(a.context=c.CONTEXT,a.version=c.VERSION,b){var e=this.keeper.getUUID();a.listener=e,this.keeper.one(e,a.method,b,d)}return this.isReady||"ready"===a.value?(c.log("Player.send",a,this.origin),!0===this.loaded&&this.elem.contentWindow.postMessage(JSON.stringify(a),this.origin),!0):(c.log("Player.queue",a),this.queue.push(a),!1)},c.Player.prototype.receive=function(a){if(c.log("Player.receive",a),a.origin!==this.origin)return!1;var b;try{b=JSON.parse(a.data)}catch(d){return!1}if(b.context!==c.CONTEXT)return!1;"ready"===b.event&&b.value&&b.value.src===this.elem.src&&this.ready(b),this.keeper.has(b.event,b.listener)&&this.keeper.execute(b.event,b.listener,b.value,this)},c.Player.prototype.ready=function(a){if(!0===this.isReady)return!1;a.value.events&&(this.events=a.value.events),a.value.methods&&(this.methods=a.value.methods),this.isReady=!0,this.loaded=!0;for(var b=0;b<this.queue.length;b++){var d=this.queue[b];c.log("Player.dequeue",d),"ready"===a.event&&this.keeper.execute(d.event,d.listener,!0,this),this.send(d)}this.queue=[]},c.Player.prototype.on=function(a,b,c){var d=this.keeper.getUUID();return"ready"===a?this.keeper.one(d,a,b,c):this.keeper.on(d,a,b,c),this.send({method:"addEventListener",value:a,listener:d}),!0},c.Player.prototype.off=function(a,b){var d=this.keeper.off(a,b);if(c.log("Player.off",d),d.length>0)for(var e in d)return this.send({method:"removeEventListener",value:a,listener:d[e]}),!0;return!1},c.Player.prototype.supports=function(a,b){c.assert(c.indexOf(["method","event"],a)>-1,'evtOrMethod needs to be either "event" or "method" got '+a),b=c.isArray(b)?b:[b];for(var d="event"===a?this.events:this.methods,e=0;e<b.length;e++)if(-1===c.indexOf(d,b[e]))return!1;return!0};for(var d=0,e=c.METHODS.all().length;d<e;d++){var f=c.METHODS.all()[d];c.Player.prototype.hasOwnProperty(f)||(c.Player.prototype[f]=function(a){return function(){var b={method:a},d=Array.prototype.slice.call(arguments);/^get/.test(a)?(c.assert(d.length>0,"Get methods require a callback."),d.unshift(b)):(/^set/.test(a)&&(c.assert(0!==d.length,"Set methods require a value."),b.value=d[0]),d=[b]),this.send.apply(this,d)}}(f))}c.addEvent(a,"message",function(a){var b;try{b=JSON.parse(a.data)}catch(d){return!1}if(b.context!==c.CONTEXT)return!1;"ready"===b.event&&b.value&&b.value.src&&c.READIED.push(b.value.src)}),c.Receiver=function(a,b){this.init(a,b)},c.Receiver.prototype.init=function(d,e){var f=this;this.isReady=!1,this.origin=c.origin(b.referrer),this.methods={},this.supported={events:d||c.EVENTS.all(),methods:e||c.METHODS.all()},this.eventListeners={},this.reject=!(a.self!==a.top&&c.POST_MESSAGE),this.reject||c.addEvent(a,"message",function(a){f.receive(a)})},c.Receiver.prototype.receive=function(b){if(b.origin!==this.origin)return!1;var d={};if(c.isObject(b.data))d=b.data;else try{d=a.JSON.parse(b.data)}catch(g){c.log("JSON Parse Error",g)}if(c.log("Receiver.receive",b,d),!d.method)return!1;if(d.context!==c.CONTEXT)return!1;if(-1===c.indexOf(c.METHODS.all(),d.method))return this.emit("error",{code:2,msg:'Invalid Method "'+d.method+'"'}),!1;var e=c.isNone(d.listener)?null:d.listener;if("addEventListener"===d.method)this.eventListeners.hasOwnProperty(d.value)?-1===c.indexOf(this.eventListeners[d.value],e)&&this.eventListeners[d.value].push(e):this.eventListeners[d.value]=[e],"ready"===d.value&&this.isReady&&this.ready();else if("removeEventListener"===d.method){if(this.eventListeners.hasOwnProperty(d.value)){var f=c.indexOf(this.eventListeners[d.value],e);f>-1&&this.eventListeners[d.value].splice(f,1),0===this.eventListeners[d.value].length&&delete this.eventListeners[d.value]}}else this.get(d.method,d.value,e)},c.Receiver.prototype.get=function(a,b,c){var d=this;if(!this.methods.hasOwnProperty(a))return this.emit("error",{code:3,msg:'Method Not Supported"'+a+'"'}),!1;var e=this.methods[a];if("get"===a.substr(0,3)){var f=function(b){d.send(a,b,c)};e.call(this,f)}else e.call(this,b)},c.Receiver.prototype.on=function(a,b){this.methods[a]=b},c.Receiver.prototype.send=function(b,d,e){if(c.log("Receiver.send",b,d,e),this.reject)return c.log("Receiver.send.reject",b,d,e),!1;var f={context:c.CONTEXT,version:c.VERSION,event:b};c.isNone(d)||(f.value=d),c.isNone(e)||(f.listener=e);var g=JSON.stringify(f);a.parent.postMessage(g,""===this.origin?"*":this.origin)},c.Receiver.prototype.emit=function(a,b){if(!this.eventListeners.hasOwnProperty(a))return!1;c.log("Instance.emit",a,b,this.eventListeners[a]);for(var d=0;d<this.eventListeners[a].length;d++){var e=this.eventListeners[a][d];this.send(a,b,e)}return!0},c.Receiver.prototype.ready=function(){c.log("Receiver.ready"),this.isReady=!0;var b={src:a.location.toString(),events:this.supported.events,methods:this.supported.methods};this.emit("ready",b)||this.send("ready",b)},c.HTML5Adapter=function(a){if(!(this instanceof c.HTML5Adapter))return new c.HTML5Adapter(a);this.init(a)},c.HTML5Adapter.prototype.init=function(a){c.assert(a,"playerjs.HTML5Adapter requires a video element");var b=this.receiver=new c.Receiver;a.addEventListener("playing",function(){b.emit("play")}),a.addEventListener("pause",function(){b.emit("pause")}),a.addEventListener("ended",function(){b.emit("ended")}),a.addEventListener("timeupdate",function(){b.emit("timeupdate",{seconds:a.currentTime,duration:a.duration})}),a.addEventListener("progress",function(){b.emit("buffered",{percent:a.buffered.length})}),b.on("play",function(){a.play()}),b.on("pause",function(){a.pause()}),b.on("getPaused",function(b){b(a.paused)}),b.on("getCurrentTime",function(b){b(a.currentTime)}),b.on("setCurrentTime",function(b){a.currentTime=b}),b.on("getDuration",function(b){b(a.duration)}),b.on("getVolume",function(b){b(100*a.volume)}),b.on("setVolume",function(b){a.volume=b/100}),b.on("mute",function(){a.muted=!0}),b.on("unmute",function(){a.muted=!1}),b.on("getMuted",function(b){b(a.muted)}),b.on("getLoop",function(b){b(a.loop)}),b.on("setLoop",function(b){a.loop=b})},c.HTML5Adapter.prototype.ready=function(){this.receiver.ready()},c.JWPlayerAdapter=function(a){if(!(this instanceof c.JWPlayerAdapter))return new c.JWPlayerAdapter(a);this.init(a)},c.JWPlayerAdapter.prototype.init=function(a){c.assert(a,"playerjs.JWPlayerAdapter requires a player object");var b=this.receiver=new c.Receiver;this.looped=!1,a.on("pause",function(){b.emit("pause")}),a.on("play",function(){b.emit("play")}),a.on("time",function(a){var c=a.position,d=a.duration;if(!c||!d)return!1;var e={seconds:c,duration:d};b.emit("timeupdate",e)});var d=this;a.on("complete",function(){!0===d.looped?a.seek(0):b.emit("ended")}),a.on("error",function(){b.emit("error")}),b.on("play",function(){a.play(!0)}),b.on("pause",function(){a.pause(!0)}),b.on("getPaused",function(b){b(a.getState().toLowerCase()!=="PLAYING".toLowerCase())}),b.on("getCurrentTime",function(b){b(a.getPosition())}),b.on("setCurrentTime",function(b){a.seek(b)}),b.on("getDuration",function(b){b(a.getDuration())}),b.on("getVolume",function(b){b(a.getVolume())}),b.on("setVolume",function(b){a.setVolume(b)}),b.on("mute",function(){a.setMute(!0)}),b.on("unmute",function(){a.setMute(!1)}),b.on("getMuted",function(b){b(!0===a.getMute())}),b.on("getLoop",function(a){a(this.looped)},this),b.on("setLoop",function(a){this.looped=a},this)},c.JWPlayerAdapter.prototype.ready=function(){this.receiver.ready()},c.MockAdapter=function(){if(!(this instanceof c.MockAdapter))return new c.MockAdapter;this.init()},c.MockAdapter.prototype.init=function(){var a={duration:20,currentTime:0,interval:null,timeupdate:function(){},volume:100,mute:!1,playing:!1,loop:!1,play:function(){a.interval=setInterval(function(){a.currentTime+=.25,a.timeupdate({seconds:a.currentTime,duration:a.duration})},250),a.playing=!0},pause:function(){clearInterval(a.interval),a.playing=!1}},b=this.receiver=new c.Receiver;b.on("play",function(){var b=this;a.play(),this.emit("play"),a.timeupdate=function(a){b.emit("timeupdate",a)}}),b.on("pause",function(){a.pause(),this.emit("pause")}),b.on("getPaused",function(b){b(!a.playing)}),b.on("getCurrentTime",function(b){b(a.currentTime)}),b.on("setCurrentTime",function(b){a.currentTime=b}),b.on("getDuration",function(b){b(a.duration)}),b.on("getVolume",function(b){b(a.volume)}),b.on("setVolume",function(b){a.volume=b}),b.on("mute",function(){a.mute=!0}),b.on("unmute",function(){a.mute=!1}),b.on("getMuted",function(b){b(a.mute)}),b.on("getLoop",function(b){b(a.loop)}),b.on("setLoop",function(b){a.loop=b})},c.MockAdapter.prototype.ready=function(){this.receiver.ready()},c.VideoJSAdapter=function(a){if(!(this instanceof c.VideoJSAdapter))return new c.VideoJSAdapter(a);this.init(a)},c.VideoJSAdapter.prototype.init=function(a){c.assert(a,"playerjs.VideoJSReceiver requires a player object");var b=this.receiver=new c.Receiver;a.on("pause",function(){b.emit("pause")}),a.on("play",function(){b.emit("play")}),a.on("timeupdate",function(c){var d=a.currentTime(),e=a.duration();if(!d||!e)return!1;var f={seconds:d,duration:e};b.emit("timeupdate",f)}),a.on("ended",function(){b.emit("ended")}),a.on("error",function(){b.emit("error")}),b.on("play",function(){a.play()}),b.on("pause",function(){a.pause()}),b.on("getPaused",function(b){b(a.paused())}),b.on("getCurrentTime",function(b){b(a.currentTime())}),b.on("setCurrentTime",function(b){a.currentTime(b)}),b.on("getDuration",function(b){b(a.duration())}),b.on("getVolume",function(b){b(100*a.volume())}),b.on("setVolume",function(b){a.volume(b/100)}),b.on("mute",function(){a.volume(0)}),b.on("unmute",function(){a.volume(1)}),b.on("getMuted",function(b){b(0===a.volume())}),b.on("getLoop",function(b){b(a.loop())}),b.on("setLoop",function(b){a.loop(b)})},c.VideoJSAdapter.prototype.ready=function(){this.receiver.ready()},"function"==typeof define&&define.amd?define(function(){return c}):"object"==typeof module&&module.exports?module.exports=c:a.playerjs=c}(window,document),playerjs.BetaJSAdapter=function(a){if(!(this instanceof playerjs.BetaJSAdapter))return new playerjs.BetaJSAdapter(a);this.init(a)},playerjs.BetaJSAdapter.prototype.init=function(a){playerjs.assert(a,"playerjs.BetaJSReceiver requires a player object");var b=this.receiver=new playerjs.Receiver;a.on("paused",function(){b.emit("pause")}),a.on("playing",function(){b.emit("play")}),a.on("change:position change:duration",function(){var c=a.get("position"),d=a.get("duration");if(!c||!d)return!1;var e={seconds:c,duration:d};b.emit("timeupdate",e)}),a.on("ended",function(){b.emit("ended")}),a.on("error",function(){b.emit("error")}),b.on("play",function(){a.play()}),b.on("pause",function(){a.pause()}),b.on("getPaused",function(b){b(!a.get("playing"))}),b.on("getCurrentTime",function(b){b(a.get("position"))}),b.on("setCurrentTime",function(b){a.seek(b)}),b.on("getDuration",function(b){b(a.get("duration"))}),b.on("getVolume",function(b){b(100*a.get("volume"))}),b.on("setVolume",function(b){a.set_volume(b/100)}),b.on("mute",function(){a.set_volume(0)}),b.on("unmute",function(){a.set_volume(1)}),b.on("getMuted",function(b){b(0===a.get("volume"))}),b.on("getLoop",function(b){b(a.get("loop"))}),b.on("setLoop",function(b){a.set("loop",b)})},playerjs.BetaJSAdapter.prototype.ready=function(){this.receiver.ready()};