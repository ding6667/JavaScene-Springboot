package com.amlve.agentqa.controller;

import java.util.ArrayList;
import java.util.List;

/**
 * 答疑流式片段的 think / 正文分流器（跨片段状态机）。
 * <p>
 * Dify 工作流的 text_chunk 会把 deepseek 的思考链以
 * "&lt;think&gt; ... &lt;/think&gt;" 形式混在正文前一起下发，且标签本身可能被拆在
 * 相邻两个片段里（实测首帧为 ""，次帧才补齐剩余标签字符）。
 * 本类把原始片段流转换成带事件名的帧序列，供前端分区域渲染：
 * <pre>
 *   think-start  思考内容开始（data 为空）
 *   think        思考文本片段（已去掉标签）
 *   think-end    思考内容结束（data 为空）
 *   message      正文文本片段（已去掉标签）
 * </pre>
 * 状态流转：PRE（尚未确定是否在思考块内）→ THINK → POST（思考块已闭合，余下全是正文）。
 * 模型不输出思考块时，全程停在 PRE，所有内容按 message 发出，行为等价于纯正文流。
 * <p>
 * 只解析 think 一种标记，不解析代码围栏等 Markdown 结构：
 * 排版结构属前端 markdown 渲染器职责，后端解析一旦误判就是永久错版。
 * <p>
 * 非线程安全：每个 SSE 连接各持一个实例。
 */
class AnswerStreamSplitter {

    /** 事件名：思考块开始 */
    static final String EVENT_THINK_START = "think-start";

    /** 事件名：思考文本片段 */
    static final String EVENT_THINK = "think";

    /** 事件名：思考块结束 */
    static final String EVENT_THINK_END = "think-end";

    /** 事件名：正文文本片段 */
    static final String EVENT_MESSAGE = "message";

    /**
     * 思考块开闭标签。用拼接写法，避免字面量被文本处理链当成 HTML 标签吞掉
     */
    private static final String THINK_OPEN = "<" + "think>";
    private static final String THINK_CLOSE = "</" + "think>";

    /**
     * 一帧待发送的 SSE 数据
     *
     * @param event 事件名（前端 addEventListener 的键）
     * @param data  纯文本内容；开始/结束信号为空串
     */
    record Frame(String event, String data) {
    }

    private enum State {
        /** 尚未确定当前文本属于思考块还是正文 */
        PRE,
        /** 位于思考块内部 */
        THINK,
        /** 思考块已闭合，后续全部按正文处理 */
        POST
    }

    private State state = State.PRE;

    /** 待判定缓冲区：标签可能被拆片，末尾最多留 tag.length()-1 个字符等下一帧 */
    private final StringBuilder buffer = new StringBuilder();

    /**
     * 接收一个原始文本片段
     *
     * @param chunk Dify 下发的片段，可为 null / 空
     * @return 本片段可确定的 SSE 帧列表（可能为空，表示还在等下一帧判定）
     */
    List<Frame> accept(String chunk) {
        List<Frame> frames = new ArrayList<>();
        if (chunk == null || chunk.isEmpty()) {
            return frames;
        }
        buffer.append(chunk);
        scan(frames);
        return frames;
    }

    /**
     * 流结束时的收尾：把残留缓冲发出，并对未闭合的思考块补发 think-end，
     * 否则前端"思考中"面板会一直停在加载态。
     */
    List<Frame> finish() {
        List<Frame> frames = new ArrayList<>();
        if (state == State.THINK) {
            add(frames, EVENT_THINK, buffer.toString());
            buffer.setLength(0);
            frames.add(new Frame(EVENT_THINK_END, ""));
        } else if (state == State.PRE) {
            // 残留可能是被截断的标签前缀，按正文发出，不吞内容
            add(frames, EVENT_MESSAGE, buffer.toString());
            buffer.setLength(0);
        }
        return frames;
    }

    /**
     * 反复扫描缓冲，能判定的部分立即成帧，判不定的尾部留在缓冲等下一帧
     */
    private void scan(List<Frame> frames) {
        while (true) {
            switch (state) {
                case PRE -> {
                    int index = buffer.indexOf(THINK_OPEN);
                    if (index >= 0) {
                        add(frames, EVENT_MESSAGE, buffer.substring(0, index));
                        buffer.delete(0, index + THINK_OPEN.length());
                        state = State.THINK;
                        frames.add(new Frame(EVENT_THINK_START, ""));
                        continue;
                    }
                    drainKeepingTail(frames, EVENT_MESSAGE, THINK_OPEN);
                    return;
                }
                case THINK -> {
                    int index = buffer.indexOf(THINK_CLOSE);
                    if (index >= 0) {
                        add(frames, EVENT_THINK, buffer.substring(0, index));
                        buffer.delete(0, index + THINK_CLOSE.length());
                        state = State.POST;
                        frames.add(new Frame(EVENT_THINK_END, ""));
                        continue;
                    }
                    drainKeepingTail(frames, EVENT_THINK, THINK_CLOSE);
                    return;
                }
                default -> {
                    add(frames, EVENT_MESSAGE, buffer.toString());
                    buffer.setLength(0);
                    return;
                }
            }
        }
    }

    /**
     * 发出缓冲中"可以确定"的部分，保留可能是标签前缀的尾部等下一帧
     *
     * @param tag 当前状态下正在等的标签
     */
    private void drainKeepingTail(List<Frame> frames, String event, String tag) {
        int holdLength = partialTagSuffixLength(buffer, tag);
        int flushLength = buffer.length() - holdLength;
        if (flushLength > 0) {
            add(frames, event, buffer.substring(0, flushLength));
            buffer.delete(0, flushLength);
        }
    }

    /**
     * 求 text 的后缀中"恰好是 tag 前缀"的最大长度；无则返回 0。
     * 例如 text="...abc&lt;th"、tag="&lt;think&gt;" 返回 3，这 3 个字符要留着等下一帧。
     */
    private static int partialTagSuffixLength(CharSequence text, String tag) {
        int max = Math.min(tag.length() - 1, text.length());
        for (int length = max; length > 0; length--) {
            String suffix = text.subSequence(text.length() - length, text.length()).toString();
            if (tag.startsWith(suffix)) {
                return length;
            }
        }
        return 0;
    }

    private static void add(List<Frame> frames, String event, String data) {
        if (data != null && !data.isEmpty()) {
            frames.add(new Frame(event, data));
        }
    }
}
