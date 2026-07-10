package com.amlve.ai.Service;

import java.util.List;

public interface GradingService {

   String runWorkflowWithCommonFiles(List<String> fileUrls, String rubric);
   String startBatchReviewFromZip(String uploadId, String standardConfigId);

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
