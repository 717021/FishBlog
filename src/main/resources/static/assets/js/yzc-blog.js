/*!
 * 
 * FishBlog system 前端基础组件
 * V.1.0.0316.1000
 * 
 */


var loader_arr_css = new Array();
var loader_arr_js = new Array();
var loader_stats = "not load";
var loader_finish_callback = new Array();
var loader_endload_callback = new Array();
var loader_all_finished = false;

var allJsCount = 0;
var allCssCount = 0;
var loadedJsCount = 0;
var loadingJsIndex = 0;
var loadedCssCount = 0;
var loadJs = []
var loadCss = []

var localDebug = false;
var uidz = 1123493;
var uidg = 15;

var loading_progress_bar = null;
var body = null;

var doNotHideMask = false;

//=============== 
//Entry

function setLoaderCSS(css_arr) { loader_arr_css = css_arr; }
function setLoaderJS(js_arr) { loader_arr_js = js_arr; }
function appendLoaderCSS(css) { 
    loader_arr_css.push(css);
    allCssCount++;
}
function appendLoaderJS(js) { loader_arr_js.push(js);allJsCount++; }
function getLoaderStatus() { return loader_stats; }

function setLoaderFinishCallback(callback) { loader_finish_callback.push(callback); }
function setLoaderEndCallback(callback) { loader_endload_callback.push(callback); }
function setLoaderNotHideMask(){ doNotHideMask = true; }
function setLoaderHideMaskNow(){ loaderHideMsak(); }



//===============

//Loading funs
function preRecUrl(url, pre) {
    if (url.indexOf('cdn:') == 0) {
        if (!localDebug) return (https ? 'https://' : 'https://') + 'cdn.imyzc.com/' + url.substring(4);
        else return '/assets/' + url.substring(4);
    }
    else if (url.indexOf('/') == -1)
        return '/assets/' + pre + '/' + url;
    return url;
}

function cssErr(event){
    loadCss.push({src:event.target.src,success: false});
}
function jsErr(event){
    loadJs.push({src:event.target.src,success: false});
    if(loadedJsCount<allJsCount){
        if (typeof jQuery == 'undefined') {
            body.setAttribute('class', 'no-scroll');
            body.setAttribute('style', 'visibility:visible;background-color:#200;text-align:center;padding-top:20px;');
            body.innerHTML = "<h1 style='color:#fff;'>加载必要的 JS 失败</h1><a href=\"javascript:location.reload(true);\">重新加载</a>";
        } else{
            loadingJsIndex++;
            loaderJsWorker();
        }
    }
}
function loadCSS(url) {
    var loaded = false;
    var cssLink = document.createElement("link");
    if (url == null || url == "") { loadedCssCount++; callback(true); }

    cssLink.rel = "stylesheet";
    cssLink.rev = "stylesheet";
    cssLink.type = "text/css";
    cssLink.href = preRecUrl(url, 'css');
    cssLink.onerror = cssErr;
    cssLink.onload = cssLink.onreadystatechange = function () {
        if (!loaded && (!cssLink.readyState || /loaded|complete/.test(cssLink.readyState))) {
            cssLink.onload = cssLink.onreadystatechange = null;
            loaded = true;
            loadedCssCount++;
            update_progress();
            loadCss.push({src:cssLink.href,success: true});
        }
    }
    document.getElementsByTagName("head")[0].appendChild(cssLink)
}
function loadJS(src, callback) {

    if (src == null || src == "") {
        if (typeof callback === "function") callback(true);
        return;
    }

    var script = document.createElement("script");
    var head = document.getElementsByTagName("head")[0];
    var loaded = false;

    script.setAttribute('type', 'text/javascript');
    script.src = preRecUrl(src, 'js');
    script.onerror = jsErr;
    if (typeof callback === "function") {
        script.onload = script.onreadystatechange = function () {
            if (!loaded && (!script.readyState || /loaded|complete/.test(script.readyState))) {
                script.onload = script.onreadystatechange = null;
                loaded = true;
                loadJs.push({src:script.src,success: true});
                callback(loaded)
            }
        }
    }
    try{
        head.appendChild(script)
    }catch(err){
        toast('Load Js ' + loader_arr_js[loadingJsIndex] + ' Error : ' + err,'error',15000);
        if (typeof callback === "function")  callback(false)
    }
}

//Content loader
function loaderLoadBackgroundImage() {
    if (typeof jQuery != 'undefined') {
        $('.dealy-load-bgimg').each(function () {
            var data = 'url(\'' + $(this).attr('data-original') + '\')';
            $(this).css('background-image', data);
        });
        $('.img-async').each(function () {
            $(this).attr('src', $(this).attr('data-original'));
        });
    }
}

//Progress bar
function calc_precent() { return parseInt((loadedJsCount / allJsCount) * 50) + parseInt((loadedCssCount / allCssCount) * 50); }
function update_progress() { loading_progress_bar.setAttribute('style', 'width:' + calc_precent() + '%;') }//Set progress

//===============
//Test

function loaderTestConfig() {
    //获取主机地址
    https = window.document.location.href.indexOf('https://') == 0;
    var localhostPath = getHostName();
    localDebug = localhostPath.indexOf('localhost') == 0;
    allCssCount = loader_arr_css.length;
    allJsCount = loader_arr_js.length;
}
function loaderFinishTest() {
    if (!loader_all_finished) {
        loader_all_finished = true;
        console.log("Loaded JS : " + loadedJsCount + "/" + allJsCount);
        console.log("Loaded CSS : " + loadedCssCount + "/" + allCssCount);
        if (typeof jQuery == 'undefined') {
            body.setAttribute('class', 'no-scroll');
            body.setAttribute('style', 'visibility:visible;background-color:#a00;text-align:center;padding-top:20px;');
            body.innerHTML = "<h1 style='color:#fff;'>加载必要的 JS 失败</h1><a href=\"javascript:location.reload(true);\">重新加载</a>";
            return;
        }
        if (loadedJsCount < allJsCount) {
            $("#loading-progress-bar").css('color', '#fff').css('background-color', 'rgb(182,0,0)').css('width', '100%');
            setTimeout(function () {
                setTimeout(function () {
                    $("#loading-progress-bar").prepend($("<div id=\"noscript-warning\" style=\"color:#fff;background-color:#8E0000;position:fixed;left:0;top:5px;right:0;padding:3px;width: 100%;z-index: 1050;text-align: center;\">\
                    我们需要一些额外的 JavaScript 才能正常显示页面, 但是未能成功加载，您看到的页面可能不正常. <a href=\"javascript:void(0)\" onclick=\"loaderShowErrors(true);\">详情</a> | <a href=\"javascript:void(0)\" onclick=\"location.reload(true);\">刷新</a>\
                    <a href=\"javascript:void(0)\" onclick=\"loaderEnd(true);\" style=\"float:right;margin-right:8px;margin-bottom:2px;\" title=\"关闭并继续浏览此页\"><i class=\"fa fa-times\"></i></a></div>"));
                }, 300);
            }, 500);
        }
        /*if (loadedCssCount < allCssCount) {
            var errtip = document.createElement('div');
            errtip.id = 'noscript-warning';
            errtip.innerHTML = "<div style=\"color:#fff;background-color:#FF6600;position:fixed;left:0;top:5px;right:0;padding:3px;width: 100%;z-index: 1050;text-align: center;\">我们需要一些额外的 CSS 样式表才能正常显示页面, 但是未能成功加载，您看到的页面可能不正常.<a href=\"#\" onclick=\"document.getElementById('noscript-warning').setAttribute('style', 'display:none;');\">关闭</a></div>";
            document.getElementsByTagName('body')[0].appendChild(errtip);
        }*/
    }
}

//===============
//Worker

function loaderJsWorker() {
    try {
        loadJS(loader_arr_js[loadingJsIndex], function (succeed) {
        if (succeed) loadedJsCount++;
        loadingJsIndex++;
        update_progress();
        if (loadingJsIndex < allJsCount) loaderJsWorker()
        if (loadingJsIndex >= allJsCount) loaderEnd(false)
        })
    } catch(err) {
        toast('Load Js ' + loader_arr_js[loadingJsIndex] + ' Error : ' + err,'error',15000);
        loadingJsIndex++;
        update_progress();
    }
}
function loaderStart() {

    //生成加载中遮罩
    loaderCreateUI();

    //初始化配置
    loaderTestConfig();

    //设置加载超时
    //setTimeout(function () { loaderFinishTest() }, 20000);
    //加载 css
    if (loader_arr_css instanceof Array)
        for (var j = 0, len = loader_arr_css.length; j < len; j++)
            loadCSS(loader_arr_css[j])
    //加载 js
    if (loader_arr_js instanceof Array)
        loaderJsWorker()
    else loaderEnd(false)
}

//===============


function loaderEnd(click) {
    //加载中途插入的 CSS
    if (loader_arr_css instanceof Array && loadedCssCount < allCssCount){
        for (var j = loadedCssCount, len = loader_arr_css.length; j < len; j++)
            loadCSS(loader_arr_css[j])
    }

    loaderCallcallbacks(loader_endload_callback);

    //load image
    if (!click) loaderFinishTest();
    loaderLoadBackgroundImage();
    loaderFinish(click);
}
function loaderCallcallbacks(callbacks) {
    callbacks.forEach(function (e) { if (typeof e === "function") e(); });
}
function loaderFinish(click) {
    //Hide loading
    if (typeof jQuery == 'undefined') {
        body.setAttribute('class', 'no-scroll');
        body.setAttribute('style', 'visibility:visible;');
        body.innerHTML = "<h1 style='text-align:center;'>加载必要的 JS 失败</h1>";
    } else {
        if (click) {
            $('#noscript-details').slideUp();
            $('#noscript-warning').fadeOut();
        } else if (loadedJsCount >= allJsCount) {
            $('#loading-progress-bar').css('height', '0px').css('color', '').css('background-color', '');
            $('#noscript-warning').remove();
            setTimeout(function () {
                $('#loading-progress-bar').css('display', 'none').css('height', '5px');
            }, 300);
        }
        //Init blog
        blogInitnaize();

        //发送用户浏览页面数据PV
        sendStat();

        //Call back
        loaderCallcallbacks(loader_finish_callback);
        
        if(doNotHideMask) return;

        loaderHideMsak();
    }
}
function loaderHideMsak(){

    $("#loading").fadeOut(800);
    //Body invisible class clear
    $("body").removeClass('not-load');
    $("body").removeClass('no-scroll');
}
function loaderShowErrors(){
    $new_box = $("<div id=\"noscript-details\" style=\"border: 1px solid #ddd;box-shadow: 8px 14px 38px rgba(39,44,49,0.06), 1px 3px 8px rgba(39,44,49,0.03);max-width:800px;color:#000;background-color:#fff;position:fixed;top:65px;padding:25px;width:100%;z-index: 1090;text-align:left;word-break:break-all;border-radius:10px;overflow-y:scroll;padding-top:25px;max-height:80%;\" class=\"pc-fix-scrollbar\"><h3>加载器加载日志详情<span style=\"color:#aaa;font-size:13px;margin-left:8px;\">更多错误请查看开发者工具</span></h3><hr/></div>");
    $("#loading-progress-bar").prepend($new_box);
    $new_box.css('left', ($(window).width() / 2 - $new_box.width() / 2) + 'px');
    $noscript_details = $('#noscript-details');
    for(var k in loadJs){
        $noscript_details.append($('<div style="font-family:sans-serif;word-break:break-all;border-bottom:1px solid #eee;padding-bottom: 10px;padding-top: 10px;"><i class="fa ' +(loadJs[k].success ? 'fa-check-circle-o text-success' : 'fa-times-circle-o')+'" style="vertical-align: middle;font-size:20px;color:#e00;margin-right:15px;"></i> ' +
        (loadJs[k].success ? '<span class="badge badge-pill badge-success">JS</span>' : '<span class="badge badge-pill badge-danger">JS</span>') + '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' + loadJs[k].src + '</div>'))
    }
    for(var k in loadCss){
        $noscript_details.append($('<div style="font-family:sans-serif;word-break:break-all;border-bottom:1px solid #eee;padding-bottom: 10px;padding-top: 10px;"><i class="fa ' +(loadCss[k].success ? 'fa-check-circle-o text-success' : 'fa-times-circle-o')+'" style="vertical-align: middle;font-size:20px;color:#e00;margin-right:15px;"></i> ' +
        (loadCss[k].success ? '<span class="badge badge-pill badge-success">CSS</span>' : '<span class="badge badge-pill badge-danger">CSS</span>') + '  &nbsp;&nbsp;' + loadCss[k].src + '</div>'))
    }
    $noscript_details.append($('<hr/><div>加载  JS : ' + loadedJsCount + "/" + allJsCount + '</div>'));
    $noscript_details.append($('<div>加载 CSS : ' + loadedCssCount + "/" + allCssCount + '</div>'));
}

//===============

//生成加载中遮罩
function loaderCreateUI() {
    body = document.getElementsByTagName('body')[0];
    loading_progress_bar = document.createElement('div');
    pre_loader = document.createElement('div');
    toast_overlay = document.createElement('div');
    body.appendChild(loading_progress_bar);
    body.appendChild(pre_loader);
    body.setAttribute('class', 'no-scroll ' + body.getAttribute('class').replace('not-load', ''));
    loading_progress_bar.outerHTML = '<div id="loading-progress-bar"></div>';
    pre_loader.outerHTML = '<!--Loading overlay--><div id="loading" style="z-index:1001;"><div id="loading-progress-bg"></div><div id="loading-center"><div id="loading-center-absolute"><span id="loading-simple-roll"></span></div><p>加载中<br />很快就好了</p></div></div>';
    loading_progress_bar = document.getElementById('loading-progress-bar');
    toast_overlay.setAttribute('id', 'toast-overlay');
    toast_overlay.setAttribute('class', 'toast-overlay-wrapper position-fixed');
    body.appendChild(toast_overlay);
}

//博客组件初始化
function blogInitnaize() {

    var top = true, currentTop = true, stopResizeNav = false;
    var width = $(window).width();
    //console.log(width);
    if ($('.main-menu').first().parent().attr("id") == 'header-minimum')
        stopResizeNav = true;
    if (width < 768) {
        if ($('#header-minimum').length > 0) {
            $('.main-menu').css('padding-top', '');
            $('.main-menu').css('padding-bottom', '');
        }else{
            $('.main-menu').css('padding-top', '15px');
            $('.main-menu').css('padding-bottom', '15px');
        }
    } else if ($('#header-minimum').length == 0) {
        $('.main-menu').css('padding-top', '40px');
        $('.main-menu').css('padding-bottom', '30px');
    } else if ($('#header-minimum').length > 0) {
        $('.main-menu').css('padding-top', '');
        $('.main-menu').css('padding-bottom', '');
    } else {
        $('.main-menu').css('padding-top', '15px');
        $('.main-menu').css('padding-bottom', '15px');
    }
    if (!$('.main-menu').hasClass('noscroll')) {
        top = $(this).scrollTop() < 100;
        if (!top && !stopResizeNav) {
            $('.go-top').fadeIn();
            $('.main-menu').addClass('header-scrolled');
            if (width > 768) {
                $('.main-menu').css('padding-top', '15px');
                $('.main-menu').css('padding-bottom', '15px');
            }
        } else {
            $('.main-menu-white-auto-mask').addClass('main-menu-white-fade-mask');
        }
    }

    //Fix header height


    //-------Lazy load js --------//  
    if (typeof $("img").lazyload != 'undefined') $("img").lazyload({ effect: "fadeIn", threshold: 300 });

    //-------Load ansyc image---------//
    $(".img-async").each(function () {
        if ($(this).attr('data-original') != null) {
            $(this).attr('src', $(this).attr('data-original'));
        }
    });

    //------- Window resize event --------//  
    $(window).resize(function () {
        width = $(window).width();
    });

    //------- Superfist nav menu  js --------//  
    if (typeof $("img").superfish != 'undefined') $('.nav-menu').superfish({
        animation: {
            opacity: 'show'
        },
        speed: 200
    });
    //------- Back to top Scroll --------//  
    $(window).scroll(function () {
        if ($(this).scrollTop() < 300) $('.go-top').fadeOut();
        else $('.go-top').fadeIn();
    });
    //------- Back to topevent --------//  
    $('.go-top').click(function (event) {
        event.preventDefault();
        $('html, body').animate({ scrollTop: 0 }, 300);
    });
    $('.go-top').tooltip({
        boundary: 'window',
        title: '回到顶部',
        placement: 'left',
        delay: 200
    })

    //------- Enable tooltips everywhere --------//  
    $('[data-toggle="tooltip"]').tooltip();

    if (!$('.main-menu').hasClass('noscroll')) {
        //------- Header Scroll Class  js --------//  
        $(window).scroll(function () {
            top = $(this).scrollTop() < 100;
            if (!stopResizeNav && currentTop != top) {
                currentTop = top;
                if (top) {
                    $('.main-menu').removeClass('header-scrolled');
                    $('.main-trans-white').addClass('text-white');
                    $('.main-menu-white-auto-mask').addClass('main-menu-white-fade-mask');
                    $('.main-menu-white-auto-mask-no-fade').addClass('main-menu-white-fade');
                    $('.main-menu-black-auto-mask').addClass('main-menu-black-fade-mask');
                    $('.main-menu-black-auto-mask-no-fade').addClass('main-menu-black-fade');
                    if (width > 768) {
                        $('.main-menu').css('padding-top', '40px');
                        $('.main-menu').css('padding-bottom', '30px');
                    }
                } else {
                    $('.main-trans-white').removeClass('text-white');
                    $('.main-menu').addClass('header-scrolled');
                    $('.main-menu-white-auto-mask').removeClass('main-menu-white-fade-mask');
                    $('.main-menu-white-auto-mask-no-fade').removeClass('main-menu-white-fade');
                    $('.main-menu-black-auto-mask').removeClass('main-menu-black-fade-mask');
                    $('.main-menu-black-auto-mask-no-fade').removeClass('main-menu-black-fade');

                    if (width > 768) {
                        $('.main-menu').css('padding-top', '15px');
                        $('.main-menu').css('padding-bottom', '15px');
                    }
                }
            }
        });
    }

    try{
        initTopBar();
        initFooter();
    } catch(err) {
        toast('Init auto compoents error : ' + err,'error', 4000);
    }

    setTimeout(function(){
        //tooltip fix
        $('[data-toggle="tooltip"]').tooltip();
    }, 3500);
}
//顶部菜单初始化
function initTopBar() {

    //logo设置
    if(!isNullOrEmpty(siteLogo)){
        $('#header-logo').css('background-image: url(' + siteLogo + ');width:' + siteLogoSize.width + ';heigth:' + siteLogoSize.height)
    }

    //标题设置
    $('#header-title').text(siteName);
    if(constConfig.autoTitle){
        var text = $('title').text();
        if(!isNullOrEmpty(text)) $('title').text(text + ' - ' + siteName);
        else $('title').text(siteName);
    }

    //菜单项目
    $menuContainer = $('#header-menu');
    for(var key in menuConfig){
        if(location.pathname == menuConfig[key].url) $menuContainer.prepend($('<li class="menu-active"><a>' + menuConfig[key].name + '</a></li>'));
        else $menuContainer.prepend($('<li><a href="' + menuConfig[key].url + '">' + menuConfig[key].name + '</a></li>'));
    }

    //------- Mobile Nav ---------//  
    if ($('#nav-menu-container').length) {
        var $mobile_nav = $('#nav-menu-container').clone().prop({
            id: 'mobile-nav'
        });
        $mobile_nav.find('> ul').attr({
            'class': '',
            'id': ''
        });
        $('body').append($mobile_nav);
        //$('body').prepend('<button type="button" id="mobile-nav-toggle"><i class="lnr lnr-menu"></i></button>');
        $('body').append('<div id="mobile-body-overly"></div>');
        $('#mobile-nav').find('.menu-has-children').prepend('<i class="fa fa-angle-down"></i>').prop('href', 'javascript;');
        $('#mobile-nav').find('.menu-has-children ul').attr('style', 'display:none;');
        $('mobile-body-overly').attr('style', 'display:none;');

        $('.menu-has-children a').click(function (e) {
            $(this).toggleClass('menu-item-active');
            $(this).nextAll('ul').eq(0).slideToggle();
            $(this).prev().toggleClass("reverse-icon");
        });
        $('.menu-has-children i').click(function (e) {
            $(this).next().toggleClass('menu-item-active');
            $(this).nextAll('ul').eq(0).slideToggle();
            $(this).toggleClass("reverse-icon");
        });
        $('#mobile-nav-toggle').click(function (e) {
            $('body').toggleClass('mobile-nav-active');
            $('#mobile-nav-toggle').toggleClass('icon-menu icon-close');
            $('#mobile-body-overly').toggle();
        });

        $(document).click(function (e) {
            var container = $("#mobile-nav, #mobile-nav-toggle");
            if (!container.is(e.target) && container.has(e.target).length === 0) {
                if ($('body').hasClass('mobile-nav-active')) {
                    $('body').removeClass('mobile-nav-active');
                    $('#mobile-nav-toggle i').toggleClass('fa-times fa-bars');
                    $('#mobile-body-overly').fadeOut();
                }
            }
        });
    } else if ($("#mobile-nav, #mobile-nav-toggle").length) {
        $("#mobile-nav, #mobile-nav-toggle").hide();
    }

    //显示搜索框
    if (enableSearch && location.pathname.indexOf('/admin/') != 0) {
        $menuContainer.append($('<li class="nav-search flat-pill">\
                <input type="text" id="nav-searcher" style="display: none;" placeholder="搜索 '+ siteName + ' 上的内容">\
                <a id="nav-show-searcher" href="javascript:;" title="搜索"><i class="fa fa-search" aria-hidden="true"></i></a>\
                <a id="nav-hide-searcher" href="javascript:;" title="取消" style="display:none"><i class="fa fa-times" aria-hidden="true"></i></a>\
            </li>'));
        $mobileMenuContainer = $('#mobile-nav ul');
        $mobileMenuContainer.append($('<li class="nav-search-mobile flat-pill d-flex alidn-items-center justify-content-between">\
            <input type="text" id="nav-searcher-mobile" placeholder="搜索 '+ siteName + ' 上的内容">\
            <a id="nav-show-searcher-mobile" href="javascript:;" title="搜索"><i class="fa fa-search" aria-hidden="true"></i></a>\
        </li>'));

        $('#nav-show-searcher-mobile').click(function () {
            if ($('#nav-searcher-mobile').val() == '') toast('请输入您需要查找的关键词哦！', 'info', 3500);
                else window.open('/search/?word=' + $('#nav-searcher-mobile').val());
        });
        $('.nav-search a#nav-show-searcher').click(function () {
            if ($('#nav-searcher').is(':visible')) {
                if ($('#nav-searcher').val() == '') toast('请输入您需要查找的关键词哦！', 'info', 3500);
                else window.open('/search/?word=' + $('#nav-searcher').val());
            } else {
                $('#nav-hide-searcher').fadeIn();
                $('.nav-search').addClass('show');
                $('#nav-searcher').fadeIn();
            }
        })
        $('.nav-search a#nav-hide-searcher').click(function () {
            $(this).fadeOut();
            $('#nav-show-searcher').fadeIn();
            $('.nav-search').removeClass('show');
            $('#nav-searcher').fadeOut();
        })

    }
}
function initFooter() {

    if(!constConfig.autoFooter) return;

    var footerIcon = '';
    var footerLinks = '';
    if(socialConfig && Object.keys(socialConfig).length > 0){
        for(var key in socialConfig){
            footerIcon += '<li><a href="'+socialConfig[key].url+'" style="color:'+socialConfig[key].color+'" id="footer_' + socialConfig[key].icon + '" class="icon fa fa-' + socialConfig[key].icon + '"><span class="label">' + socialConfig[key].name + '</span></a></li>';
        }
    }
    if(constConfig.footerShowLinks && Object.keys(constConfig.footerShowLinks).length > 0){
        for(var key in constConfig.footerShowLinks){
            footerLinks += '<li><a href="'+constConfig.footerShowLinks[key].url+'" target="_blank">' + constConfig.footerShowLinks[key].name + '</a></li>';
        }
    }

    //生成 Footer
    $footer = $('<!--Footer--><footer id="footer"><ul class="icons">' + footerIcon + '</ul>\
    <ul class="copyright"><li><i class="fa fa-copyright" aria-hidden="true"></i> 2019. All rights reserved</li><li>Design by <a class="link" href="https://www.imyzc.com/about.html">DreamFish</a></li></ul>\
    <ul class="copyright copyright-line"><li><a href="https://www.imyzc.com/support-browser.html" target="_blank" class="badge badge-pill badge-dark">推荐使用 Microsoft Edge 或 Chrome 浏览器浏览本页面</a></li></ul>\
    <ul class="copyright copyright-line">' + footerLinks + '</ul>\
    <ul class="copyright">' + 
        (isNullOrEmpty(constConfig.icpRecord) ? '' : '<li><a class="link" href="https://www.miitbeian.gov.cn">' + constConfig.icpRecord + '</a></li>') + 
        (isNullOrEmpty(constConfig.policeRecord) ? '' : '<li><a class="jgwab" target="_blank" href="'+constConfig.policeRecord.url+'"><p><i></i>' + constConfig.policeRecord.title + '</p></a></li>') +
    '</ul></footer>');
    $('body').append($footer);

    /** Footer button tooltips */
    if(socialConfig && Object.keys(socialConfig).length > 0){
        for(var key in socialConfig){
            if(!isNullOrEmpty(socialConfig[key].tooltip)){
                $('#footer_' + socialConfig[key].icon).popover({
                    trigger: 'hover',
                    html: true,
                    content: socialConfig[key].tooltip,
                    title: socialConfig[key].name,
                    placement: "top"
                });
            }
        }
    }
}

//===============

//PV更新
function isStatExclude(path){
    for(var expath in excludeStatPath){
        if(path.indexOf(excludeStatPath[expath]) == 0) return true;
    }
}
function sendStat(){
    if(sendStats){
        if(document.referrer!=document.location.toString() && !isStatExclude(location.pathname)){

            $.ajax({
                url: address_blog_api + 'stat',
                type: 'post',
                dataType: 'json',
                data: JSON.stringify({ "url": document.location.toString() }),
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                success: function (response) {
                  if (!response.success) toast('发送计数数据失败 : ' + response.message, 'error', 5000);
                }, error: function (xhr, err) {toast('发送计数数据失败 : ' + err, 'error', 5000); }
            });
        }
    }
}

//===============

function scrollToPos(top) {
    $('body,html').animate({ scrollTop: top }, 1000);
}
function scrollToEle(id) {
    if ($(id).length > 0) {
        var top = $(id).offset().top - 70;
        $('body,html').animate({ scrollTop: top }, 1000);
    }
}
function highlightChildCode($id) {
    $('#' + $id + ' pre code').each(function (i, block) {
        $(this).html(replaceBlockBadChr($(this).html()));
        hljs.highlightBlock(block);
    });
}
function highlightAllCode() {
    $('.highlight pre code').each(function (i, block) {
        $(this).html(replaceBlockBadChr($(this).html()));
        hljs.highlightBlock(block);
    });
}
function replaceBlockBadChr(str) {
    return str.replace(/^\s+|\s+$/g, '');
}

// toast('您的信息已成功提交', 'success', 3000);setTimeout(function(){ toast('信息提交失败 <a href="#">重试</a>', 'error', 3000)}, 2000);

//Toasts
//=====================================

var toasts = [];
var toastCurrentTop = 15;
var toastCount = 0;
var toast_overlay;

function toastTypeToIcon(type){
    switch(type){
        case 'error':
            return '<i class="toast-icon fa fa-times-circle-o text-danger"></i>';
        case 'warning':
            return '<i class="toast-icon fa fa-exclamation-triangle text-warning"></i>';
        case 'info':
            return '<i class="toast-icon fa fa-info-circle text-primary"></i>';
        case 'success':
            return '<i class="toast-icon fa fa-check-circle-o text-success"></i>';
        case 'loading':
            return '<i class="toast-icon spinner-grow text-primary" style="width:26px;height:26px" role="status"></i>';
    }
}
function toastRemove(toast, $alert){
    if(toastCurrentTop>15) toastCurrentTop -= $alert.height() + 36;  
    else toastCurrentTop = 15;  

    $alert.remove();

    for(var i = toasts.length - 1, start = toasts.indexOf(toast); i > start; i--)
        toasts[i].top = toasts[i - 1].top;
    for(var i = toasts.indexOf(toast), size = toasts.length; i < size-1; i++) {
        toasts[i] = toasts[i + 1];
        toasts[i].alert.css('top', toasts[i].top);
    }
    toasts.pop(toasts[toasts.length-1]);
    toastCount--;
}
function toastCloseById(uidz){
    for(var k in toasts){
        if(toasts[k].uidz == uidz){
            toastClose(toasts[k]);
            break;
        }
    }
}
function toastClose(toast, anim){
    if(!toast.closed){
        $alert = toast.alert;
        toast.closed = true;
        if(anim=='slide') $alert.slideUp(300, function(){ toastRemove(toast, $(this)) });
        else $alert.fadeOut(600, function(){ toastRemove(toast, $(this)) });
    }
}
function toastClear(time, toast){
    setTimeout(function(){toastClose(toast)},time)
}
function toast(str, type, time, noclose){
    uidz += parseInt(Math.random() * 10);
    if(!time) time = 2500;
    if(time == -1 || time < 2500) noclose = true;

    var top = toastCurrentTop;

    $newAlert = $('<div class="toast-alert" id="toast-' + uidz + '">' + toastTypeToIcon(type) + 
        '<div class="toast-text" style="' +(noclose?'padding-right:20px':'')+'">' +  str + '</div>' +
        (noclose ? '' : '<a class="toast-close" href="javascript:;" onclick="toastCloseById(' + uidz + ')"></a>') + '</div>')
    $('#toast-overlay').append($newAlert);
    $newAlert.css('top', top + 'px');
    $newAlert.css('left', ($(window).width() / 2 - $newAlert.width() / 2) + 'px');

    toastCurrentTop += $newAlert.height() + 36;
    var toast = {
        uidz: uidz,
        alert: $newAlert,
        top: top,
        closed: false
    };
    toasts[toastCount] = toast;
    toastCount++;

    if(time!=-1) toastClear(time, toast);
    return toast;
}

function getHostName() {
    var curWwwPath = window.document.location.href;
    if (curWwwPath.indexOf('https://') == 0)
        curWwwPath = curWwwPath.substr(7);
    else if (curWwwPath.indexOf('https://') == 0)
        curWwwPath = curWwwPath.substr(8);
    var pathName = window.document.location.pathname;
    if (pathName == '') {
        return curWwwPath;
    } else {
        var pos = curWwwPath.indexOf(pathName);
        return curWwwPath.substring(0, pos);
    }
}


window.onload = loaderStart;
