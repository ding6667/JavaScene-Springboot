package com.amlve.teach.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownToPptxUtilTest {

    @TempDir
    Path tempDir;

    private MarkdownToPptxUtil util;

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name").toLowerCase().contains("win");

    @BeforeEach
    void setUp() {
        util = new MarkdownToPptxUtil();
        ReflectionTestUtils.setField(util, "commonFileDir", tempDir.resolve("commonFile").toString());
        ReflectionTestUtils.setField(util, "timeoutSeconds", 5L);
    }

    @Test
    void convertContent_empty_returnsNull() {
        assertNull(util.convertContent(null));
        assertNull(util.convertContent("   "));
    }

    @Test
    void convertContent_success() throws Exception {
        // 假命令：解析 -o 参数，写入一个假 pptx，退出码 0
        String body = IS_WINDOWS
                ? "@echo off\r\n"
                + "set OUT=\r\n"
                + ":loop\r\n"
                + "if \"%~1\"==\"\" goto end\r\n"
                + "if \"%~1\"==\"-o\" ( set OUT=%~2 & shift & shift & goto loop )\r\n"
                + "shift\r\n"
                + "goto loop\r\n"
                + ":end\r\n"
                + "echo fake-pptx> \"%OUT%\"\r\n"
                + "exit /b 0\r\n"
                : "#!/bin/bash\n"
                + "OUT=\"\"\n"
                + "while [ $# -gt 0 ]; do\n"
                + "  case \"$1\" in\n"
                + "    -o) OUT=\"$2\"; shift 2;;\n"
                + "    *) shift;;\n"
                + "  esac\n"
                + "done\n"
                + "echo fake-pptx > \"$OUT\"\n"
                + "exit 0\n";

        Path script = createScript("fake-marp-success", body);
        ReflectionTestUtils.setField(util, "marpCommand", script.toString());

        String result = util.convertContent("# slide1\n---\n# slide2");

        assertNotNull(result);
        Path pptx = Paths.get(result);
        assertTrue(Files.exists(pptx));
        assertTrue(Files.size(pptx) > 0);
    }

    @Test
    void convertContent_commandFail_returnsNull() throws Exception {
        String body = IS_WINDOWS
                ? "@echo off\r\nexit /b 1\r\n"
                : "#!/bin/bash\nexit 1\n";

        Path script = createScript("fake-marp-fail", body);
        ReflectionTestUtils.setField(util, "marpCommand", script.toString());

        assertNull(util.convertContent("# slide1"));
    }

    @Test
    void convertContent_timeout_returnsNull() throws Exception {
        String body = IS_WINDOWS
                ? "@echo off\r\nping -n 11 127.0.0.1 > nul\r\nexit /b 0\r\n"   // 约 10 秒
                : "#!/bin/bash\nsleep 10\nexit 0\n";

        Path script = createScript("fake-marp-timeout", body);
        ReflectionTestUtils.setField(util, "marpCommand", script.toString());
        ReflectionTestUtils.setField(util, "timeoutSeconds", 1L);

        long start = System.currentTimeMillis();
        assertNull(util.convertContent("# slide1"));
        long cost = System.currentTimeMillis() - start;
        // 确认超时生效，而不是卡死
        assertTrue(cost < 8000, "应被 1 秒超时强杀，实际耗时 " + cost + "ms");
    }

    private Path createScript(String name, String content) throws IOException {
        Path script;
        if (IS_WINDOWS) {
            script = tempDir.resolve(name + ".cmd");
            // cmd 脚本写 GBK 更保险，但内容是纯 ASCII，UTF-8 也能跑
            Files.writeString(script, content, StandardCharsets.UTF_8);
        } else {
            script = tempDir.resolve(name + ".sh");
            Files.writeString(script, content, StandardCharsets.UTF_8);
            script.toFile().setExecutable(true);
        }
        return script;
    }
}