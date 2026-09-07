package com.amlve.teach;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试专用启动配置类。
 * teach 模块自身没有 @SpringBootApplication（全局启动类在 bootstrap），
 * @WebMvcTest 切片测试向上查找配置时以此类为锚点。
 * 切片模式下仅加载 Web 层组件，不会初始化数据源/Mapper。
 */
@SpringBootApplication
public class TeachTestApplication {
}
