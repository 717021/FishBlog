var main_is_showed_more = false;
var main_is_stat_loaded = false;

setLoaderFinishCallback(function () { 
    console.log('%c Welecome DreamFish\'s BLOG ! Version : 1.5.0501.0 ' +
    '%c Wow ! 你发现了我的小秘密 ' +
    '%c 交个朋友好不好？ 😋',
    'color:white;background-color:black;padding:5px 0',
    'color:white;background-color:#6195FF;padding:5px 0',
    'color:#007bff;padding:5px 0');
    gs();
});

function MainSwitch() {
    if (main_is_showed_more) {

        $('#main-anim-switch').removeClass('main-bottom-hidden');
        $('.main-footer').removeClass('main-bottom-hidden');
        $('#main-sw').removeClass('main-bottom-hidden');
        $('#main-more-button i').removeClass('revrse-180');
        $('#main-content').removeClass('main-show-center');
        /*$('#main-content').animate({
            height: $('#main-default').height()
        },800,"linear",function(){
            
        });*/
        setTimeout(function () {
            $('#main-status').slideUp();
        }, 200)
        setTimeout(function () {
            $('#main-content').hide();
            $('#main-default').fadeIn();
        }, 800)
    } else {
        $('#main-more-button i').addClass('revrse-180');
        $('#main-default').fadeOut(500, function () {
            $('#main-content').show();
            $('#main-content').addClass('main-show-center');
            $('#main-status').slideDown();
            $('.main-footer').addClass('main-bottom-hidden');
            $('#main-sw').addClass('main-bottom-hidden');
            $('#main-anim-switch').addClass('main-bottom-hidden');
        });
    }
    main_is_showed_more = !main_is_showed_more;
}
function df(a, e) {
    var c, b, d;
    a = Date.parse(a);
    e = Date.parse(e);
    c = e - a;
    c = Math.abs(c);
    d = Math.floor(c / (24 * 3600 * 1000));
    return d
}
function gs() {
    var a = new Date(),
        b = new Date("2018-11-29"),
        c = df(a, b);
    $("#my-age").text((a.getFullYear() - 2000) + " 岁");
    $("#web-live-time").text(c + " 天");

    $.ajax({
        url: address_blog_api + "stat/today",
        type: "get",
        success: function (r) {
            try {
                if (r.success){
                    var d = r.data;
                    $("#web-day-visit").text(d.pv + " / " + d.ip);
                    $("#web-arc-count").text(d.count + " 篇")
                    console.log('%c 您是今天第 %c ' + d.ip + ' %c 个访问者！ ','color:white;background-color:black;padding:5px 0','color:white;background-color:#6195FF;padding:5px 0','color:white;background-color:black;padding:5px 0');
                }
            } catch (e) { }
        }
    })
}