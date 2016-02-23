
/**
 * ======== 获取浏览器相关属性
 */
function getPerformanceTiming() {
	var performance = window.performance;
	if (!performance) {
		//当前浏览器不支持
		console.log('no suppout performance 接口');
		return;
	}
	var t = performance.timing;
	var times = {};
	
	//[重要] 页面加载完成的时间
	//[原因] 这几乎代表了用户等待页面可用的时间
	//alert(t.loadEventEnd);
	times.loadPage = t.loadEventEnd - t.navigationStart;
	//[重要] 解析DOM树结构的时间
	times.domReady = t.domComplete - t.responseEnd;
	//重定向的时间
	times.redirect = t.redirectEnd - t.redirectStart;
	
	//DNS查询时间
	times.lookupDomain = t.domainLookupEnd - t.domainLookupStart;
	
	//读取页面第一个字节的时间
	times.ttfb = t.responseStart - t.navigationStart;
	
	//内容加载完成的时间
	times.request = t.responseEnd - t.requestStart;
	//执行onload回掉函数的时间
	times.loadEvent = t.loadEventEnd - t.loadEventStart;
	
	//DNS缓存时间
	times.appcache = t.domainLookupStart - t.fetchStart;
	//卸载页面时间
	times.connect = t.connectEnd - t.connectStart;

	return times;
}
/*
function css_load() {
	var oLink = document.getElementsByTagName('script');
    console.log(oLink[0]['src']);
	var imgs = document.getElementsByTagName("img");
	console.log(imgs[0]['src']);
}
css_load();*/



(function (win, doc) {
	//防止 重复加载  
	var objectName = win.dsObjName || 'ds';
	var oldObject = win[objectName];
	if (oldObject && oldObject.defined) {
		return;
	}

	var ie = win.attachEvent && !window.opera;
	var clickJsLinkTime;
	/**
	 * 追踪器字典
	 */
	var trackers={};
	
	var closing;// 页面关闭中 
	
	/**
     * 默认追踪器
     */
    var defaultTracker;
	/**
	 * 监听列表
	 */
	var dsListeners={};
	
	
	/**起始时间  */
	var startTime = (oldObject && oldObject.l) || (+new Date());
	
	/**
	 * session id 优先从服务端获取
	 * 
	 */
	
	var sid=win.logId || ((+new Date()).toString(36)+Math.random().toString(36).substr(2,3));
	
	/**
	 * id编码
	 */
	var guid=0;
	
	/**
	 * 正在加载的脚本
	 */
	var loadScripts ={};
	
	var modules={
		ds:{
			name:'ds',
			defined:true,
			instance:entry
		}
	};
	
	
	/**
	 * 处理入口
	 */
	function entry(params) {
		var args = arguments;
		var moduleName;
		var requires;
		var creator;

		if (params === 'define' || params === 'require') {
			//校正参数调用
			for (var i = 1; i < args.length; i++) {
				switch (typeof args[i]) {
					case 'string':
						moduleName = args[i];
						break;
					case 'object':
						requires = args[i];
						break;
					case 'function':
						creator = args[i];
						break;
				}
			}
			
			if(params === 'require'){
				if(moduleName && !requires){
					requires = [moduleName];
				}
				moduleName = null;
			}
			//如果是引用，这产生临时模块名
			moduleName = !moduleName ? '#' +(guid++):moduleName;
			var module;
			if(modules[moduleName]){
				module=modules[moduleName];
			}else{
				module={};
				modules[moduleName] = module;
			}
			
			//避免模块重复定义
			if(!module.defined){
				module.name = moduleName;
				module.requires=requires;
				module.creator=creator;
				if(params==='define'){
					module.defining =true;
				}
				clearDeps(module);
			}
			return;
			

		}
		
		if(typeof params === 'function'){
			params(entry);
			return;
		}
		
		String(params).replace(/^(?:([\w$_]+)\.)?(\w+)$/,
            function (all, trackerName, method) {
                args[0] = method; // 'hunter.send' -> 'send'
                command.apply(entry.tracker(trackerName), args);
            }
        );
	}
	
	/**
	 * 加载模块
	 */
	function loadModules(moduleName){
		var modulesConfig = defaultTracker.get('alias') || {};
		var scriptUrl = modulesConfig[moduleName] || (moduleName+'.js');
		if (loadScripts[scriptUrl]){
			return;
		}
		loadScripts[scriptUrl] = true;
		
		var scriptTag ='script';
		var scriptElement= doc.createElement(scriptTag);
		var lastElement = doc.getElementsByTagName(scriptTag)[0];
		scriptElement.async=!0;
		scriptElement.src = scriptUrl;
		lastElement.parentNode.insertBefore(scriptElement, lastElement);
	}
	
	/**
	 * 处理依赖关系
	 */
	function clearDeps(module){
		
		if(module.defined){
			return;
		}
		var defined = true;
		var params=[];
		var requires=module.requires;
		if(requires){
			for(var i=0;i<requires.length;i++){
				var moduleName=requires[i];
				var deps = modules[moduleName] =(modules[moduleName] || {});
				if(deps.defined || deps === module){
					params.push(deps.instance);
				}else{
					defined =false;
					if(!deps.defining){
						loadModules(moduleName);
					}
					deps.waiting = deps.waiting || {};
					deps.waiting[module.name] = module;
				}
			}
		}
		if(defined){
			module.defined = true;
			if(module.creator){
				module.instance = module.creator.apply(module,params);
				
			}
			clearWaiting(module);
		}
	}
	/**
	 * 清空等待依赖项加载的模块
	 */
	function clearWaiting(module){
		for(var moduleName in module.waiting){
			if(module.waiting.hasOwnProperty(moduleName)){
				clearDeps(module.waiting[moduleName]);
			}
		}
		
	}
	/**
	 * 获取时间戳
	 */
	function timestamp(now){
		return (now || new Date()) - startTime;
	}
	
	/**
	 * 绑定事件
	 */

	function on(elemt, eventName, callback) {
		if (!elemt) {
			return;
		}
		if (typeof elemt === 'string') {
			callback = eventName;
			eventName = elemt;
			elemt = entry;
		}
		try {
			if (elemt === entry) {
				dsListeners[eventName] = dsListeners[eventName] ||[];
				dsListeners[eventName].unshift(callback);
				return ;

			}
			if(elemt.addEventListener){
				elemt.addEventListener(eventName,callback,false);
			}else if(elemt.attachEvent){
				elemt.attacheEvent('on'+eventName,callback);
			}
		}
		catch (ex) { }

	}
	
	/**
	 * 注销事件绑定
	 */
	function un(element,eventName,callback){
		if(!element){
			return;
		}
		if(typeof element ==='string'){
			callback = eventName;
			eventName =element;
			element = entry;
		}
		
		try{
			if(element === entry){
				var listener = dsListeners[eventName];
				if(!listener){
					return;
				}
				var i=listener.length;
				while(i--){
					if(listener[i] === callback){
						listener.splic(i,1);
					}
				}
				return;
			}
			
			if(element.removeEventListener){
				element.removeEventListener(eventName,callback,false);
			}else{
				element.detachEvent && element.detachEvent('on'+eventName,callback);
			}
			
		}catch(ex){}
	}
	
	/**
	 * 触发事件
	 */
	
	function fire(eventName){
		var listener= dsListeners[eventName];
		if(!listener){
			return;
		}
		var items=[];
		var args=arguments;
		for(var i=1,len =args.length;i<len;i++){
			items.push(args[i]);
		}
		var result =0;
		var j = listener.length;
		while(j--){
			if(listener[j].apply(this,items)){
				result++;
			}
		}
		return result;
	}
	
	/**
	 * 上报数据
	 */
	function report(url, data) {
		if (!url || !data) {
			return;
		}
		var image = doc.createElement('img');
		var items = [];

		for (var key in data) {
			if (data[key]) {
				items.push(key + '=' + encodeURIComponent(data[key]));
			}
		}
		var name = 'img_' + (+new Date());
		entry[name] = image;
		image.onload = image.onerror = function () {
			entry[name]=
			 image=
			  image.onload=
			  image.onerror=null;
		};

		image.src = url + (url.indexOf('?') < 0 ? '?' : '&') + items.join('&');

	} 
	
	/**
	 * 字段名使用简写
	 */
	function runProtocolParameter(protocolParameter,data){
		if(!protocolParameter){
			return data;
		}
		var result={};
		for(var p in data){
			if(protocolParameter[p] !== null){
				result[protocolParameter[p] || p] = data[p];
			}
		}
		return result;
	}
	
	/**
	 * 执行命令
	 */
	function command(){
		var args=arguments;
		var method = args[0];
		
		if(this.created || /^(on|un|set|get|create)$/.test(method)){
			var methodFunc = Tracker.prototype[method];
			var params=[];
			for(var i=1,len =args.length;i<len;i++){
				params.push(args[i]);
			}
			if(typeof methodFunc === 'function'){
				methodFunc.apply(this,params);
			}
		}else{//实例创建以后才能调用的方法
			this.argsList.push(args);
		}
	}
	
	/**
	 * 合并两个对象
	 */
	function merge(a,b){
		var result ={};
		for(var p in a){
			if(a.hasOwnProperty(p)){
				result[p]=a[p];
			}
		}
		for(var q in b){
			if(b.hasOwnProperty(q)){
				result[q]=b[q];
			}
		}
		return result;
	}
	
	/**
	 * 追踪器构造器
	 */
	function Tracker(name){
		this.name=name;
		this.fields={
			protocolParameter:{
				postUrl:null,
				protocolParameter:null
			}
		};
		this.argsList=[];
		this.ds=entry;
	}
	
	function getTracker(trackerName){
		trackerName = trackerName || 'defaule';
		if(trackerName === '*'){
			var result = [];
			for(var p in trackers){
				if(trackers.hasOwnProperty(p)){
					result.push(trackers[p]);
				}
			}
			return result;
		}
		return (trackers[trackerName]= trackers[trackerName] || new Tracker(trackerName));
		
	}
	
	/**
	 * 创建追踪器
	 */
	Tracker.prototype.create=function(fields){
		if(this.created){
			return;
		}
		if(typeof fields === 'object'){
			this.set(fields);
		}
		this.created = new Date();
        this.fire('create', this);
        var args;
        while (args = this.argsList.shift()) {
            command.apply(this, args);
        }
		
	}
	/**
	 * 发送日志数据
	 * 
	 */
	Tracker.prototype.send = function(hitType,fieldObject){
		var data =merge({
		ts : timestamp().toString(36),
		t:hitType,
		sid:sid},this.fields);
		
		if(typeof fieldObject === 'object'){
			data = merge(data,fieldObject);
		}else{
			var args=arguments;
			switch(hitType){
				case 'pageview':
				if(args[1]){
					data.page=args[1];
				}
				if(args[2]){
					data.title=args[2];
				}
				break;
				case 'event':
				if(args[1]){
					data.eventCategory=args[1];
				}
				if(args[2]){
					data.eventAction = args[2];
				}
				if (args[3]) {
                        data.eventLabel = args[3];
                    }
                    if (args[4]) {
                        data.eventValue = args[4];
                    }
				break;
				case 'timing':
				// timingCategory, timingVar, timingValue[, timingLabel]
                    if (args[1]) {
                        data.timingCategory = args[1];
                    }
                    if (args[2]) {
                        data.timingVar = args[2];
                    }
                    if (args[3]) {
                        data.timingValue = args[3];
                    }
                    if (args[4]) {
                        data.timingLabel = args[4];
                    }
				break;
				case 'exception':
				// exDescription[, exFatal]
                    if (args[1]) {
                        data.exDescription = args[1];
                    }
                    if (args[2]) {
                        data.exFatal = args[2];
                    }
				break;
				default:
				return;
			}
			
			
		}
		this.fire('send',data);
			report(this.fields.postUrl,runProtocolParameter(this.fields.protocolParameter,data));
	};
	/**
	 * 设置字段值
	 */
	Tracker.prototype.set=function(name,value){
		if(typeof name ==='string'){
			if(name === 'protocolParameter'){
				value = merge({
					postUrl:null,
					protocolParameter:null
				},value);
			}
			this.fields[name]=value;
		}else if(typeof name === 'object'){
			for(var p in name){
				if(name.hasOwnProperty(p)){
					this.set(p,name[p]);
				}
			}
		}
	};
	
	/**
	 * 获取字段值
	 */
	Tracker.prototype.get = function(name,callback){
		var result = this.fields[name];
		if(typeof callback === 'function'){
			callback(result);
		}
		return result;
	}
	
	
	//触发事件
	Tracker.prototype.fire=function (eventName) {
		var items = [this.name + '.' + eventName];

        var args = arguments;
        for (var i = 1, len = args.length; i < len; i++) {
            items.push(args[i]);
        }
        return fire.apply(this, items);
	};
	
	/**
	 * 绑定事件
	 */
	Tracker.prototype.on = function(eventName,callback){
	   entry.on(this.name +'.'+eventName,callback);	
	};
	
	/**
	 * 注销事件
	 */
	Tracker.prototype.un = function(eventName,callback){
		entry.un(this.name+'.'+eventName,callback);
	};
	
	
	entry.name = 'ds';
    entry.sid = sid;
    entry.defined = true;
    entry.timestamp = timestamp;
    entry.un = un;
    entry.on = on;
    entry.fire = fire;
    entry.tracker = getTracker;

    entry('init');
	defaultTracker = getTracker();
    defaultTracker.set('protocolParameter', {
        modules: null
    });
	
	if(oldObject){
		// 处理临时ds
		var items=[].concat(oldObject.p || [],oldObject.q || []);
		oldObject.p = oldObject.q = null;//清理内存
		for(var p in entry){
			if(entry.hasOwnProperty(p)){
				oldObject[p] = entry[p];
			}
		}
		entry.p =entry.q={ //接管之前的定义
			push:function(args){
				entry.apply(entry,args);
			}
		};
		//开始处理缓存命令
		for(var i = 0;i<items.length;i++){
			entry.apply(entry,items[i]);
		}
	}
	win[objectName]=entry;
	
	
	//Cookie 操作 
	function setCookie(cname, cvalue, exdays) {
		var d = new Date();
		d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
		var expires = "expires=" + d.toUTCString();
		document.cookie = cname + "=" + cvalue + ";" + expires;
	}

	//获取cookie
	function getCookie(cname) {
		var name = cname + "=";
		var ca = document.cookie.split(";");
		for (var i = 0; i < ca.length; i++) {
			var c = ca[i];
			while (c.charAt(0) == ' ') c = c.substring(1);
			if (c.indexOf(name) != -1) return c.substring(name.length, c.length);
		}
		return "";
	}

	function clrCookie(cname) {
		setCookie(name, "", -1);
	}

	if (ie) {
		on(doc, 'mouseup', function (e) {
			var target = e.target || e.srcElement;
			if (target.nodeType === 1 && /^ajavascript:/i.test(target.tagName + target.href)) {
                clickJsLinkTime = new Date();
            }
		});
	}
	
	
	//关闭浏览器触发 
	
	function unloadHandler() {
		if (ie && ((new Date()) - clickJsLinkTime < 50)) {
			return;
		}
		if (closing) {
			return;
		}
		closing = true;
		var sleepCount = 0;
		for (var p in trackers) {

		}

		if (sleepCount) {
			var isSleep = new Date();
			while ((new Date() - isSleep) < 100) {

			}
		}
	}
	on(win, 'beforeunload', unloadHandler);
	on(win, 'unload', unloadHandler);
	
})(window, document);