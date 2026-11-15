package com.amlve.exam.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.request.Result;
import com.amlve.exam.mapper.ExamQuestionSnapshotMapper;
import com.amlve.exam.service.ExamQuestionSnapshotService;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.exam.tableEnt.ExamQuestionSnapshot;
import com.amlve.exam.util.QuestionSnapshotCodec;
import com.amlve.teachapi.dto.response.HomeworkQuestionSnapshotDTO;
import com.amlve.teachapi.service.TeachHomeworkService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 题目快照服务实现。
 * <p>
 * 幂等靠 {@code uk_snapshot_key} 唯一键兜底：先查后插，并发撞键时回查已存在的那一行，
 * 保证同一个题目版本只会有一行快照（学生数再多也不冗余）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamQuestionSnapshotServiceImpl implements ExamQuestionSnapshotService {

    private final ExamQuestionSnapshotMapper snapshotMapper;

    /**
     * teach 作业快照（发布态原文）。用 ObjectProvider 而非直接注入：
     * 单模块启动 / teach 未部署时自动降级为「按题库当前内容建快照」，不阻塞批改。
     */
    private final ObjectProvider<TeachHomeworkService> teachHomeworkServiceProvider;

    @Override
    public ExamQuestionSnapshot resolve(ExamQuestion question, Long teachSnapshotId) {
        if (question == null || question.getId() == null) {
            return null;
        }
        boolean fromHomework = teachSnapshotId != null && teachSnapshotId > 0;
        // 作业来源优先用发布时固化的那份内容建包：题库后来改了题干/用例，也不影响已发布作业的错题
        HomeworkQuestionSnapshotDTO teachSnapshot = fromHomework ? loadTeachSnapshot(teachSnapshotId) : null;
        if (teachSnapshot != null && !Objects.equals(teachSnapshot.getQuestionId(), question.getId())) {
            // 快照指向的题目与提交的题目不一致：宁可退回题库内容，也不把别的题按这一版存下来
            log.warn("作业快照与题目不匹配，回退题库内容: teachSnapshotId={}, snapshotQuestionId={}, questionId={}",
                    teachSnapshotId, teachSnapshot.getQuestionId(), question.getId());
            teachSnapshot = null;
        }
        String fingerprint = teachSnapshot != null
                ? QuestionSnapshotCodec.fingerprintOf(teachSnapshot.getContent(),
                        QuestionSnapshotCodec.optionsText(teachSnapshot.getOptions()), teachSnapshot.getAnswer())
                : QuestionSnapshotCodec.fingerprint(question);
        String key = fromHomework
                ? "P:" + question.getId() + ":" + teachSnapshotId
                : "H:" + question.getId() + ":" + fingerprint;

        ExamQuestionSnapshot exist = findByKey(key);
        if (exist != null) {
            return exist;
        }

        ExamQuestionSnapshot snapshot = new ExamQuestionSnapshot();
        snapshot.setSnapshotKey(key);
        snapshot.setSnapshotType(fromHomework
                ? ExamQuestionSnapshot.TYPE_HOMEWORK : ExamQuestionSnapshot.TYPE_QUESTION_BANK);
        snapshot.setQuestionId(question.getId());
        snapshot.setTeachSnapshotId(fromHomework ? teachSnapshotId : 0L);
        snapshot.setFingerprint(fingerprint);
        if (teachSnapshot != null) {
            snapshot.setShortTitle(teachSnapshot.getShortTitle());
            snapshot.setQuestionType(teachSnapshot.getType());
            snapshot.setScore(teachSnapshot.getScore());
            snapshot.setChapterId(teachSnapshot.getChapterId() != null
                    ? teachSnapshot.getChapterId() : question.getChapterId());
            // 难度配置没有随发布固化，取题库当前值用于展示难度名（不影响判题）
            snapshot.setDifficultyConfigId(question.getDifficultyConfigId());
            snapshot.setSnapshotView(QuestionSnapshotCodec.viewOf(teachSnapshot));
            snapshot.setSnapshotJudge(QuestionSnapshotCodec.judgeOf(teachSnapshot));
        } else {
            snapshot.setShortTitle(question.getShortTitle());
            snapshot.setQuestionType(question.getType() == null ? null : question.getType().getCode());
            snapshot.setScore(question.getScore());
            snapshot.setChapterId(question.getChapterId());
            snapshot.setDifficultyConfigId(question.getDifficultyConfigId());
            snapshot.setSnapshotView(QuestionSnapshotCodec.viewOf(question));
            snapshot.setSnapshotJudge(QuestionSnapshotCodec.judgeOf(question));
        }
        snapshot.setLegacy(0);
        try {
            if (snapshotMapper.insert(snapshot) == 0) {
                log.warn("题目快照写入失败: key={}", key);
                return null;
            }
            return snapshot;
        } catch (DuplicateKeyException e) {
            // 并发下另一个请求先插入了同一版本，回查即可，不算失败
            ExamQuestionSnapshot concurrent = findByKey(key);
            if (concurrent == null) {
                log.warn("题目快照并发写入后回查不到: key={}", key);
            }
            return concurrent;
        }
    }

    @Override
    public ExamQuestionSnapshot getById(Long snapshotId) {
        if (snapshotId == null || snapshotId <= 0) {
            return null;
        }
        return snapshotMapper.selectById(snapshotId);
    }

    @Override
    public Map<Long, ExamQuestionSnapshot> mapByIds(Collection<Long> snapshotIds) {
        if (snapshotIds == null || snapshotIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = snapshotIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, ExamQuestionSnapshot> result = new LinkedHashMap<>();
        for (ExamQuestionSnapshot snapshot : snapshotMapper.selectBatchIds(ids)) {
            result.put(snapshot.getId(), snapshot);
        }
        return result;
    }

    private ExamQuestionSnapshot findByKey(String key) {
        return snapshotMapper.selectOne(Wrappers.<ExamQuestionSnapshot>lambdaQuery()
                .eq(ExamQuestionSnapshot::getSnapshotKey, key));
    }

    /**
     * 读 teach 发布时固化的那份题目内容。取不到（teach 未部署、快照已删、调用异常）返回 null，
     * 由调用方回退到题库当前内容 —— 宁可归档一份近似快照，也不要因为依赖抖动把错题丢掉。
     */
    private HomeworkQuestionSnapshotDTO loadTeachSnapshot(Long teachSnapshotId) {
        TeachHomeworkService service = teachHomeworkServiceProvider.getIfAvailable();
        if (service == null) {
            log.warn("teach 模块不可用，作业快照回退题库内容: teachSnapshotId={}", teachSnapshotId);
            return null;
        }
        try {
            Result<List<HomeworkQuestionSnapshotDTO>> result =
                    service.getQuestionSnapshots(List.of(teachSnapshotId));
            if (result == null || result.getCode() != ErrorCodeConstant.SUCCESS
                    || result.getData() == null || result.getData().isEmpty()) {
                log.warn("作业快照读取为空，回退题库内容: teachSnapshotId={}", teachSnapshotId);
                return null;
            }
            return result.getData().get(0);
        } catch (Exception e) {
            log.warn("作业快照读取异常，回退题库内容: teachSnapshotId={}, error={}", teachSnapshotId, e.getMessage());
            return null;
        }
    }

    @Override
    public List<ExamQuestionSnapshot> listByQuestionId(Long questionId) {
        if (questionId == null) {
            return List.of();
        }
        return snapshotMapper.selectList(Wrappers.<ExamQuestionSnapshot>lambdaQuery()
                .eq(ExamQuestionSnapshot::getQuestionId, questionId)
                .orderByDesc(ExamQuestionSnapshot::getId));
    }
}
