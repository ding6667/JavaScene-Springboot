package com.amlve.system.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入结果。单条失败不回滚整批，失败清单原样返回
 */
@Data
public class BatchImportResponse {

    private Integer total = 0;

    private Integer successNum = 0;

    private Integer failNum = 0;

    private List<FailItem> failList = new ArrayList<>();

    public void addSuccess() {
        this.successNum++;
    }

    /**
     * @param line 出错行号（从 1 开始，跳过注释与空行后的有效行序号），供管理员回文件定位
     */
    public void addFail(int line, String studentNo, String realName, String reason) {
        this.failNum++;
        this.failList.add(new FailItem(line, studentNo, realName, reason));
    }

    @Data
    public static class FailItem {

        private Integer line;

        private String studentNo;

        private String realName;

        private String reason;

        public FailItem() {
        }

        public FailItem(Integer line, String studentNo, String realName, String reason) {
            this.line = line;
            this.studentNo = studentNo;
            this.realName = realName;
            this.reason = reason;
        }
    }
}
