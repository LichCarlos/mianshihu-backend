package com.yupi.mianshiya.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.mianshiya.model.dto.userComment.UserCommentQueryRequest;
import com.yupi.mianshiya.model.entity.UserComment;
import com.yupi.mianshiya.model.vo.UserCommentVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户评论服务
 *
 * @author <a href="https://github.com/licarlos">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
public interface UserCommentService extends IService<UserComment> {

    /**
     * 校验数据
     *
     * @param userComment
     * @param add 对创建的数据进行校验
     */
    void validUserComment(UserComment userComment, boolean add);

    /**
     * 获取查询条件
     *
     * @param userCommentQueryRequest
     * @return
     */
    QueryWrapper<UserComment> getQueryWrapper(UserCommentQueryRequest userCommentQueryRequest);
    
    /**
     * 获取用户评论封装
     *
     * @param userComment
     * @param request
     * @return
     */
    UserCommentVO getUserCommentVO(UserComment userComment, HttpServletRequest request);

    /**
     * 分页获取用户评论封装
     *
     * @param userCommentPage
     * @param request
     * @return
     */
    Page<UserCommentVO> getUserCommentVOPage(Page<UserComment> userCommentPage, HttpServletRequest request);
}
