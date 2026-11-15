package com.amlve.dialogue.dto;

import com.amlve.dialogue.enumeration.Command;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = false)
public class CommonJsonDTO {
    private String answer;
    private Command command;
}
