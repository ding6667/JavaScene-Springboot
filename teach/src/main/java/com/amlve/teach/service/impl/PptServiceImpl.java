package com.amlve.teach.service.impl;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.teach.dto.response.PptDownloadData;
import com.amlve.teach.mapper.TeachPptMapper;
import com.amlve.teach.mapper.TeachPptTemplateMapper;
import com.amlve.teach.mapper.TeachPptThemeMapper;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.service.PptService;
import com.amlve.teach.tableEnt.TeachPpt;
import com.amlve.teach.tableEnt.TeachPptTemplate;
import com.amlve.teach.tableEnt.TeachPptTheme;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.amlve.teach.util.MarkdownToPptxUtil;
import com.amlve.teach.util.PptAvailabilityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 备课成果 PPT 业务实现。
 * <p>
 * 归属校验：teach_ppt 表无 teacher_id 列，归属校验经 teach_prepare_record（teacher_id + pptx_url）实现，
 * 即只能下载本人备课记录关联的 PPT。
 * <p>
 * 下载计数：成功读取文件后按 file_path 匹配原子自增，防并发丢失。
 * <p>
 * 文件补生成（下载接口对前端无感）：历史记录或转换降级会留下「有 pptx_md、无 pptx_url」的状态，
 * 此时不再直接报「还没生成 PPT」，而是用库里的 Markdown 现场重建文件（按记录里的 theme_code 还原
 * 首次生成的主题样式）、回填 pptx_url 并补一条 teach_ppt 记录，本次请求即返回文件。
 * 接口签名、返回结构、错误语义均不变。
 */
@Slf4j
@Service
public class PptServiceImpl implements PptService {

    /** 补生成互斥锁（按备课记录ID）：避免前端重试/双击并发跑出多份文件与多条 teach_ppt 记录 */
    private final ConcurrentHashMap<Long, Object> regenerateLocks = new ConcurrentHashMap<>();

    @Resource
    private TeachPrepareRecordMapper teachPrepareRecordMapper;

    @Resource
    private TeachPptMapper teachPptMapper;

    @Resource
    private TeachPptTemplateMapper teachPptTemplateMapper;

    @Resource
    private TeachPptThemeMapper teachPptThemeMapper;

    @Resource
    private MarkdownToPptxUtil markdownToPptxUtil;

    @Override
    public PptDownloadData downloadPpt(Long recordId) {
        // 1. 经备课记录校验归属（teach_ppt 无 teacher_id 列）
        TeachPrepareRecord record = teachPrepareRecordMapper.selectById(recordId);
        if (record == null || !SecurityUtils.getUserId().equals(record.getTeacherId())) {
            throw new BusinessException(ErrorMessageConstant.PREPARE_RECORD_NOT_FOUND);
        }

        // 2. 取文件路径：缺失（从未生成 / 被清理）时用 pptx_md 补生成，补不出来再按原语义报错
        String filePath = record.getPptxUrl();
        if (!isUsableFile(filePath)) {
            filePath = regeneratePptx(record);
            if (filePath == null) {
                // 与改造前完全一致的前端错误语义：有 URL 但文件没了=过期；压根没 URL=还没生成
                throw new BusinessException(StringUtils.hasText(record.getPptxUrl())
                        ? ErrorMessageConstant.PPT_FILE_EXPIRED
                        : ErrorMessageConstant.PREPARE_PPT_NOT_GENERATED);
            }
        }

        // 3. 读取文件内容
        byte[] content;
        try {
            content = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            log.error("PPT文件读取失败: recordId={}, path={}", recordId, filePath, e);
            throw new BusinessException(ErrorMessageConstant.PPT_FILE_READ_FAILED);
        }

        // 4. 下载计数 +1（setSql 原子自增，避免并发下计数丢失）
        teachPptMapper.update(null, new LambdaUpdateWrapper<TeachPpt>()
                .eq(TeachPpt::getFilePath, filePath)
                .setSql("download_count = download_count + 1"));

        // 5. 文件名优先取 teach_ppt.file_name，查不到用 recordId 兜底
        TeachPpt ppt = teachPptMapper.selectOne(new LambdaQueryWrapper<TeachPpt>()
                .eq(TeachPpt::getFilePath, filePath)
                .last("LIMIT 1"));
        PptDownloadData data = new PptDownloadData();
        data.setFileName(ppt != null && StringUtils.hasText(ppt.getFileName())
                ? ppt.getFileName() : "课件_" + recordId + ".pptx");
        data.setContent(content);
        log.info("PPT下载成功: recordId={}, teacherId={}, bytes={}", recordId, record.getTeacherId(), content.length);
        return data;
    }

    /** 路径非空且文件确实存在（生成文件可能已被清理）；与列表/详情接口用同一判定 */
    private boolean isUsableFile(String filePath) {
        return PptAvailabilityUtil.isFileReady(filePath);
    }

    /**
     * 按备课记录的 pptx_md 补生成 PPT 文件，并回填 pptx_url + 补落 teach_ppt 记录。
     * <p>
     * 没有 pptx_md（PPT 开关当时就没开）时返回 null，由调用方按原错误语义报错。
     * 同一记录加进程内互斥：并发请求只生成一份，后到的直接复用先到者写好的 URL。
     *
     * @return 可用的 pptx 文件路径；无法补生成时返回 null
     */
    private String regeneratePptx(TeachPrepareRecord record) {
        if (!StringUtils.hasText(record.getPptxMd())) {
            return null;
        }
        Long recordId = record.getId();
        Object lock = regenerateLocks.computeIfAbsent(recordId, key -> new Object());
        try {
            synchronized (lock) {
                // 双重检查：并发下另一个请求可能已经把文件补好并回填了
                TeachPrepareRecord latest = teachPrepareRecordMapper.selectById(recordId);
                if (latest != null && isUsableFile(latest.getPptxUrl())) {
                    return latest.getPptxUrl();
                }
                String mdContent = latest != null && StringUtils.hasText(latest.getPptxMd())
                        ? latest.getPptxMd() : record.getPptxMd();
                String themeCode = latest != null && StringUtils.hasText(latest.getThemeCode())
                        ? latest.getThemeCode() : record.getThemeCode();
                String filePath = writePptxAndRecord(record.getChapterId(), mdContent, themeCode);
                if (filePath == null) {
                    return null;
                }
                // 只回填 pptx_url：材料与 status 在生成阶段已是终态，这里不动
                TeachPrepareRecord patch = new TeachPrepareRecord();
                patch.setPptxUrl(filePath);
                teachPrepareRecordMapper.update(patch, new LambdaUpdateWrapper<TeachPrepareRecord>()
                        .eq(TeachPrepareRecord::getId, recordId));
                log.info("PPT补生成完成并已回填: recordId={}, pptxUrl={}", recordId, filePath);
                return filePath;
            }
        } finally {
            regenerateLocks.remove(recordId, lock);
        }
    }

    /**
     * 转换文件 + 落 teach_ppt 记录（顺序与备课生成链路一致：先落文件记录再回写 URL，
     * 否则下载计数按 file_path 匹配时找不到记录）。
     *
     * @param themeCode 生成时落库的主题编码，用于还原与首次生成一致的样式
     * @return 生成的 pptx 路径；转换失败返回 null
     */
    private String writePptxAndRecord(Long chapterId, String mdContent, String themeCode) {
        // 与备课生成保持一致：主题作为 marp2pptx 的同名 yaml 配置传入（md 落库存原始内容）
        // 不能内联 <style>：marp2pptx 不解析 CSS，只会把 CSS 全文渲染到第一页
        String filePath = markdownToPptxUtil.convertContent(mdContent, resolveThemeCss(themeCode));
        if (filePath == null) {
            log.warn("PPT补生成失败（marp2pptx 不可用？）: chapterId={}", chapterId);
            return null;
        }
        TeachPpt ppt = new TeachPpt();
        ppt.setChapterId(chapterId);
        ppt.setFileName(Paths.get(filePath).getFileName().toString());
        ppt.setFilePath(filePath);
        ppt.setMdContent(mdContent);
        ppt.setFileSize(readFileSize(filePath));
        ppt.setDownloadCount(0);
        teachPptMapper.insert(ppt);
        return filePath;
    }

    /**
     * 解析补生成要用的主题 CSS。
     * <p>
     * 优先用备课记录生成时落库的 theme_code，保证补出来的 PPT 与首次生成同一套皮肤；
     * 历史记录该列为空（加列之前生成的）时退回系统默认模板的主题，尽量贴近原效果。
     * 主题表/模板表缺失或查询异常一律降级为无样式，不能因为样式查不到就不给下载。
     */
    private String resolveThemeCss(String recordedThemeCode) {
        String themeCode = StringUtils.hasText(recordedThemeCode)
                ? recordedThemeCode : resolveDefaultThemeCode();
        if (!StringUtils.hasText(themeCode)) {
            return null;
        }
        try {
            TeachPptTheme theme = teachPptThemeMapper.selectOne(
                    new LambdaQueryWrapper<TeachPptTheme>()
                            .eq(TeachPptTheme::getThemeCode, themeCode));
            return theme == null ? null : theme.getCssText();
        } catch (Exception e) {
            log.warn("补生成时主题样式不可用，本次按无主题生成: themeCode={}, cause={}", themeCode, e.getMessage());
            return null;
        }
    }

    /** 系统默认模板的主题编码（与备课未指定模板时的解析口径一致）；查不到返回 null */
    private String resolveDefaultThemeCode() {
        try {
            List<TeachPptTemplate> templates = teachPptTemplateMapper.selectList(
                    new LambdaQueryWrapper<TeachPptTemplate>()
                            .eq(TeachPptTemplate::getIsSystem, 1)
                            .eq(TeachPptTemplate::getIsActive, 1)
                            .orderByAsc(TeachPptTemplate::getSortOrder));
            return templates.isEmpty() ? null : templates.get(0).getThemeCode();
        } catch (Exception e) {
            log.warn("补生成时读取系统默认模板失败，本次按无主题生成: cause={}", e.getMessage());
            return null;
        }
    }

    /** 读取文件大小，失败降级为 null（不影响下载） */
    private Long readFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            log.warn("读取 PPT 文件大小失败: {}", filePath, e);
            return null;
        }
    }
}
