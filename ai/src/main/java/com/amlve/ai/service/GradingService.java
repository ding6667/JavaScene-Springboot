package com.amlve.ai.service;

import java.util.List;

public interface GradingService {

   void runWorkflowWithCommonFiles(List<String> uploadIds, Long standardConfigId);
   String startBatchReviewFromZip(String uploadId, Long standardConfigId);

   /** 暂停指定批阅任务 */
   void pauseReview(String taskId);
   /** 继续指定批阅任务 */
   void resumeReview(String taskId);
   /** 取消指定批阅任务 */
   void cancelReview(String taskId);
   /** 指定任务是否正在运行 */
   boolean isRunning(String taskId);
   /** 指定任务是否已暂停 */
   boolean isPaused(String taskId);
}
