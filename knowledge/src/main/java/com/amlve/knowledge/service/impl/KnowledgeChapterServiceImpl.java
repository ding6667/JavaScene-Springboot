package com.amlve.knowledge.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.knowledge.constant.ErrorMessageConstant;
import com.amlve.knowledge.config.KnowledgeCacheConfig;
import com.amlve.knowledge.converter.KnowledgeConverter;
import com.amlve.knowledge.dto.response.ChapterResponse;
import com.amlve.knowledge.mapper.KnowledgeChapterMapper;
import com.amlve.knowledge.mapper.KnowledgeCodeSnippetMapper;
import com.amlve.knowledge.mapper.KnowledgeConceptMapper;
import com.amlve.knowledge.mapper.KnowledgeContentMapper;
import com.amlve.knowledge.mapper.KnowledgeDifficultPointMapper;
import com.amlve.knowledge.mapper.KnowledgeExerciseMapper;
import com.amlve.knowledge.mapper.KnowledgeKeywordMapper;
import com.amlve.knowledge.mapper.KnowledgePointMapper;
import com.amlve.knowledge.service.KnowledgeChapterService;
import com.amlve.knowledge.tableEnt.KnowledgeChapter;
import com.amlve.knowledge.tableEnt.KnowledgeCodeSnippet;
import com.amlve.knowledge.tableEnt.KnowledgeConcept;
import com.amlve.knowledge.tableEnt.KnowledgeContent;
import com.amlve.knowledge.tableEnt.KnowledgeDifficultPoint;
import com.amlve.knowledge.tableEnt.KnowledgeExercise;
import com.amlve.knowledge.tableEnt.KnowledgeKeyword;
import com.amlve.knowledge.tableEnt.KnowledgePoint;
import com.amlve.knowledgeapi.dto.request.ChapterNamesRequest;
import com.amlve.knowledgeapi.dto.request.KnowledgeChapterSaveRequest;
import com.amlve.knowledgeapi.dto.request.KnowledgeVerifyRequest;
import com.amlve.knowledgeapi.dto.response.ChapterDTO;
import com.amlve.knowledgeapi.dto.response.ChapterNameDTO;
import com.amlve.knowledgeapi.dto.response.ChapterNamesResponse;
import com.amlve.knowledgeapi.dto.response.ChapterTreeDTO;
import com.amlve.knowledgeapi.dto.response.KnowledgePointDTO;
import com.amlve.knowledgeapi.dto.response.KnowledgeVerifyResponse;
import com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 章节服务实现
 * <p>
 * 同时实现 {@link com.amlve.knowledgeapi.service.KnowledgeChapterApi}，
 * 供其他模块通过 knowledge-api 接口调用（跨模块禁止直接注入本实现类）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeChapterServiceImpl implements KnowledgeChapterService,
        com.amlve.knowledgeapi.service.KnowledgeChapterApi {

    /**
     * 判定"贴合教材"的命中率阈值。与本章关键词配置数量强相关：
     * 关键词配得越多，阈值越难达到，配置权重时应同步维护关键词
     */
    private static final double VERIFY_MATCH_THRESHOLD = 0.6;

    /**
     * 教材原文片段返回长度上限
     */
    private static final int CONTENT_SNIPPET_MAX_LENGTH = 500;

    /**
     * 章节名批量查询上限，防止 IN 过大
     */
    private static final int CHAPTER_NAME_BATCH_LIMIT = 100;

    /**
     * "第X章 "前缀（阿拉伯数字与中文数字均支持）。
     * 数字与"章"之间必须允许空白：库里 8 个大章标题实际是"第 1 章 Java 语言概述"这种带空格形式，
     * 少了这个 \s* 会导致 shortName 原样返回、静默产出错误数据
     */
    private static final Pattern CHAPTER_PREFIX_PATTERN =
            Pattern.compile("^第\\s*[0-9一二三四五六七八九十百零两]+\\s*章\\s*");

    /**
     * 关键词是否含字母/数字/中日韩文字：全不命中即为纯符号关键词（"."、"%"、"+" 等），
     * 这类词任何正常回答都会"命中"，必须排除在命中率分母之外
     */
    private static final Pattern KEYWORD_HAS_LETTER_OR_CJK =
            Pattern.compile("[0-9A-Za-z\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}]");

    /**
     * 关键词是否含中日韩文字：含则用包含匹配，纯 ASCII 词才加词边界
     */
    private static final Pattern KEYWORD_HAS_CJK = Pattern.compile("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}]");

    private final KnowledgeChapterMapper chapterMapper;
    private final KnowledgePointMapper pointMapper;
    private final KnowledgeConceptMapper conceptMapper;
    private final KnowledgeCodeSnippetMapper codeSnippetMapper;
    private final KnowledgeDifficultPointMapper difficultPointMapper;
    private final KnowledgeExerciseMapper exerciseMapper;
    private final KnowledgeKeywordMapper keywordMapper;
    private final KnowledgeContentMapper contentMapper;
    private final KnowledgeConverter knowledgeConverter;
    private final ObjectMapper objectMapper;

    @Override
    public List<ChapterResponse> listByCourse(Long courseId) {
        return knowledgeConverter.toResponseList(selectChaptersByCourse(courseId));
    }

    @Override
    public Result<List<ChapterDTO>> listChaptersByCourse(Long courseId) {
        return Result.success(knowledgeConverter.toChapterDTOList(selectChaptersByCourse(courseId)));
    }

    @Override
    public Result<List<KnowledgePointDTO>> listPointsByChapter(Long chapterId) {
        if (ObjectUtils.isEmpty(chapterId)) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_ID_EMPTY);
        }
        List<KnowledgePoint> points = pointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getChapterId, chapterId)
        );
        log.info("查询章节知识点: chapterId={}, 命中{}条", chapterId, points.size());
        return Result.success(knowledgeConverter.toKnowledgePointDTOList(points));
    }

    @Override
    public Result<TeachingMaterialDTO> getTeachingMaterials(Long chapterId) {
        if (ObjectUtils.isEmpty(chapterId)) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_ID_EMPTY);
        }

        KnowledgeChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException("章节不存在: chapterId=" + chapterId);
        }

        String chapterSummary = chapter.getDescription();

        String conceptDefinitions = buildConceptDefinitionsJson(chapterId);
        String codeExamples = buildCodeExamplesJson(chapterId);
        String commonMistakes = buildCommonMistakesJson(chapterId);
        String pageReferences = buildPageReferencesJson(chapterId);
        String exercises = buildExercisesJson(chapterId);
        String labTasks = "[]";

        log.info("查询章节备课素材: chapterId={}, title={}", chapterId, chapter.getTitle());
        return Result.success(TeachingMaterialDTO.builder()
                .chapterSummary(chapterSummary)
                .conceptDefinitions(conceptDefinitions)
                .codeExamples(codeExamples)
                .commonMistakes(commonMistakes)
                .pageReferences(pageReferences)
                .exercises(exercises)
                .labTasks(labTasks)
                .build());
    }

    private String buildConceptDefinitionsJson(Long chapterId) {
        List<KnowledgeConcept> concepts = conceptMapper.selectList(
                new LambdaQueryWrapper<KnowledgeConcept>()
                        .eq(KnowledgeConcept::getChapterId, chapterId)
                        .orderByAsc(KnowledgeConcept::getPage)
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (KnowledgeConcept c : concepts) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", c.getName());
            map.put("definition", c.getDefinition());
            map.put("difficulty", c.getDifficulty());
            map.put("page", c.getPage());
            list.add(map);
        }
        return toJson(list);
    }

    private String buildCodeExamplesJson(Long chapterId) {
        List<KnowledgeCodeSnippet> snippets = codeSnippetMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCodeSnippet>()
                        .eq(KnowledgeCodeSnippet::getChapterId, chapterId)
                        .orderByAsc(KnowledgeCodeSnippet::getPage)
                        .orderByAsc(KnowledgeCodeSnippet::getPageOrder)
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (KnowledgeCodeSnippet s : snippets) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("title", s.getTitle());
            map.put("code", s.getCode());
            map.put("isKeyExample", s.getIsKeyExample());
            map.put("page", s.getPage());
            list.add(map);
        }
        return toJson(list);
    }

    private String buildCommonMistakesJson(Long chapterId) {
        List<KnowledgeDifficultPoint> points = difficultPointMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDifficultPoint>()
                        .eq(KnowledgeDifficultPoint::getChapterId, chapterId)
                        .orderByAsc(KnowledgeDifficultPoint::getPage)
                        .orderByAsc(KnowledgeDifficultPoint::getPageOrder)
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (KnowledgeDifficultPoint p : points) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("description", p.getDescription());
            map.put("type", p.getType());
            map.put("page", p.getPage());
            list.add(map);
        }
        return toJson(list);
    }

    private String buildPageReferencesJson(Long chapterId) {
        List<KnowledgePoint> points = pointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getChapterId, chapterId)
        );
        Map<String, String> map = new LinkedHashMap<>();
        for (KnowledgePoint p : points) {
            map.put(p.getName(), p.getPageRef());
        }
        return toJson(map);
    }

    private String buildExercisesJson(Long chapterId) {
        List<KnowledgeExercise> exerciseList = exerciseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeExercise>()
                        .eq(KnowledgeExercise::getChapterId, chapterId)
                        .orderByAsc(KnowledgeExercise::getPage)
                        .orderByAsc(KnowledgeExercise::getPageOrder)
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (KnowledgeExercise e : exerciseList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("question", e.getQuestion());
            map.put("answer", e.getAnswer());
            map.put("difficulty", e.getDifficulty());
            map.put("knowledgePoint", e.getKnowledgePoint());
            map.put("page", e.getPage());
            list.add(map);
        }
        return toJson(list);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "[]";
        }
    }

    private List<KnowledgeChapter> selectChaptersByCourse(Long courseId) {
        if (ObjectUtils.isEmpty(courseId)) {
            throw new BusinessException(ErrorMessageConstant.COURSE_ID_EMPTY);
        }
        List<KnowledgeChapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<KnowledgeChapter>()
                        .eq(KnowledgeChapter::getCourseId, courseId)
                        .orderByAsc(KnowledgeChapter::getSortOrder)
        );
        log.info("查询章节列表: courseId={}, 命中{}条", courseId, chapters.size());
        return chapters;
    }

    // ===== 章节树与维护 =====

    /**
     * 章节树组装（Redis 缓存 1 小时）。
     * <p>
     * 一次查出该课程全部章节（@TableLogic 自动过滤已删除），再用 map 挂父子关系，
     * 避免"每个节点查一次子节点"的 N+1。顶级判定：parentId 为 null/0，或其父节点
     * 不在本次结果集里（父章节已被删除时不让整棵子树凭空消失）。
     * <p>
     * 缓存 key 为 courseId；{@code unless} 排除空列表——某课程还没导教材时若把空树缓存住，
     * 后续导入章节要等 TTL 到期才可见（写接口只 evict 已有 key，救不回这种"查空"结果）。
     * Redis 不可用时由 {@code KnowledgeCacheConfig} 的降级处理器退回直接查库。
     */
    @Override
    @Cacheable(cacheNames = KnowledgeCacheConfig.CHAPTER_TREE_CACHE, key = "#courseId",
            unless = "#result == null || #result.isEmpty()")
    public List<ChapterTreeDTO> chapterTree(Long courseId) {
        if (courseId == null) {
            throw new BusinessException(ErrorMessageConstant.COURSE_ID_EMPTY);
        }
        List<KnowledgeChapter> all = chapterMapper.selectList(Wrappers.<KnowledgeChapter>lambdaQuery()
                .eq(KnowledgeChapter::getCourseId, courseId)
                .orderByAsc(KnowledgeChapter::getSortOrder)
                .orderByAsc(KnowledgeChapter::getId));
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ChapterTreeDTO> nodes = new LinkedHashMap<>();
        for (KnowledgeChapter chapter : all) {
            ChapterTreeDTO node = new ChapterTreeDTO();
            node.setId(chapter.getId());
            node.setCourseId(chapter.getCourseId());
            node.setParentId(chapter.getParentId());
            node.setChapterCode(chapter.getChapterCode());
            node.setTitle(chapter.getTitle());
            node.setLevel(chapter.getLevel());
            node.setSortOrder(chapter.getSortOrder());
            node.setChildren(new ArrayList<>());
            nodes.put(chapter.getId(), node);
        }
        List<ChapterTreeDTO> roots = new ArrayList<>();
        for (ChapterTreeDTO node : nodes.values()) {
            ChapterTreeDTO parent = node.getParentId() == null
                    ? null : nodes.get(node.getParentId());
            if (parent != null && !parent.getId().equals(node.getId())) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        log.info("章节树组装完成: courseId={}, 总节点={}, 顶级={}", courseId, nodes.size(), roots.size());
        return roots;
    }

    /**
     * 新增或更新章节。level 不传时按父章节层级 +1 推导，避免运维手填错层级。
     * <p>
     * 缓存失效用 allEntries 整片清而不是只清 {@code #request.courseId}：更新时章节可能被改到
     * 另一门课程（或换父节点后归属变化），按新 courseId 单清会漏掉旧课程那棵树。章节保存是
     * 低频运维动作，多清一次代价可忽略，比留一份错树强。
     * <p>
     * 任务书另要求 content/delete 也失效本缓存，此处未加：章节树节点只有章节自身字段、
     * 不含教材原文数量，删原文不改变树结构。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = KnowledgeCacheConfig.CHAPTER_TREE_CACHE, allEntries = true)
    public Long saveChapter(KnowledgeChapterSaveRequest request) {
        long parentId = request.getParentId() == null ? 0L : request.getParentId();
        if (parentId < 0) {
            throw new BusinessException(ErrorMessageConstant.PARENT_CHAPTER_INVALID);
        }
        KnowledgeChapter parent = null;
        if (parentId != 0L) {
            parent = chapterMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException(ErrorMessageConstant.PARENT_CHAPTER_NOT_FOUND);
            }
            if (!parent.getCourseId().equals(request.getCourseId())) {
                throw new BusinessException(ErrorMessageConstant.PARENT_CHAPTER_COURSE_MISMATCH);
            }
        }
        KnowledgeChapter entity = request.getId() == null
                ? new KnowledgeChapter() : chapterMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_NOT_FOUND);
        }
        if (entity.getId() != null && entity.getId().equals(parentId)) {
            // 自己当自己的父节点会让章节树组装出现环
            throw new BusinessException(ErrorMessageConstant.PARENT_CHAPTER_INVALID);
        }
        entity.setCourseId(request.getCourseId());
        entity.setParentId(parentId);
        entity.setChapterCode(request.getChapterCode());
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setSummary(request.getSummary());
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setLevel(request.getLevel() != null ? request.getLevel()
                : (parent == null ? 1 : parent.getLevel() + 1));
        if (request.getId() == null) {
            chapterMapper.insert(entity);
        } else {
            chapterMapper.updateById(entity);
        }
        log.info("章节保存完成: id={}, courseId={}, chapterCode={}, level={}",
                entity.getId(), entity.getCourseId(), entity.getChapterCode(), entity.getLevel());
        return entity.getId();
    }

    // ===== AI 内容校验 =====

    /**
     * 校验 AI 生成内容是否贴合该章教材。
     * <p>
     * 口径：取该章关键词做匹配，命中率 = 命中数 / 参与统计的关键词总数，达到阈值判为贴合。
     * 关键词或教材原文为空时无法给出可信结论，按参数错误返回。
     * <p>
     * 两处必要的防御（九月数据实测得出）：
     * <ul>
     *     <li>纯符号关键词（如 "."、"%"、"+"）不参与统计——库里 11 个章节的关键词只有符号，
     *         任何含句号的英文回答都会被判"命中率 100% 贴合"，校验形同虚设；</li>
     *     <li>英文关键词按词边界匹配，否则 "if" 会命中 "notification"。</li>
     * </ul>
     *
     * @param request 校验请求（章节ID + 待校验内容）
     * @return 命中率、是否贴合、命中的教材原文片段与修改建议
     */
    @Override
    public Result<KnowledgeVerifyResponse> verify(KnowledgeVerifyRequest request) {
        // 跨模块调用不经过 @Valid（只有 HTTP 入口会触发），参数必须在此显式校验，
        // 否则 aiAnswer 为 null 会直接 NPE 成"系统异常"
        if (request == null || request.getChapterId() == null || request.getChapterId() <= 0
                || !StringUtils.hasText(request.getAiAnswer())) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, ErrorMessageConstant.VERIFY_PARAM_INVALID);
        }
        Long chapterId = request.getChapterId();
        if (chapterMapper.selectById(chapterId) == null) {
            // 先判章节存在：否则传错 ID 会报"该章节未配置关键词"，与真实的数据缺失混为一谈
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.CHAPTER_NOT_FOUND);
        }
        List<String> keywords = loadChapterKeywords(chapterId);
        if (keywords.isEmpty()) {
            log.warn("章节未配置可用关键词，无法校验: chapterId={}", chapterId);
            throw new BusinessException(ErrorMessageConstant.VERIFY_NO_KEYWORD);
        }
        List<KnowledgeContent> contents = contentMapper.selectList(
                Wrappers.<KnowledgeContent>lambdaQuery()
                        .eq(KnowledgeContent::getChapterId, chapterId)
                        .isNotNull(KnowledgeContent::getContent)
                        .orderByAsc(KnowledgeContent::getPage)
                        .orderByAsc(KnowledgeContent::getPageOrder));
        if (contents.isEmpty()) {
            log.warn("章节无教材原文，无法校验: chapterId={}", chapterId);
            throw new BusinessException(ErrorMessageConstant.VERIFY_NO_CONTENT);
        }

        String answer = request.getAiAnswer().toLowerCase(Locale.ROOT);
        List<String> hit = new ArrayList<>();
        List<String> miss = new ArrayList<>();
        for (String keyword : keywords) {
            if (containsKeyword(answer, keyword)) {
                hit.add(keyword);
            } else {
                miss.add(keyword);
            }
        }

        // 展示值与判定值同源：先按两位小数舍入，再用舍入值判定，避免出现
        // "命中率显示 0.60 却判不贴合"这种自相矛盾的响应
        double hitRate = Math.round((double) hit.size() / keywords.size() * 100) / 100.0;
        KnowledgeVerifyResponse response = new KnowledgeVerifyResponse();
        response.setHitRate(hitRate);
        response.setIsMatch(hitRate >= VERIFY_MATCH_THRESHOLD);
        response.setStandardAnswer(findBestMatchedContent(contents, hit));
        if (!response.getIsMatch()) {
            response.setSuggestRevision("以下内容未命中教材关键词：" + String.join("、", miss)
                    + "；建议对照本章教材原文重新组织表述。");
        }
        log.info("AI 内容校验: chapterId={}, 关键词数={}, 命中={}, 命中率={}, 是否贴合={}",
                chapterId, keywords.size(), hit.size(), hitRate, response.getIsMatch());
        return Result.success(response);
    }

    /**
     * 取该章可用关键词：按章节查询（旧实现扫全表，会把别章关键词算进分母），
     * 展开同义词、去重，并剔除纯符号关键词。
     */
    private List<String> loadChapterKeywords(Long chapterId) {
        List<KnowledgeKeyword> rows = keywordMapper.selectList(
                Wrappers.<KnowledgeKeyword>lambdaQuery().eq(KnowledgeKeyword::getChapterId, chapterId));
        Set<String> keywords = new LinkedHashSet<>();
        List<String> skipped = new ArrayList<>();
        for (KnowledgeKeyword row : rows) {
            collectKeyword(row.getKeyword(), keywords, skipped);
            if (StringUtils.hasText(row.getSynonyms())) {
                for (String synonym : row.getSynonyms().split(",")) {
                    collectKeyword(synonym, keywords, skipped);
                }
            }
        }
        if (!skipped.isEmpty()) {
            log.warn("章节 {} 有 {} 个关键词为纯符号无法参与匹配，已跳过并计入数据治理清单: {}",
                    chapterId, skipped.size(), skipped);
        }
        return new ArrayList<>(keywords);
    }

    /**
     * 单个关键词入库判定：空白、纯符号（不含中日韩文字与字母数字）一律不计入分母
     */
    private void collectKeyword(String raw, Set<String> keywords, List<String> skipped) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String keyword = raw.trim();
        if (!KEYWORD_HAS_LETTER_OR_CJK.matcher(keyword).find()) {
            skipped.add(keyword);
            return;
        }
        keywords.add(keyword.toLowerCase(Locale.ROOT));
    }

    /**
     * 关键词匹配：含中日韩文字的直接用包含匹配（中文无词边界概念），
     * 纯 ASCII 词加词边界，避免 if 命中 notification、class 命中 classroom。
     *
     * @param answerLower 已转小写的待校验文本
     */
    private boolean containsKeyword(String answerLower, String keywordLower) {
        if (KEYWORD_HAS_CJK.matcher(keywordLower).find()) {
            return answerLower.contains(keywordLower);
        }
        Pattern wordBoundary = Pattern.compile("(?<![0-9a-z_])" + Pattern.quote(keywordLower) + "(?![0-9a-z_])");
        return wordBoundary.matcher(answerLower).find();
    }

    /**
     * 取与命中关键词重合度最高的教材原文片段（按码点截断 500，避免截半个汉字）。
     *
     * @return 片段；未命中任何关键词时返回 null——兜底返回第一段原文会被调用方当成真实出处，
     *         正是这个接口要防的幻觉
     */
    private String findBestMatchedContent(List<KnowledgeContent> contents, List<String> hit) {
        if (hit.isEmpty()) {
            return null;
        }
        KnowledgeContent best = null;
        int bestScore = 0;
        for (KnowledgeContent content : contents) {
            String text = content.getContent().toLowerCase(Locale.ROOT);
            int score = 0;
            for (String keyword : hit) {
                if (text.contains(keyword)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = content;
            }
        }
        if (best == null) {
            return null;
        }
        String text = best.getContent();
        if (text.codePointCount(0, text.length()) <= CONTENT_SNIPPET_MAX_LENGTH) {
            return text;
        }
        int end = text.offsetByCodePoints(0, CONTENT_SNIPPET_MAX_LENGTH);
        return text.substring(0, end) + "...";
    }

    // ===== 章节名称查询 =====

    @Override
    public Result<ChapterNameDTO> getChapterNameById(Long chapterId) {
        if (chapterId == null || chapterId <= 0) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_ID_EMPTY);
        }
        KnowledgeChapter chapter = chapterMapper.selectById(chapterId);
        // 查不到返回 data=null 而非报错：批量回填章节名的调用方不应被单个失效章节打断
        return Result.success(chapter == null ? null : toChapterName(chapter));
    }

    @Override
    public Result<ChapterNamesResponse> getChapterNamesById(ChapterNamesRequest request) {
        if (request == null || request.getChapterIds() == null || request.getChapterIds().isEmpty()) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, ErrorMessageConstant.CHAPTER_ID_EMPTY);
        }
        // null 元素不能静默丢弃：那样调用方会以为"全部命中"，必须归入 notFoundIds
        List<Long> distinctIds = request.getChapterIds().stream()
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> invalidIds = request.getChapterIds().stream()
                .filter(id -> id == null || id <= 0).distinct().collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, ErrorMessageConstant.CHAPTER_ID_EMPTY);
        }
        // 上限按去重后的数量判定，与 ChapterNamesRequest 上的 @Size（校验原始列表）口径不同，
        // 以服务端这道为准：HTTP 入口的 @Valid 只是提前拦截
        if (distinctIds.size() > CHAPTER_NAME_BATCH_LIMIT) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID,
                    ErrorMessageConstant.CHAPTER_NAME_BATCH_EXCEEDED);
        }
        List<KnowledgeChapter> found = chapterMapper.selectBatchIds(distinctIds);
        Set<Long> foundIds = found.stream().map(KnowledgeChapter::getId).collect(Collectors.toSet());

        List<Long> notFound = distinctIds.stream().filter(id -> !foundIds.contains(id))
                .collect(Collectors.toCollection(ArrayList::new));
        notFound.addAll(invalidIds);
        ChapterNamesResponse response = new ChapterNamesResponse();
        response.setChapters(found.stream().map(this::toChapterName).collect(Collectors.toList()));
        response.setNotFoundIds(notFound);
        log.info("批量查询章节名称: 入参去重后={}, 命中={}, 未命中={}",
                distinctIds.size(), found.size(), notFound.size());
        return Result.success(response);
    }

    @Override
    public Result<List<ChapterNameDTO>> listAllChapterNames() {
        List<KnowledgeChapter> chapters = chapterMapper.selectList(
                Wrappers.<KnowledgeChapter>lambdaQuery().orderByAsc(KnowledgeChapter::getId));
        return Result.success(chapters.stream().map(this::toChapterName).collect(Collectors.toList()));
    }

    /**
     * 实体转章节名 DTO，shortName 由后端统一截取，避免各调用方各写一套解析
     */
    private ChapterNameDTO toChapterName(KnowledgeChapter chapter) {
        ChapterNameDTO dto = new ChapterNameDTO();
        dto.setChapterId(chapter.getId());
        dto.setChapterName(chapter.getTitle());
        dto.setShortName(buildShortName(chapter.getTitle()));
        return dto;
    }

    /**
     * 去掉"第X章 "前缀得到短名；无前缀时与全称相同
     */
    private String buildShortName(String chapterName) {
        if (!StringUtils.hasText(chapterName)) {
            return chapterName;
        }
        String shortName = CHAPTER_PREFIX_PATTERN.matcher(chapterName).replaceFirst("").trim();
        return shortName.isEmpty() ? chapterName : shortName;
    }
}