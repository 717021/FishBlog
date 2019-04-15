package com.dreamfish.fishblog.core.service.impl;

import com.dreamfish.fishblog.core.entity.Post;
import com.dreamfish.fishblog.core.entity.PostTag;
import com.dreamfish.fishblog.core.entity.User;
import com.dreamfish.fishblog.core.enums.UserPrivileges;
import com.dreamfish.fishblog.core.mapper.PostMapper;
import com.dreamfish.fishblog.core.repository.PostRepository;
import com.dreamfish.fishblog.core.service.PostService;
import com.dreamfish.fishblog.core.utils.Result;
import com.dreamfish.fishblog.core.utils.ResultCodeEnum;
import com.dreamfish.fishblog.core.utils.StringUtils;
import com.dreamfish.fishblog.core.utils.request.ContextHolderUtils;
import com.dreamfish.fishblog.core.utils.request.RequestUtils;
import com.dreamfish.fishblog.core.utils.response.AuthCode;
import com.dreamfish.fishblog.core.utils.auth.PublicAuth;
import com.dreamfish.fishblog.core.utils.response.PostErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 文章 服务
 */
@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostRepository postRepository = null;
    @Autowired
    private PostMapper postMapper = null;

    @Override
    @Cacheable(value = "blog-simple-reader-cache", key = "'post_'+#p0")
    public Result findPostWithIdOrUrlName(String idOrUrlName, boolean authForRead) {

        HttpServletRequest request = ContextHolderUtils.getRequest();
        Post post;
        if(StringUtils.isInteger(idOrUrlName)) {
            Optional<Post> o = postRepository.findById(Integer.parseInt(idOrUrlName));
            if(!o.isPresent()) return Result.failure(ResultCodeEnum.NOT_FOUNT);
            post = o.get();
        }
        else post =  postRepository.findByUrlName(RequestUtils.encoderURLString(idOrUrlName));

        if(post==null) return Result.failure(ResultCodeEnum.NOT_FOUNT);
        //访问非公开文章需要验证权限
        if(post.getStatus().intValue() != Post.POST_STATUS_PUBLISH) {
            if(authForRead) {

                Integer authorId = PublicAuth.authGetUseId(request);
                if(authorId <= User.LEVEL_LOCKED) return Result.failure(ResultCodeEnum.UNAUTHORIZED, String.valueOf(authorId));
                if(post.getAuthorId() != authorId.intValue()){
                    int authCode = PublicAuth.authCheckIncludeLevelAndPrivileges(request, User.LEVEL_WRITER, UserPrivileges.PRIVILEGE_MANAGE_ALL_ARCHIVES);
                    if (authCode < AuthCode.SUCCESS)  return Result.failure(ResultCodeEnum.FORIBBEN);
                }
            }
            else return Result.failure(ResultCodeEnum.NOT_FOUNT);
        }

        //Get prv and next post title
        Integer prvId = post.getPostPrvId(),
                nextId = post.getPostNextId();
        if(prvId!=0) post.setPostPrvTitle(postMapper.getPostTitle(prvId));
        if(nextId!=0) post.setPostNextTitle(postMapper.getPostTitle(nextId));

        //Get tag names
        String postTags = post.getTags();
        if(!StringUtils.isBlank(postTags)){
            List<PostTag> tagNames = new ArrayList<>();
            String[] tagIds = postTags.split("-");
            for (String id : tagIds) {
                if(!StringUtils.isBlank(id) && StringUtils.isInteger(id))
                    tagNames.add(postMapper.getTag(Integer.parseInt(id)));
            }

            post.setPostTagNames(tagNames);
        }

        return Result.success(post);
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(value = "blog-simple-reader-cache", key = "'post_'+#p0"),
            @CacheEvict(value = "blog-simple-reader-cache", key = "'post_'+#p1.urlName"),
            @CacheEvict(value = "blog-posts-pages-cache", allEntries = true)
        }
    )
    public Result updatePost(Integer id, Post post) {

        HttpServletRequest request = ContextHolderUtils.getRequest();

        if(post.getId().intValue() != id) return Result.failure(ResultCodeEnum.BAD_REQUEST, String.valueOf(PostErrorCode.POST_REQ_ID_ERROR));
        if(!postRepository.existsById(post.getId())) return Result.failure(ResultCodeEnum.NOT_FOUNT);

        Integer authorId = PublicAuth.authGetUseId(request);
        if(authorId <= User.LEVEL_LOCKED) return Result.failure(ResultCodeEnum.UNAUTHORIZED, String.valueOf(authorId));
        Integer postAuthorId = postMapper.getPostAuthorId(id);
        if(postAuthorId.intValue() != authorId.intValue()) {
            int authCode = PublicAuth.authCheckIncludeLevelAndPrivileges(request, User.LEVEL_WRITER, UserPrivileges.PRIVILEGE_MANAGE_ALL_ARCHIVES);
            if(authCode < AuthCode.SUCCESS) return Result.failure(ResultCodeEnum.FORIBBEN);
        }

        return Result.success(postRepository.saveAndFlush(post));
    }
    @Override
    @Caching(
        evict = {
            @CacheEvict(value = "blog-posts-pages-cache", allEntries = true)
        }
    )
    public Result addPost(Post post) {
        return Result.success(postRepository.saveAndFlush(post));
    }
    @Override
    @Caching(
        evict = {
            @CacheEvict(value = "blog-simple-reader-cache", key = "'post_'+#p0"),
            @CacheEvict(value = "blog-posts-pages-cache", allEntries = true)
        }
    )
    public Result deletePost(Integer id) {

        HttpServletRequest request = ContextHolderUtils.getRequest();
        Integer authorId = PublicAuth.authGetUseId(request);
        if(authorId <= User.LEVEL_LOCKED) return Result.failure(ResultCodeEnum.UNAUTHORIZED, String.valueOf(authorId));
        Integer postAuthorId = postMapper.getPostAuthorId(id);
        if(postAuthorId.intValue() != authorId.intValue()) {
            int authCode = PublicAuth.authCheckIncludeLevelAndPrivileges(request, User.LEVEL_WRITER, UserPrivileges.PRIVILEGE_MANAGE_ALL_ARCHIVES);
            if(authCode < AuthCode.SUCCESS) return Result.failure(ResultCodeEnum.FORIBBEN);
        }

        if(!postRepository.existsById(id)) return Result.failure(ResultCodeEnum.NOT_FOUNT);
        postMapper.deletePost(id);
        return Result.success();
    }

    @Override
    @CacheEvict(value = "blog-simple-reader-cache", key = "'post_'+#p0")
    public Result increasePostViewCount(Integer id) {
        postMapper.increasePostValue(id, "view_count");
        return Result.success();
    }


}
