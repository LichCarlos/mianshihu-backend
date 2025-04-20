package com.carlos.mianshihu.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carlos.mianshihu.common.ErrorCode;
import com.carlos.mianshihu.constant.CommonConstant;
import com.carlos.mianshihu.exception.BusinessException;
import com.carlos.mianshihu.exception.ThrowUtils;
import com.carlos.mianshihu.manager.AiManager;
import com.carlos.mianshihu.mapper.QuestionMapper;
import com.carlos.mianshihu.model.dto.question.QuestionQueryRequest;
import com.carlos.mianshihu.model.entity.Question;
import com.carlos.mianshihu.model.entity.QuestionBankQuestion;
import com.carlos.mianshihu.model.entity.User;
import com.carlos.mianshihu.model.vo.QuestionVO;
import com.carlos.mianshihu.model.vo.UserVO;
import com.carlos.mianshihu.service.QuestionBankQuestionService;
import com.carlos.mianshihu.service.QuestionService;
import com.carlos.mianshihu.service.UserService;
import com.carlos.mianshihu.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 题目服务实现
 *
 */
@Service
@Slf4j
public class  QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private UserService userService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Resource
    private AiManager aiManager;
    /**
     * 校验数据
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = question.getTitle();
        String content = question.getContent();
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
        if (StringUtils.isNotBlank(content)) {
            ThrowUtils.throwIf(content.length() > 10240, ErrorCode.PARAMS_ERROR, "内容过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        String searchText = questionQueryRequest.getSearchText();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        List<String> tagList = questionQueryRequest.getTags();
        Long userId = questionQueryRequest.getUserId();
        String answer = questionQueryRequest.getAnswer();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
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
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);
        // endregion
        return questionVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            return QuestionVO.objToVo(question);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
        });
        // endregion

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    /**
     * 分页获取题目列表
     *
     * @param questionQueryRequest
     * @return
     */
    public Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 题目表的查询条件
        QueryWrapper<Question> queryWrapper = this.getQueryWrapper(questionQueryRequest);
        // 根据题库查询题目列表接口
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        if (questionBankId != null) {
            // 查询题库内的题目 id
            LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .select(QuestionBankQuestion::getQuestionId)
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
            List<QuestionBankQuestion> questionList = questionBankQuestionService.list(lambdaQueryWrapper);
            if (CollUtil.isNotEmpty(questionList)) {
                // 取出题目 id 集合
                Set<Long> questionIdSet = questionList.stream()
                        .map(QuestionBankQuestion::getQuestionId)
                        .collect(Collectors.toSet());
                // 复用原有题目表的查询条件
                queryWrapper.in("id", questionIdSet);
            }
        }
        // 查询数据库
        Page<Question> questionPage = this.page(new Page<>(current, size), queryWrapper);
        return questionPage;
    }
    /**
     * 批量删除题目
     *
     * @param questionIdList
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteQuestions(List<Long> questionIdList) {
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "要删除的题目列表不能为空");

        // 检查题目是否存在
        List<Question> questions = this.listByIds(questionIdList);
        ThrowUtils.throwIf(questions.size() != questionIdList.size(), ErrorCode.PARAMS_ERROR, "部分题目不存在");

        // 获取所有关联关系
        LambdaQueryWrapper<QuestionBankQuestion> relationWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .in(QuestionBankQuestion::getQuestionId, questionIdList);
        List<QuestionBankQuestion> relations = questionBankQuestionService.list(relationWrapper);

        // 找出未关联的题目 ID
        List<Long> missingRelationIds = questionIdList.stream()
                .filter(id -> !relations.stream().map(QuestionBankQuestion::getQuestionId).collect(Collectors.toList()).contains(id))
                .collect(Collectors.toList());

        // 如果有未关联的题目，可以选择跳过或直接删除
        if (!missingRelationIds.isEmpty()) {
            log.warn("以下题目未关联，将直接删除: {}", missingRelationIds);
            // 直接软删除未关联的题目
            LambdaUpdateWrapper<Question> unassociatedQuestionWrapper = Wrappers.lambdaUpdate(Question.class)
                    .in(Question::getId, missingRelationIds)
                    .set(Question::getIsDelete, 1); // 标记为已删除
            boolean removedUnassociatedQuestions = this.update(unassociatedQuestionWrapper);
            ThrowUtils.throwIf(!removedUnassociatedQuestions, ErrorCode.OPERATION_ERROR, "删除未关联题目失败");
        }

        // 过滤掉未关联的题目，处理剩余有关联关系的题目
        questionIdList = questionIdList.stream()
                .filter(id -> !missingRelationIds.contains(id))
                .collect(Collectors.toList());

        // 如果还有有关联关系的题目，则删除关联关系和题目
        if (CollUtil.isNotEmpty(questionIdList)) {
            // 删除关联关系
            try {
                boolean removedRelations = questionBankQuestionService.removeBatchByIds(
                        relations.stream().map(QuestionBankQuestion::getId).collect(Collectors.toList()));
                if (!removedRelations) {
                    log.error("删除关联关系失败，题目 ID 列表: {}", questionIdList);
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除关联关系失败，请稍后重试");
                }
            } catch (DataAccessException e) {
                log.error("数据库访问异常", e);
                throw new BusinessException(ErrorCode.DATABASE_ERROR, "数据库访问异常，请联系管理员");
            } catch (Exception e) {
                log.error("未知异常", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统异常，请联系管理员");
            }

            // 软删除题目（更新 isDelete 字段）
            LambdaUpdateWrapper<Question> questionWrapper = Wrappers.lambdaUpdate(Question.class)
                    .in(Question::getId, questionIdList)
                    .set(Question::getIsDelete, 1); // 标记为已删除
            boolean removedQuestions = this.update(questionWrapper);
            ThrowUtils.throwIf(!removedQuestions, ErrorCode.OPERATION_ERROR, "删除题目失败");
        }

        log.info("批量删除成功，题目 ID 列表: {}", questionIdList);
    }
    /**
     * AI 生成题目
     *
     * @param questionType 题目类型，比如 Java
     * @param number       题目数量，比如 10
     * @param user         创建人
     * @return ture / false
     */
    @Override
    public boolean aiGenerateQuestions(String questionType, int number, User user) {
        if (ObjectUtil.hasEmpty(questionType, number, user)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        // 1. 定义系统 Prompt
        String systemPrompt = "你是一位专业的程序员面试官，你要帮我生成 {数量} 道 {方向} 面试题，要求输出格式如下：\n" +
                "\n" +
                "1. 什么是 Java 中的反射？\n" +
                "2. Java 8 中的 Stream API 有什么作用？\n" +
                "3. xxxxxx\n" +
                "\n" +
                "除此之外，请不要输出任何多余的内容，不要输出开头、也不要输出结尾，只输出上面的列表。\n" +
                "\n" +
                "接下来我会给你要生成的题目{数量}、以及题目{方向}\n";
        // 2. 拼接用户 Prompt
        String userPrompt = String.format("题目数量：%s, 题目方向：%s", number, questionType);
        // 3. 调用 AI 生成题目
        String answer = aiManager.doChat(systemPrompt, userPrompt);
        // 4. 对题目进行预处理
        // 按行拆分
        List<String> lines = Arrays.asList(answer.split("\n"));
        // 移除序号和 `
        List<String> titleList = lines.stream()
                .map(line -> StrUtil.removePrefix(line, StrUtil.subBefore(line, " ", false))) // 移除序号
                .map(line -> line.replace("`", "")) // 移除 `
                .collect(Collectors.toList());
        // 5. 保存题目到数据库中
        List<Question> questionList = titleList.stream().map(title -> {
            Question question = new Question();
            question.setTitle(title);
            question.setContent("待审核");
            question.setUserId(user.getId());
            question.setTags("[\"待审核\"]");
            // 优化点：可以并发生成
            question.setAnswer(aiGenerateQuestionAnswer(title));
            return question;
        }).collect(Collectors.toList());
        boolean result = this.saveBatch(questionList);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存题目失败");
        }
        return true;
    }

    /**
     * AI 生成题解
     *
     * @param questionTitle
     * @return
     */
    private String aiGenerateQuestionAnswer(String questionTitle) {
        // 1. 定义系统 Prompt
        String systemPrompt = "你是一位专业的程序员面试官，我会给你一道面试题，请帮我生成详细的题解。要求如下：\n" +
                "\n" +
                "1. 题解的语句要自然流畅\n" +
                "2. 题解可以先给出总结性的回答，再详细解释\n" +
                "3. 要使用 Markdown 语法输出\n" +
                "\n" +
                "除此之外，请不要输出任何多余的内容，不要输出开头、也不要输出结尾，只输出题解。\n" +
                "\n" +
                "接下来我会给你要生成的面试题";
        // 2. 拼接用户 Prompt
        String userPrompt = String.format("面试题：%s", questionTitle);
        // 3. 调用 AI 生成题解
        return aiManager.doChat(systemPrompt, userPrompt);
    }

}
