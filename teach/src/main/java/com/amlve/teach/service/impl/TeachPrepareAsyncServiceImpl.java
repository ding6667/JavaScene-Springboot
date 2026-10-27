package com.amlve.teach.service.impl;

import com.amlve.agentprepareapi.dto.request.PrepareRequest;
import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;
import com.amlve.agentprepareapi.dto.response.PrepareResponse;
import com.amlve.agentprepareapi.service.PrepareAgentClient;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.teach.enums.PrepareRecordStatusEnum;
import com.amlve.teach.mapper.TeachPptMapper;
import com.amlve.teach.mapper.TeachPptThemeMapper;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.service.TeachPrepareAsyncService;
import com.amlve.teach.tableEnt.TeachPpt;
import com.amlve.teach.tableEnt.TeachPptTheme;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.amlve.teach.util.MarkdownToPptxUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * 备课材料后台生成实现（运行在 prepareExecutor 虚拟线程上）。
 * <p>
 * 链路顺序：调用智能体拿五类材料 → PPT Markdown 转 pptx 文件 → 落 teach_ppt 文件记录 →
 * 材料与 pptxUrl 连同 status=1 一次性回写备课记录。PPT 任一环节抛异常都不会把记录标成成功。
 * <p>
 * 异常无法向外回传，故统一记录日志并把备课记录置为 status=0 + error_msg，
 * 保证每条受理成功的记录最终都能走到终态。远程调用与文件转换不置于事务内。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachPrepareAsyncServiceImpl implements TeachPrepareAsyncService {

    /** 错误信息落库截断长度 */
    private static final int ERROR_MSG_MAX = 500;

    /** frontmatter 分隔线（独占一行的 ---），用于定位已含 frontmatter 内容的闭合位置 */
    private static final Pattern FRONTMATTER_DELIM = Pattern.compile("(?m)^---\\s*$");

    private final PrepareAgentClient prepareAgentClient;

    private final MarkdownToPptxUtil markdownToPptxUtil;

    private final TeachPrepareRecordMapper teachPrepareRecordMapper;

    private final TeachPptMapper teachPptMapper;

    private final TeachPptThemeMapper teachPptThemeMapper;

    @Async("prepareExecutor")
    @Override
    public void generateAsync(Long recordId, Long chapterId, Long teacherId,
                              PrepareRequest agentRequest, TeachingMaterialDTO material, String themeCode) {
        try {
            doGenerate(recordId, chapterId, teacherId, agentRequest, material, themeCode);
        } catch (Exception e) {
            log.error("备课后台生成失败: recordId={}, chapterId={}, teacherId={}",
                    recordId, chapterId, teacherId, e);
            markFailed(recordId, e.getMessage());
        }
    }

    private void doGenerate(Long recordId, Long chapterId, Long teacherId,
                            PrepareRequest agentRequest, TeachingMaterialDTO material, String themeCode) {
        // 1. 调用备课智能体（一次工作流返回五类材料），失败即中断
        Result<PrepareResponse> agentResultRaw = prepareAgentClient.generate(agentRequest, material);
        if (agentResultRaw == null || agentResultRaw.getCode() != ErrorCodeConstant.SUCCESS) {
            throw new BusinessException("备课智能体调用失败: "
                    + (agentResultRaw == null ? "接口返回为空" : agentResultRaw.getMessage()));
        }
        PrepareResponse agentResult = agentResultRaw.getData();

        // 2. PPT Markdown 注入视觉主题 frontmatter 后转 pptx 文件（转换失败返回 null，其余材料照常回写）
        String pptxUrl = null;
        Long pptxFileSize = null;
        String pptxFileName = null;
        if (agentResult.getPptx() != null && !agentResult.getPptx().isBlank()) {
            pptxUrl = markdownToPptxUtil.convertContent(injectTheme(agentResult.getPptx(), themeCode));
            if (pptxUrl != null) {
                pptxFileName = Paths.get(pptxUrl).getFileName().toString();
                try {
                    pptxFileSize = Files.size(Paths.get(pptxUrl));
                } catch (IOException e) {
                    log.warn("读取 PPT 文件大小失败: {}", pptxUrl, e);
                }
            } else {
                log.warn("PPT 转换失败（marp2pptx 不可用？），本次仅回写其余备课材料: recordId={}, chapterId={}",
                        recordId, chapterId);
            }
        }

        // 3. PPT 文件记录落库（仅生成了文件才落；md_content 存 Dify 返回的原始内容，不含主题 frontmatter）
        //    下载接口按 file_path 匹配这张表取文件名并自增下载数，所以必须在状态翻转之前落成功，
        //    否则记录已是"成功"、前端点下载却找不到文件记录
        if (pptxUrl != null) {
            TeachPpt ppt = new TeachPpt();
            ppt.setChapterId(chapterId);
            ppt.setFileName(pptxFileName);
            ppt.setFilePath(pptxUrl);
            ppt.setMdContent(agentResult.getPptx());
            ppt.setFileSize(pptxFileSize);
            ppt.setDownloadCount(0);
            teachPptMapper.insert(ppt);
        }

        // 4. 材料、pptxUrl 与 status=1 在同一条 UPDATE 里落地：
        //    拆成两次写会留下"材料已有但仍在生成中"的半态，前端轮询到非2又取不到URL
        TeachPrepareRecord update = new TeachPrepareRecord();
        update.setId(recordId);
        update.setLessonPlan(agentResult.getLessonPlan());
        update.setKeyPoints(agentResult.getKeyPoints());
        update.setHomework(agentResult.getHomework());
        update.setLab(agentResult.getLab());
        update.setPptxMd(agentResult.getPptx());
        update.setPptxUrl(pptxUrl);
        update.setStatus(PrepareRecordStatusEnum.SUCCESS.getCode());
        teachPrepareRecordMapper.updateById(update);

        log.info("备课后台生成成功: recordId={}, chapterId={}, teacherId={}, pptxUrl={}",
                recordId, chapterId, teacherId, pptxUrl);
    }

    /** 备课记录置为失败终态（status=0，错误信息截断），回写自身失败只记日志 */
    private void markFailed(Long recordId, String errorMsg) {
        try {
            TeachPrepareRecord update = new TeachPrepareRecord();
            update.setId(recordId);
            update.setStatus(PrepareRecordStatusEnum.FAILED.getCode());
            update.setErrorMsg(errorMsg == null ? null
                    : errorMsg.substring(0, Math.min(errorMsg.length(), ERROR_MSG_MAX)));
            teachPrepareRecordMapper.updateById(update);
        } catch (Exception ex) {
            log.error("备课失败状态回写异常: recordId={}", recordId, ex);
        }
    }

    /**
     * PPT Markdown 注入 Marp 基础 frontmatter 与主题样式。
     * <p>
     * marp2pptx 不支持向 Marp CLI 透传 --theme-set，自定义 CSS 主题以 {@code <style>} 块
     * 内联进 Markdown（Marp 会将其应用到每页并覆盖默认外观）。themeCode 无对应 CSS 时
     * 仅注入基础 frontmatter（marp/paginate/16:9），走 Marp 默认外观。
     * <ul>
     *     <li>内容无 frontmatter：头部拼接 frontmatter + style 块；</li>
     *     <li>内容已含 frontmatter（如 Dify 输出自带）：style 块插入到 frontmatter 闭合线之后，不重复注入基础配置；</li>
     *     <li>无主题样式可注入：原样返回。</li>
     * </ul>
     */
    private String injectTheme(String contentMd, String themeCode) {
        String css = getThemeStylesheet(themeCode);
        if (css == null) {
            return contentMd;
        }
        String styleBlock = "<style>\n" + css + "\n</style>\n\n";
        if (!contentMd.startsWith("---")) {
            // 无 frontmatter：注入基础配置 + 主题样式
            return "---\nmarp: true\npaginate: true\nsize: 16:9\n---\n\n" + styleBlock + contentMd;
        }
        // 已含 frontmatter：定位第 2 条 --- 分隔线（闭合线），样式插在其后；
        // 找不到闭合线时放弃注入，避免破坏原文结构
        java.util.regex.Matcher matcher = FRONTMATTER_DELIM.matcher(contentMd);
        int closeEnd = -1;
        for (int i = 0; i < 2 && matcher.find(); i++) {
            closeEnd = matcher.end();
        }
        if (closeEnd > 0) {
            return contentMd.substring(0, closeEnd) + "\n\n" + styleBlock + contentMd.substring(closeEnd);
        }
        return contentMd;
    }

    /**
     * 读取主题样式表内容。
     *
     * @param themeCode 模板主题码（如 java-default）
     * @return CSS 文本；themeCode 为空、格式非法、无对应文件或读取失败时返回 null（调用方降级为不注入样式）
     */
    public String getThemeStylesheet(String themeCode) {
        if (themeCode == null || themeCode.isBlank()) {
            return null;
        }
        TeachPptTheme theme;
        try {
            theme = teachPptThemeMapper.selectOne(
                    new LambdaQueryWrapper<TeachPptTheme>()
                            .eq(TeachPptTheme::getThemeCode, themeCode));
        } catch (Exception e) {
            // 主题表缺失或查询异常时降级为无主题：备课材料已生成，不能因样式查不到就整单失败
            log.warn("主题样式不可用，本次按无主题生成: themeCode={}, cause={}", themeCode, e.getMessage());
            return null;
        }

        return theme == null ? null : theme.getCssText();
    }
}
