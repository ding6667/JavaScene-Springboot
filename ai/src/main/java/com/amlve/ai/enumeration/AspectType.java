package com.amlve.ai.enumeration;

import com.amlve.ai.entity.CommentJson;
import lombok.Getter;

import java.util.function.Function;

@Getter
public enum AspectType {
     TITLE("标题", CommentJson::getTitle),
     SCORING_TABLE("评分细则", CommentJson::getScoringTable),
     MISTAKES("错误", CommentJson::getMistakes),
     ADVANTAGES("优势", CommentJson::getAdvantages),
     SUMMARY("总结", CommentJson::getSummary);


     private final String desc;
     private final Function<CommentJson, String> extractor;

     AspectType(String desc, Function<CommentJson, String> extractor) {
          this.desc = desc;
          this.extractor = extractor;
     }

     // 提取方法
     public String extractFrom(CommentJson json) {
          return extractor.apply(json);
     }
}
