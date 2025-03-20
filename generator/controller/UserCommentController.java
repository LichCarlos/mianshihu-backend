package com.yupi.mianshiya.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.mianshiya.annotation.AuthCheck;
import com.yupi.mianshiya.common.BaseResponse;
import com.yupi.mianshiya.common.DeleteRequest;
import com.yupi.mianshiya.common.ErrorCode;
import com.yupi.mianshiya.common.ResultUtils;
import com.yupi.mianshiya.constant.UserConstant;
import com.yupi.mianshiya.exception.BusinessException;
import com.yupi.mianshiya.exception.ThrowUtils;
import com.yupi.mianshiya.model.dto.userComment.UserCommentAddRequest;
import com.yupi.mianshiya.model.dto.userComment.UserCommentEditRequest;
import com.yupi.mianshiya.model.dto.userComment.UserCommentQueryRequest;
import com.yupi.mianshiya.model.dto.userComment.UserCommentUpdateRequest;
import com.yupi.mianshiya.model.entity.UserComment;
import com.yupi.mianshiya.model.entity.User;
import com.yupi.mianshiya.model.vo.UserCommentVO;
import com.yupi.mianshiya.service.UserCommentService;
import com.yupi.mianshiya.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户评论接口
 *
 * @author <a href="https://github.com/licarlos">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/userComment")
@Slf4j
public class UserCommentController {

    @Resource
    private UserCommentService userCommentService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建用户评论
     *
     * @param userCommentAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addUserComment(@RequestBody UserCommentAddRequest userCommentAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userCommentAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        UserComment userComment = new UserComment();
        BeanUtils.copyProperties(userCommentAddRequest, userComment);
        // 数据校验
        userCommentService.validUserComment(userComment, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        userComment.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = userCommentService.save(userComment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newUserCommentId = userComment.getId();
        return ResultUtils.success(newUserCommentId);
    }

    /**
     * 删除用户评论
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUserComment(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        UserComment oldUserComment = userCommentService.getById(id);
        ThrowUtils.throwIf(oldUserComment == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldUserComment.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = userCommentService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户评论（仅管理员可用）
     *
     * @param userCommentUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUserComment(@RequestBody UserCommentUpdateRequest userCommentUpdateRequest) {
        if (userCommentUpdateRequest == null || userCommentUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        UserComment userComment = new UserComment();
        BeanUtils.copyProperties(userCommentUpdateRequest, userComment);
        // 数据校验
        userCommentService.validUserComment(userComment, false);
        // 判断是否存在
        long id = userCommentUpdateRequest.getId();
        UserComment oldUserComment = userCommentService.getById(id);
        ThrowUtils.throwIf(oldUserComment == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = userCommentService.updateById(userComment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户评论（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserCommentVO> getUserCommentVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        UserComment userComment = userCommentService.getById(id);
        ThrowUtils.throwIf(userComment == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(userCommentService.getUserCommentVO(userComment, request));
    }

    /**
     * 分页获取用户评论列表（仅管理员可用）
     *
     * @param userCommentQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserComment>> listUserCommentByPage(@RequestBody UserCommentQueryRequest userCommentQueryRequest) {
        long current = userCommentQueryRequest.getCurrent();
        long size = userCommentQueryRequest.getPageSize();
        // 查询数据库
        Page<UserComment> userCommentPage = userCommentService.page(new Page<>(current, size),
                userCommentService.getQueryWrapper(userCommentQueryRequest));
        return ResultUtils.success(userCommentPage);
    }

    /**
     * 分页获取用户评论列表（封装类）
     *
     * @param userCommentQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserCommentVO>> listUserCommentVOByPage(@RequestBody UserCommentQueryRequest userCommentQueryRequest,
                                                               HttpServletRequest request) {
        long current = userCommentQueryRequest.getCurrent();
        long size = userCommentQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<UserComment> userCommentPage = userCommentService.page(new Page<>(current, size),
                userCommentService.getQueryWrapper(userCommentQueryRequest));
        // 获取封装类
        return ResultUtils.success(userCommentService.getUserCommentVOPage(userCommentPage, request));
    }

    /**
     * 分页获取当前登录用户创建的用户评论列表
     *
     * @param userCommentQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<UserCommentVO>> listMyUserCommentVOByPage(@RequestBody UserCommentQueryRequest userCommentQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(userCommentQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        userCommentQueryRequest.setUserId(loginUser.getId());
        long current = userCommentQueryRequest.getCurrent();
        long size = userCommentQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<UserComment> userCommentPage = userCommentService.page(new Page<>(current, size),
                userCommentService.getQueryWrapper(userCommentQueryRequest));
        // 获取封装类
        return ResultUtils.success(userCommentService.getUserCommentVOPage(userCommentPage, request));
    }

    /**
     * 编辑用户评论（给用户使用）
     *
     * @param userCommentEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editUserComment(@RequestBody UserCommentEditRequest userCommentEditRequest, HttpServletRequest request) {
        if (userCommentEditRequest == null || userCommentEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        UserComment userComment = new UserComment();
        BeanUtils.copyProperties(userCommentEditRequest, userComment);
        // 数据校验
        userCommentService.validUserComment(userComment, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = userCommentEditRequest.getId();
        UserComment oldUserComment = userCommentService.getById(id);
        ThrowUtils.throwIf(oldUserComment == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldUserComment.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = userCommentService.updateById(userComment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
