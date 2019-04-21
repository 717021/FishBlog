
var main = new Vue({
  el: '#main',
  data: {
    message: 'Hello Vue.js !',
    contentMainLoading: false,
    contentMainLoadFailed: false,
    contentMainLoadError: "",
    contentPosts: null,
    contentTagsLoaded: false,
    contentTags: null,
    currentPostPage: 0,
    allPostPage: 0,
    contentLoadTag: "none",
    currentTagShow: "所有文章",
  },
  methods: {
    loadStart: function(){
      var currViewTag = getQueryString('tag');
      if(isNullOrEmpty(currViewTag)) currViewTag =  getLastUrlAgrs(location.href);
      if(isNumber(currViewTag.arg)) this.contentLoadTag = currViewTag.arg;
      this.loadTagPosts();
    },
    loadTagPosts: function(){
      var url = address_blog_api + "posts/page/" + this.currentPostPage + "/20?sortBy=date&noTopMost=true&onlyTag=" + this.contentLoadTag;
      var tags_url = address_blog_api + "tags";
      var oldScrollTop = $('body,html').scrollTop();
      this.contentMainLoading = true;
      $.ajax({
        url: url,
        success: function (response) {
          main.contentMainLoading = false;
          if (response.success) {
            main.allPostPage = response.data.totalPages;
            if (main.contentPosts == null) main.contentPosts = response.data.content;
            else main.contentPosts = mergeJsonArray(main.contentPosts, response.data.content);
            main.reloadPostStats();
            main.currentPostPage++;
            $('body,html').scrollTop(oldScrollTop);
          } else {
            main.contentMainLoadError = response.message;
            main.contentMainLoadFailed = true;
          }
        }, error: function (xhr, err) {
          main.contentMainLoadError = err;
          main.contentMainLoadFailed = true;
        }
      });
      $.get(tags_url, function (response) {
        if (response.success) {
          main.contentTagsLoaded = true;
          main.contentTags = response.data;
          //Find current tag name
          for(var key in response.data){ 
            if(response.data[key].id == main.contentLoadTag)
              main.currentTagShow=response.data[key].name;
            }
        }
      }, "json");
    },
    getDateString: function (str) {
      arr = str.split("-");
      return arr[0] + " 年 " + arr[1] + " 月";
    },
    getImageUrl: function (str) {
      return getImageUrlFormHash(str);
    },
    getPostUrl: function (post) {
      return getPostRealUrl(post)
    },
    getPostPrefix: function (prefixId){
      return getPostPrefix(prefixId);
    },
    goViewTag (tagId) {
      location.href = partPositions.viewTag + tagId + '/';
    },
    reloadPostStats: function(){
      var getPosts = { archives: [] };
      for(var key in main.contentPosts)
        if(main.contentPosts[key].id) 
          getPosts.archives.push(main.contentPosts[key].id);

      var findOldPosts = function(id){
        for(var key in main.contentPosts)
          if(main.contentPosts[key].id==id) 
            return main.contentPosts[key];
        return null;
      }
      var reloadPostsStat = function(arr){
        for(var key in arr) {
          var o = findOldPosts(key);
          if(o){
            o.viewCount = arr[key].viewCount;
            o.commentCount = arr[key].commentCount;
          }
        }
      }

      $.ajax({
        url: address_blog_api + 'posts/stat',
        type: 'post',
        data: JSON.stringify(getPosts),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (response) {
          if (response.success) 
            reloadPostsStat(response.data);
        }, error: function (xhr, err) {}
      });
      
    },
  }
})


setLoaderFinishCallback(function () {
  main.loadStart();
})