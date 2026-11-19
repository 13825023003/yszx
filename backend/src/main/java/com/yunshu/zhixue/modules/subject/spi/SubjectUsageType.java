package com.yunshu.zhixue.modules.subject.spi;

/**
 * 学科关联内容类型：用于统计与删除保护提示。
 */
public enum SubjectUsageType {

    QUESTION("题目"),
    COURSE("课程"),
    ROUTE("学习路线"),
    INTERVIEW_QUESTION("面试题");

    private final String label;

    SubjectUsageType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
