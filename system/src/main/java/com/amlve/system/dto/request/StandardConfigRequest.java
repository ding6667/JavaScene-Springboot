package com.amlve.system.dto.request;


import com.amlve.common.entity.RubricItem;

import com.amlve.system.constant.ErrorMessageConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardConfigRequest {


    /**
     * 标准名称
     */
    @NotBlank(message = ErrorMessageConstant.STANDARD_NAME_EMPTY)
    private String standardName;

    /**
     * 评分标准内容(Markdown)
     */
    @NotBlank(message = ErrorMessageConstant.STANDARD_CONTENT_EMPTY)
    private List<RubricItem> standardContent;

    /**
     * 是否当前使用: 0=否, 1=是
     */
    @NotBlank(message = ErrorMessageConstant.IS_CURRENT_EMPTY)
    private Integer isCurrent;

    /**
     * 创建人/来源
     */
    @NotBlank(message = ErrorMessageConstant.USER_ID_EMPTY)
    private String userId;

}