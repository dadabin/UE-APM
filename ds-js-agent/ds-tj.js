(function (winElement, docElement) {
    // 压缩代码相关
    /* compressor */

    var objectName = winElement.dsObjectName || 'ds';

    var ds = winElement[objectName] = winElement[objectName] || function () {
        winElement[objectName].l = winElement[objectName].l || +new Date;
        (winElement[objectName].q = winElement[objectName].q || []).push(arguments);
    };
	
    var trackerName = 'speed';
    ds('define', trackerName, function () {
        var tracker = ds.tracker(trackerName);
        var timestamp = ds.timestamp; // 获取时间戳的函数，相对于ds被声明的时间
        tracker.on('record', function (url, time) {
            var data = {};
            data['imgurl'] = url;
			data['time']= timestamp(time);
            tracker.send('timing', data);
        });
        tracker.set('protocolParameter', {
            // 配置字段，不需要上报
            headend: null,
            bodyend: null,
            domready: null
        });
        tracker.create({
           // postUrl: 'http://111.1.15.173:9090/t.gif'
		    postUrl:'http://127.0.0.1:9090/t.gif'
			        });
        tracker.send('pageview', {
            ht: timestamp(tracker.get('headend')),
            lt: timestamp(tracker.get('bodyend')),
            drt: timestamp(tracker.get('domready')),
			nav:navigator_data()
        });
        return tracker;
    });
	// ----------------------异常采集-------------------
	//异常采集
	ds('define','err',function(){
		var errTracker=ds.tracker('err');
		window.onerror=function(msg,file,line){
			errTracker.send('err',{msg:msg,js:file,ln:line});
		};
		return errTracker;
	});
	//创建一个err统计模块实例
	ds('err.create',{
		 //postUrl: 'http://111.1.15.173:9090/t.gif'
		postUrl:'http://127.0.0.1:9090/t.gif'
	});
    
    //
    /**
	 * 获取浏览器指标
	 *  
	 */
    function navigator_data() {
		var navObj = {};
		if (navigator) {
			navObj.appCodeName = navigator.appCodeName;
			navObj.appMinorVersion = navigator.appMinorVersion;
			navObj.appName = navigator.appName;
			navObj.appVersion = navigator.appVersion;
			navObj.buildID = navigator.buildID;
			navObj.cpuClass = navigator.cpuClass;
			navObj.cookieEnabled = navigator.cookieEnabled;
			navObj.javaEnabled = navigator.javaEnabled;
			navObj.language = navigator.language;
			navObj.oscpu = navigator.oscpu;
			navObj.platform = navigator.platform;
			navObj.sysLanguage = navigator.systemLanguage;
			navObj.usrLanguage = navigator.userLanguage;
			navObj.usrAgent = navigator.userAgent;
			navObj.vendor = navigator.vendor;
			navObj.avail=window.screen.width+"x"+window.screen.height;
			
		}
		return JSON.stringify(navObj);
		//send_msg_post("http://" + server_ip + "/rest/naviator", b);
	}
})(window, document);