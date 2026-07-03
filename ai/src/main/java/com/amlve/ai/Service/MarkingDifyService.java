package com.amlve.ai.Service;

import java.util.List;
import java.util.Map;

public interface MarkingDifyService {
   Map<String, List<String>> wordConvertToMd(List<String> fileUrls);
   Map<String, String> mdConvertToWord(String mdContent);

   String runWorkflowWithCommonFiles(List<String> fileUrls, String rubric);
   String runWorkflowWithProjectZip(String zipFilePath, String rubric);
}
