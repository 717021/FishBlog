package com.dreamfish.fishblog.core.service.impl;

import com.dreamfish.fishblog.core.entity.PostComment;
import com.dreamfish.fishblog.core.entity.User;
import com.dreamfish.fishblog.core.enums.UserPrivileges;
import com.dreamfish.fishblog.core.exception.NoPrivilegeException;
import com.dreamfish.fishblog.core.mapper.PostCommentMapper;
import com.dreamfish.fishblog.core.mapper.PostMapper;
import com.dreamfish.fishblog.core.mapper.SettingsMapper;
import com.dreamfish.fishblog.core.mapper.UserMapper;
import com.dreamfish.fishblog.core.repository.PostCommentRepository;
import com.dreamfish.fishblog.core.repository.PostRepository;
import com.dreamfish.fishblog.core.service.MessagesService;
import com.dreamfish.fishblog.core.service.PostCommentService;
import com.dreamfish.fishblog.core.service.SettingsService;
import com.dreamfish.fishblog.core.utils.Result;
import com.dreamfish.fishblog.core.utils.ResultCodeEnum;
import com.dreamfish.fishblog.core.utils.log.ActionLog;
import com.dreamfish.fishblog.core.utils.request.ContextHolderUtils;
import com.dreamfish.fishblog.core.utils.request.IpUtil;
import com.dreamfish.fishblog.core.utils.response.AuthCode;
import com.dreamfish.fishblog.core.utils.auth.PublicAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Service
public class PostCommentServiceImpl implements PostCommentService {

    @Autowired
    private PostRepository postRepository = null;
    @Autowired
    private PostCommentRepository postCommentRepository = null;
    @Autowired
    private PostCommentMapper postCommentMapper = null;
    @Autowired
    private PostMapper postMapper = null;
    @Autowired
    private UserMapper userMapper = null;
    @Autowired
    private MessagesService messagesService = null;
    @Autowired
    private SettingsService settingsService = null;

    @Override
    @Cacheable(value = "blog-single-reader-cache", key = "'comment-single-'+#commentId")
    public Result getOneComment(Integer postId, Integer commentId) {
        List<PostComment> comment = postCommentMapper.getComment(commentId);
        return Result.success(comment);
    }
    @Override
    @Cacheable(value = "blog-single-reader-cache", key = "'comment-in-post-'+#p0")
    public Result getCommentsForPost(Integer postId) {
        if(!postRepository.existsById(postId)) return Result.failure(ResultCodeEnum.NOT_FOUNT);
        return Result.success(postCommentRepository.findByPostId(postId, new Sort(Sort.Direction.DESC, "postDate")));
    }
    @Override
    @Cacheable(value = "blog-comment-pages-cache", key = "'comment-in-post-page-'+#p0+'-'+#p1+'-'+#p2")
    public Result getCommentsForPostWithPager(Integer postId, Integer page, Integer pageSize) {
        if(!postRepository.existsById(postId)) return Result.failure(ResultCodeEnum.NOT_FOUNT);
        Pageable pageable = PageRequest.of(page, pageSize, new Sort(Sort.Direction.DESC, "postDate"));
        return Result.success(postCommentRepository.findByPostId(postId, pageable));
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(value = "blog-single-reader-cache", key = "'comment-single-'+#p1.id"),
            @CacheEvict(value = "blog-single-reader-cache", key = "'comment-in-post-'+#p0"),
            @CacheEvict(value = "blog-simple-reader-cache", key = "'post_stat_'+#p0"),
            @CacheEvict(value = "blog-comment-pages-cache", allEntries = true)
        }
    )
    public Result addCommentInPost(Integer postId, PostComment postComment, HttpServletRequest request)
    {
        if(!postRepository.existsById(postId)) return Result.failure(ResultCodeEnum.NOT_FOUNT);
        if(postComment.getId()!=null && postCommentRepository.existsById(postComment.getId()))
            return Result.failure(ResultCodeEnum.FAILED_RES_ALREADY_EXIST);

        Integer authUserId = PublicAuth.authGetUseId(request);
        if(authUserId > AuthCode.UNKNOW) {
            postComment.setAuthorId(authUserId);
            postComment.setAuthorName(userMapper.getUserFriendlyNameById(authUserId));
        }else{
            if(!Boolean.parseBoolean(settingsService.getSettingCache("AnonymousComment")))
                return Result.failure(ResultCodeEnum.FAILED_NOT_ALLOW.getCode(),"不允许匿名用户评论");
        }

        postComment.setId(0);
        postComment.setAuthorIp(IpUtil.getIpAddr(request));
        postComment.setAuthorUa(request.getHeader("HTTP_USER_AGENT"));
        postComment = postCommentRepository.save(postComment);

        Result result = Result.success(postComment);

        //Update count post comment count
        postMapper.increasePostValue(postId,"comment_count");

        //推送消息
        //推送至文章作者
        Integer postUserId = postMapper.getPostAuthorId(postId);
        if(postUserId > 0 && postUserId.intValue() != postComment.getAuthorId()) messagesService.sendMessage(postUserId, 0, "您有一个新回复！", postComment.getAuthorName() + " 回复了你的文章 <a href=\"/archives/post/"+postId+"/\" target=\"_blank\">" + postMapper.getPostTitle(postId) + "</a> ，去看看吧！");
        //推送至父评论
        Integer parentCommentId = postComment.getParentComment();
        if(parentCommentId != null && parentCommentId > 0){
            postUserId = postCommentMapper.getCommentUserId(parentCommentId);
            if(postUserId > 0 && postUserId.intValue() != postComment.getAuthorId()) messagesService.sendMessage(postUserId, 0, "您有一个新回复！", postComment.getAuthorName() + " 回复了你的评论 <a href=\"/archives/post/"+postId+"/#comment-" +  parentCommentId + "\" target=\"_blank\">" + postMapper.getPostTitle(postId) + "</a> #" +  parentCommentId + " ，去看看吧！");
        }

        //日志
        ActionLog.logUserAction("创建评论 : " + postId + " : " + postComment.getId(), ContextHolderUtils.getRequest());

        return result;
    }
    @Override
    @Caching(
        evict = {
            @CacheEvict(value = "blog-single-reader-cache", key = "'comment-single-'+#p1"),
            @CacheEvict(value = "blog-single-reader-cache", key = "'comment-in-post-'+#p0"),
            @CacheEvict(value = "blog-simple-reader-cache", key = "'post_stat_'+#p0"),
            @CacheEvict(value = "blog-comment-pages-cache", allEntries = true)
        }
    )
    public Result deleteCommentInPost(Integer postId, Integer commentId, HttpServletRequest request)
    {
        Integer authCode = PublicAuth.authCheck(request);
        boolean authHasPrivilege = PublicAuth.authCheckIncludeLevelAndPrivileges(request, User.LEVEL_WRITER, UserPrivileges.PRIVILEGE_MANAGE_ALL_ARCHIVES) == AuthCode.SUCCESS;
        if(authCode < AuthCode.SUCCESS) return Result.failure(ResultCodeEnum.UNAUTHORIZED, String.valueOf(authCode));

        Integer authUserId = PublicAuth.authGetUseId(request);
        if(authUserId <= AuthCode.UNKNOW) return Result.failure(ResultCodeEnum.UNAUTHORIZED, String.valueOf(authCode));

        if(!postRepository.existsById(postId)) return Result.failure(ResultCodeEnum.NOT_FOUNT);
        if(!postCommentRepository.existsById(commentId)) return Result.failure(ResultCodeEnum.NOT_FOUNT);

        Integer postUserId = postCommentMapper.getCommentUserId(commentId);
        if(postUserId <= AuthCode.UNKNOW  && !authHasPrivilege) return Result.failure(ResultCodeEnum.FORIBBEN);
        if(authUserId.intValue() != postUserId.intValue() && !authHasPrivilege) return Result.failure(ResultCodeEnum.FORIBBEN);

        ActionLog.logUserAction("删除评论 : " + postId + " : " +commentId, ContextHolderUtils.getRequest());
        postCommentMapper.deleteComment(commentId);

        //Update count post comment count
        postMapper.decreasePostValue(postId,"comment_count");

        return Result.success();
    }
    @Override
    @Caching(
        evict = {
            @CacheEvict(value = "blog-single-reader-cache", key = "'comment-single-'+#p1.id"),
            @CacheEvict(value = "blog-single-reader-cache", key = "'comment-in-post-'+#p0"),
            @CacheEvict(value = "blog-simple-reader-cache", key = "'post_stat_'+#p0"),
            @CacheEvict(value = "blog-comment-pages-cache", allEntries = true)
        }
    )
    public Result updateCommentInPost(Integer postId, PostComment postComment, HttpServletRequest request)
    {
        Integer authCode = PublicAuth.authCheck(request);
        if(authCode < AuthCode.SUCCESS) return Result.failure(ResultCodeEnum.UNAUTHORIZED, String.valueOf(authCode));

        Integer authUserId = PublicAuth.authGetUseId(request);
        if(authUserId <= AuthCode.UNKNOW) return Result.failure(ResultCodeEnum.UNAUTHORIZED, String.valueOf(authCode));

        if(!postCommentRepository.existsById(postComment.getId()))
            return Result.failure(ResultCodeEnum.NOT_FOUNT);

        Integer postUserId = postCommentMapper.getCommentUserId(postComment.getId());
        if(authUserId.intValue() != postUserId.intValue()) return Result.failure(ResultCodeEnum.FORIBBEN);

        postComment.setAuthorIp(IpUtil.getIpAddr(request));
        postComment.setAuthorUa(request.getHeader("HTTP_USER_AGENT"));

        ActionLog.logUserAction("更新评论 : " + postId + " : " + postComment.getId(), ContextHolderUtils.getRequest());

        return Result.success(postCommentRepository.save(postComment));
    }

    @Caching(
        evict = {
             @CacheEvict(value = "blog-single-reader-cache", allEntries = true),
             @CacheEvict(value = "blog-comment-pages-cache", allEntries = true)
        }
    )
    @Override
    public Result deleteCommentsForPost(Integer postId, List<Integer> ids, HttpServletRequest request) throws NoPrivilegeException {

        //检查当前用户是否有权限删除文章
        int userId = PublicAuth.authGetUseId(request);
        Integer postAuthorId = postMapper.getPostAuthorId(postId);
        if(postAuthorId == null) return Result.failure(ResultCodeEnum.NOT_FOUNT);
        if(userId < AuthCode.SUCCESS) throw new NoPrivilegeException("未授权操作", 403);

        //检查是否是当前用户的文章
        if(userId != postAuthorId) {
            //检查用户是否有管理文章的权限
            int authCode = PublicAuth.authCheckIncludeLevelAndPrivileges(request, User.LEVEL_WRITER, UserPrivileges.PRIVILEGE_MANAGE_ALL_ARCHIVES);
            if (authCode < AuthCode.SUCCESS) throw new NoPrivilegeException("未授权操作", 403);
        }

        //日志
        StringBuilder idStrs = new StringBuilder("[");
        for(Integer id : ids) {
            idStrs.append(",");
            idStrs.append(id);
        }
        idStrs.append("]");
        ActionLog.logUserAction("删除评论组：" + postId + " : " + idStrs.toString(), ContextHolderUtils.getRequest());

        //写入数据库进行删除
        postCommentRepository.deleteByPostIdAndIdIn(postId, ids);
        return Result.success();
    }



}
