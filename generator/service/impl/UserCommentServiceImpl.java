package com.yupi.mianshiya.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.mianshiya.common.ErrorCode;
import com.yupi.mianshiya.constant.CommonConstant;
import com.yupi.mianshiya.exception.ThrowUtils;
import com.yupi.mianshiya.mapper.UserCommentMapper;
import com.yupi.mianshiya.model.dto.userComment.UserCommentQueryRequest;
import com.yupi.mianshiya.model.entity.UserComment;
import com.yupi.mianshiya.model.entity.UserCommentFavour;
import com.yupi.mianshiya.model.entity.UserCommentThumb;
import com.yupi.mianshiya.model.entity.User;
import com.yupi.mianshiya.model.vo.UserCommentVO;
import com.yupi.mianshiya.model.vo.UserVO;
import com.yupi.mianshiya.service.UserCommentService;
import com.yupi.mianshiya.service.UserService;
import com.yupi.mianshiya.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户评论服务实现
 *
 * @author <a href="https://github.com/licarlos">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class UserCommentServiceImpl extends ServiceImpl<UserCommentMapper, UserComment> implements UserCommentService {

    @Resource
    private UserService userService;

    /**
     * 校验数据
     *
     * @param userComment
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validUserComment(UserComment userComment, boolean add) {
        ThrowUtils.throwIf(userComment == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = userComment.getTitle();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param userCommentQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<UserComment> getQueryWrapper(UserCommentQueryRequest userCommentQueryRequest) {
        QueryWrapper<UserComment> queryWrapper = new QueryWrapper<>();
        if (userCommentQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = userCommentQueryRequest.getId();
        Long notId = userCommentQueryRequest.getNotId();
        String title = userCommentQueryRequest.getTitle();
        String content = userCommentQueryRequest.getContent();
        String searchText = userCommentQueryRequest.getSearchText();
        String sortField = userCommentQueryRequest.getSortField();
        String sortOrder = userCommentQueryRequest.getSortOrder();
        List<String> tagList = userCommentQueryRequest.getTags();
        Long userId = userCommentQueryRequest.getUserId();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取用户评论封装
     *
     * @param userComment
     * @param request
     * @return
     */
    @Override
    public UserCommentVO getUserCommentVO(UserComment userComment, HttpServletRequest request) {
        // 对象转封装类
        UserCommentVO userCommentVO = UserCommentVO.objToVo(userComment);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = userComment.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        userCommentVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        long userCommentId = userComment.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<UserCommentThumb> userCommentThumbQueryWrapper = new QueryWrapper<>();
            userCommentThumbQueryWrapper.in("userCommentId", userCommentId);
            userCommentThumbQueryWrapper.eq("userId", loginUser.getId());
            UserCommentThumb userCommentThumb = userCommentThumbMapper.selectOne(userCommentThumbQueryWrapper);
            userCommentVO.setHasThumb(userCommentThumb != null);
            // 获取收藏
            QueryWrapper<UserCommentFavour> userCommentFavourQueryWrapper = new QueryWrapper<>();
            userCommentFavourQueryWrapper.in("userCommentId", userCommentId);
            userCommentFavourQueryWrapper.eq("userId", loginUser.getId());
            UserCommentFavour userCommentFavour = userCommentFavourMapper.selectOne(userCommentFavourQueryWrapper);
            userCommentVO.setHasFavour(userCommentFavour != null);
        }
        // endregion

        return userCommentVO;
    }

    /**
     * 分页获取用户评论封装
     *
     * @param userCommentPage
     * @param request
     * @return
     */
    @Override
    public Page<UserCommentVO> getUserCommentVOPage(Page<UserComment> userCommentPage, HttpServletRequest request) {
        List<UserComment> userCommentList = userCommentPage.getRecords();
        Page<UserCommentVO> userCommentVOPage = new Page<>(userCommentPage.getCurrent(), userCommentPage.getSize(), userCommentPage.getTotal());
        if (CollUtil.isEmpty(userCommentList)) {
            return userCommentVOPage;
        }
        // 对象列表 => 封装对象列表
        List<UserCommentVO> userCommentVOList = userCommentList.stream().map(userComment -> {
            return UserCommentVO.objToVo(userComment);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = userCommentList.stream().map(UserComment::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> userCommentIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> userCommentIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> userCommentIdSet = userCommentList.stream().map(UserComment::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            // 获取点赞
            QueryWrapper<UserCommentThumb> userCommentThumbQueryWrapper = new QueryWrapper<>();
            userCommentThumbQueryWrapper.in("userCommentId", userCommentIdSet);
            userCommentThumbQueryWrapper.eq("userId", loginUser.getId());
            List<UserCommentThumb> userCommentUserCommentThumbList = userCommentThumbMapper.selectList(userCommentThumbQueryWrapper);
            userCommentUserCommentThumbList.forEach(userCommentUserCommentThumb -> userCommentIdHasThumbMap.put(userCommentUserCommentThumb.getUserCommentId(), true));
            // 获取收藏
            QueryWrapper<UserCommentFavour> userCommentFavourQueryWrapper = new QueryWrapper<>();
            userCommentFavourQueryWrapper.in("userCommentId", userCommentIdSet);
            userCommentFavourQueryWrapper.eq("userId", loginUser.getId());
            List<UserCommentFavour> userCommentFavourList = userCommentFavourMapper.selectList(userCommentFavourQueryWrapper);
            userCommentFavourList.forEach(userCommentFavour -> userCommentIdHasFavourMap.put(userCommentFavour.getUserCommentId(), true));
        }
        // 填充信息
        userCommentVOList.forEach(userCommentVO -> {
            Long userId = userCommentVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            userCommentVO.setUser(userService.getUserVO(user));
            userCommentVO.setHasThumb(userCommentIdHasThumbMap.getOrDefault(userCommentVO.getId(), false));
            userCommentVO.setHasFavour(userCommentIdHasFavourMap.getOrDefault(userCommentVO.getId(), false));
        });
        // endregion

        userCommentVOPage.setRecords(userCommentVOList);
        return userCommentVOPage;
    }

}
