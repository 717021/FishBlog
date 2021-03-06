package com.dreamfish.fishblog.core.mapper;

import com.dreamfish.fishblog.core.entity.PostSimple;
import com.dreamfish.fishblog.core.entity.PostStat;
import com.dreamfish.fishblog.core.entity.PostTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostSimpleMapper {


    /**
     * 获取文章简要信息并分页
     * @param startIndex 起始记录位置
     * @param pageSize 一页的大小
     * @return 返回文章数组
     */
    @Select("SELECT * FROM fish_posts WHERE status=1 AND show_in_list=1 LIMIT #{startIndex},#{pageSize}")
    List<PostSimple> getPosts(@Param("startIndex") Integer startIndex, @Param("pageSize") Integer pageSize);

    @Select("SELECT * WHERE status=1 AND show_in_list=1 ORDER BY ${order} LIMIT #{startIndex},#{pageSize}")
    List<PostSimple> getPostsOrderBy(@Param("startIndex") Integer startIndex, @Param("pageSize") Integer pageSize, @Param("order") String order);

    /**
     * 获取所有文章简要信息
     * @return 返回文章数组
     */
    @Select("SELECT * FROM fish_posts WHERE status=1 AND show_in_list=1")
    List<PostSimple> getAllPosts();
    @Select("SELECT * WHERE status=1 AND show_in_list=1 ORDER BY ${order}")
    List<PostSimple> getAllPostsOrderBy(@Param("order") String order);
    @Select("SELECT * FROM fish_posts WHERE status=1 AND show_in_list=1 LIMIT #{maxSize}")
    List<PostSimple> getAllPostsWithLimit(@Param("maxSize") Integer maxSize);
    @Select("SELECT * FROM fish_posts WHERE status=1 AND show_in_list=1 ORDER BY ${order} LIMIT #{maxSize}")
    List<PostSimple> getAllPostsWithLimitOrderBy(@Param("maxSize") Integer maxSize, @Param("order") String order);

    @Select("SELECT * FROM fish_posts WHERE status=1 AND show_in_list=1 AND tags LIKE '%-${tag}-%'")
    List<PostSimple> getTagPosts(@Param("tag") String tag);
    @Select("SELECT * FROM fish_posts WHERE status=1 AND show_in_list=1 AND tags LIKE '%-${tag}-%' LIMIT #{startIndex},#{pageSize}")
    List<PostSimple> getTagPostsWithLimit(@Param("startIndex") Integer startIndex, @Param("pageSize") Integer pageSize, @Param("tag") String tag);
    @Select("SELECT * WHERE status=1 AND show_in_list=1 AND tags LIKE '%-${tag}-%' ORDER BY ${order} LIMIT #{startIndex},#{pageSize}")
    List<PostSimple> getTagPostsWithLimitOrderBy(@Param("startIndex") Integer startIndex, @Param("pageSize") Integer pageSize, @Param("order") String order, @Param("tag") String tag);
    @Select("SELECT * WHERE status=1 AND show_in_list=1 AND tags LIKE '%-${tag}-%' ORDER BY ${order}")
    List<PostSimple> getTagPostsOrderBy(@Param("order") String order, @Param("tag") String tag);

    @Select("SELECT view_count,comment_count,like_count FROM fish_posts WHERE id=#{id}")
    PostStat getPostStatById(@Param("id") Integer id);

    /**
     * 获取文章作者 ID
     * @param id 文章 ID
     * @return
     */
    @Select("SELECT author_id FROM fish_posts WHERE post_id=#{id}")
    Integer getPostAuthorId(@Param("id") Integer id);
}
