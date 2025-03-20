package com.yupi.mianshiya.model.vo;

import cn.hutool.json.JSONUtil;
import com.yupi.mianshiya.model.entity.UserComment;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户评论视图
 *
 * @author <a href="https://github.com/licarlos">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Data
public class UserCommentVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 封装类转对象
     *
     * @param userCommentVO
     * @return
     */
    public static UserComment voToObj(UserCommentVO userCommentVO) {
        if (userCommentVO == null) {
            return null;
        }
        UserComment userComment = new UserComment();
        BeanUtils.copyProperties(userCommentVO, userComment);
        List<String> tagList = userCommentVO.getTagList();
        userComment.setTags(JSONUtil.toJsonStr(tagList));
        return userComment;
    }

    /**
     * 对象转封装类
     *
     * @param userComment
     * @return
     */
    public static UserCommentVO objToVo(UserComment userComment) {
        if (userComment == null) {
            return null;
        }
        UserCommentVO userCommentVO = new UserCommentVO();
        BeanUtils.copyProperties(userComment, userCommentVO);
        userCommentVO.setTagList(JSONUtil.toList(userComment.getTags(), String.class));
        return userCommentVO;
    }
}
