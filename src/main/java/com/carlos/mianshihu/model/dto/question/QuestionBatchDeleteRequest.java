package com.carlos.mianshihu.model.dto.question;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
/**
 * 批量删除题目请求
 */

public class QuestionBatchDeleteRequest {


    /**
     * 标题
     */
    private String title;

    /**
     * 题目id列表
     */

    private List<Long> questionIdList;

    private static final long serialVersionUID = 1L;
}