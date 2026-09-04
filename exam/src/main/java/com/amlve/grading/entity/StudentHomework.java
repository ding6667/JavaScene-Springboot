package com.amlve.grading.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentHomework {
    private String studentId;
    private String studentName;
    private String wordContent;
    /** 学生作业 docx 文件的绝对路径 */
    private String path;

    /** 不含 path 的便捷构造（wordContent 可为 null，后续通过 path 按需加载） */
    public StudentHomework(String studentId, String studentName, String wordContent) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.wordContent = wordContent;
        this.path = null;
    }

    @Override
    public String toString() {
        return String.format("学号: %s, 姓名: %s, 文档长度: %d字符", studentId, studentName,
                wordContent != null ? wordContent.length() : 0);
    }
}