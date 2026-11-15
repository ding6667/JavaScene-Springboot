package com.amlve.teachapi.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生作业详情（teach → study / exam）
 * 含作业元信息 + 题目快照（去答案）
 */
@Data
public class StudentHomeworkDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long homeworkId;
    private String title;
    private String content;
    private Long chapterId;
    private String chapterName;
    private Integer totalScore;
    private Integer questionCount;
    private LocalDateTime deadline;
    private Integer allowLateSubmit;
    private LocalDateTime publishTime;
    private List<StudentQuestionItem> questions;

    @Data
    public static class StudentQuestionItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 作业题目快照ID（teach_homework_question.id），仅用于前端列表 key/排序，不要拿去提交 */
        private Long id;

        /**
         * 题库题目ID（exam_question.id），提交作业答案时必须用它。
         * <p>
         * 作业提交接口按题库ID校验题目并按题库分值批改，与快照ID不是同一套主键。
         */
        private Long questionId;

        private String content;

        /** 简短题目：一句话概括题意（如「最长k个相等字符串」），作答页标题/列表直接用 */
        private String shortTitle;

        /**
         * 前端渲染配置（JSON 对象）：
         * 填空 {@code {"blankCount":3}} 表示要留几个空；
         * 选择 {@code {"choiceMode":"single"|"multiple"}} 表示单选还是多选；
         * 其它题型可能为空对象或 null。
         */
        private JsonNode renderConfig;

        private Integer type;
        private String typeName;
        private List<OptionItem> options;

        /**
         * 编程题代码框架：学生答题编辑器的初始内容。
         * 发布作业时已固化，题库后续修改框架不影响本次作业。
         */
        private String codeFramework;

        /** 作答要求（主观题/编程题），发布时固化的快照 */
        private List<String> requirements;

        /** 阶梯提示：前端做「显示提示」时逐条展开，不计分 */
        private List<String> hints;

        private Integer score;
        private Integer sortOrder;
    }

    @Data
    public static class OptionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String key;
        private String value;
    }
}