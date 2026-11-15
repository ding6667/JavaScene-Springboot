package com.amlve.dialogue.annotation;

import com.amlve.dialogue.enumeration.TargetScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ToolScope {
    TargetScope value() ;
}
