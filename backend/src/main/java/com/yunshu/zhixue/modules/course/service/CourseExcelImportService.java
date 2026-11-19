package com.yunshu.zhixue.modules.course.service;

import cn.idev.excel.FastExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.course.dto.CourseImportConfirmResponse;
import com.yunshu.zhixue.modules.course.dto.CourseImportData;
import com.yunshu.zhixue.modules.course.dto.CourseImportPreviewResponse;
import com.yunshu.zhixue.modules.course.dto.CourseImportRowResponse;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.subject.dto.SubjectBrief;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import com.yunshu.zhixue.security.SecurityUtils;
import com.yunshu.zhixue.support.audit.OperationLogRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 课程 Excel 两段式导入（A-C06 / A-C07）。
 *
 * <p>Excel 列：课程名称 / 所属学科 / 来源 / 时长（分钟） / BV号 / 封面 / 播放链接。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseExcelImportService {

    private static final String PREVIEW_KEY_PREFIX = "content:import:course:";
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);
    private static final int MAX_ROWS = 1000;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int SOURCE_BILIBILI = 1;
    private static final int SOURCE_OTHER = 2;

    private static final Pattern BVID_PATTERN = Pattern.compile("^BV[0-9A-Za-z]{8,18}$");
    private static final Pattern HTTP_URL = Pattern.compile("^https?://\\S+$");
    private static final Pattern SUBJECT_SPLITTER = Pattern.compile("[、,，;；]");

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CourseMapper courseMapper;
    private final CourseService courseService;
    private final SubjectService subjectService;
    private final OperationLogRecorder operationLogRecorder;

    public CourseImportPreviewResponse preview(MultipartFile file) {
        validateFile(file);
        List<Map<Integer, String>> rows = readRows(file);
        if (rows.size() < 2) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED, "Excel 中没有可导入的数据");
        }
        Map<String, Integer> headers = resolveHeaders(rows.get(0));
        List<Map<Integer, String>> dataRows = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            if (!isEmptyRow(rows.get(index))) {
                dataRows.add(rows.get(index));
            }
        }
        if (dataRows.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED, "Excel 中没有可导入的数据");
        }
        if (dataRows.size() > MAX_ROWS) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED,
                    "单次最多导入 " + MAX_ROWS + " 门课程");
        }

        Map<String, SubjectBrief> subjectsByName = new HashMap<>();
        for (SubjectBrief subject : subjectService.options()) {
            subjectsByName.put(subject.name(), subject);
        }
        Set<String> existingBvids = existingBvids(dataRows, headers);
        Set<String> fileBvids = new HashSet<>();

        List<CourseImportRowResponse> previewRows = new ArrayList<>();
        List<CourseImportData> importRows = new ArrayList<>();
        int validCount = 0;
        int warningCount = 0;
        int invalidCount = 0;

        for (int index = 0; index < dataRows.size(); index++) {
            int rowNumber = index + 2;
            Map<Integer, String> row = dataRows.get(index);
            String name = cell(row, headers.get("name"));
            String subjectText = cell(row, headers.get("subject"));
            String sourceText = cell(row, headers.get("source"));
            String durationText = cell(row, headers.get("duration"));
            String bvidText = cell(row, headers.get("bvid"));
            String coverText = cell(row, headers.get("cover"));
            String linkText = cell(row, headers.get("link"));

            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            if (!StringUtils.hasText(name)) {
                errors.add("课程名称不能为空");
            } else if (name.length() > 100) {
                errors.add("课程名称不能超过 100 个字符");
            }

            Integer durationMinutes = parseDuration(durationText);
            if (durationMinutes == null || durationMinutes <= 0) {
                errors.add("时长需为大于 0 的整数分钟，或「小时:分钟」格式");
            }

            SubjectParse subjectParse = parseSubjects(subjectText, subjectsByName);

            int source;
            String normalizedSource = sourceText.replace(" ", "").toLowerCase(Locale.ROOT);
            if (normalizedSource.isEmpty()) {
                source = SOURCE_OTHER;
                warnings.add("未填写来源，按「其他」处理");
            } else if (normalizedSource.equals("b站")
                    || normalizedSource.equals("bilibili")
                    || normalizedSource.equals("b")) {
                source = SOURCE_BILIBILI;
            } else if (normalizedSource.equals("其他")) {
                source = SOURCE_OTHER;
            } else {
                source = SOURCE_OTHER;
                warnings.add("来源「" + sourceText + "」无法识别，按「其他」处理");
            }

            String bvid = bvidText;
            if (StringUtils.hasText(bvid)) {
                String bvidKey = bvid.toLowerCase(Locale.ROOT);
                if (!BVID_PATTERN.matcher(bvid).matches()) {
                    errors.add("BV 号格式不正确");
                } else if (source == SOURCE_OTHER) {
                    errors.add("非 B 站课程不能填写 BV 号");
                } else if (existingBvids.contains(bvidKey)) {
                    errors.add("BV 号已存在：" + bvid);
                } else if (!fileBvids.add(bvidKey)) {
                    errors.add("文件内 BV 号重复：" + bvid);
                }
            } else {
                bvid = "";
                if (source == SOURCE_BILIBILI) {
                    warnings.add("未填写 BV 号，可导入后补充");
                }
            }

            String link = linkText;
            if (!StringUtils.hasText(link)) {
                link = "";
                warnings.add("未填写播放链接，可导入后补充");
            } else if (!HTTP_URL.matcher(link).matches()) {
                errors.add("播放链接需以 http(s):// 开头");
            }

            String cover = coverText;
            if (StringUtils.hasText(cover)
                    && !HTTP_URL.matcher(cover).matches()
                    && !cover.startsWith("/files/")) {
                warnings.add("封面 URL 无效，已置空");
                cover = "";
            }

            warnings.addAll(subjectParse.warnings());
            String status;
            if (!errors.isEmpty()) {
                status = "invalid";
                invalidCount++;
            } else if (!warnings.isEmpty()) {
                status = "warning";
                warningCount++;
            } else {
                status = "valid";
                validCount++;
            }

            previewRows.add(new CourseImportRowResponse(rowNumber,
                    StringUtils.hasText(name) ? name : "(未命名)",
                    subjectText, sourceText,
                    durationMinutes, bvid, cover, link, status,
                    errors.isEmpty() ? warnings : errors));
            if (errors.isEmpty()) {
                importRows.add(new CourseImportData(
                        name, durationMinutes,
                        subjectParse.primarySubjectId(), subjectParse.extraSubjectIds(),
                        source,
                        StringUtils.hasText(link) ? link : null,
                        StringUtils.hasText(cover) ? cover : null,
                        StringUtils.hasText(bvid) ? bvid : null));
            }
        }

        String token = null;
        if (!importRows.isEmpty()) {
            token = UUID.randomUUID().toString().replace("-", "");
            ImportSession session = new ImportSession(SecurityUtils.requireUserId(), importRows);
            try {
                stringRedisTemplate.opsForValue().set(PREVIEW_KEY_PREFIX + token,
                        objectMapper.writeValueAsString(session), PREVIEW_TTL);
            } catch (Exception ex) {
                log.error("课程导入预览缓存失败", ex);
                throw new BizException(ErrorCode.INTERNAL_ERROR, "导入预览保存失败，请重试");
            }
        }
        String filename = file.getOriginalFilename() == null
                ? "课程导入" : file.getOriginalFilename();
        return new CourseImportPreviewResponse(token, filename,
                previewRows.size(), validCount, warningCount, invalidCount, previewRows);
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseImportConfirmResponse confirm(String token) {
        String key = PREVIEW_KEY_PREFIX + token;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            throw new BizException(ErrorCode.NOT_FOUND, "导入预览已失效，请重新上传文件");
        }
        ImportSession session;
        try {
            session = objectMapper.readValue(json, ImportSession.class);
        } catch (Exception ex) {
            stringRedisTemplate.delete(key);
            throw new BizException(ErrorCode.NOT_FOUND, "导入预览已失效，请重新上传文件");
        }
        if (!SecurityUtils.requireUserId().equals(session.adminId())
                || session.rows() == null || session.rows().isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "导入预览已失效，请重新上传文件");
        }

        Set<Long> subjectIds = new LinkedHashSet<>();
        Set<String> bvids = new LinkedHashSet<>();
        for (CourseImportData data : session.rows()) {
            if (data.primarySubjectId() != null) {
                subjectIds.add(data.primarySubjectId());
            }
            subjectIds.addAll(data.extraSubjectIds());
            if (data.bvid() != null) {
                bvids.add(data.bvid());
            }
        }
        if (!subjectIds.isEmpty()
                && subjectService.briefsByIds(subjectIds).size() != subjectIds.size()) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED,
                    "学科信息已变更，请重新上传预览");
        }
        if (!bvids.isEmpty()) {
            Set<String> existing = new HashSet<>(courseMapper.selectExistingBvids(bvids));
            if (!existing.isEmpty()) {
                throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED,
                        "部分 BV 号已存在，请重新上传预览");
            }
        }
        for (CourseImportData data : session.rows()) {
            courseService.createFromImport(data);
        }
        stringRedisTemplate.delete(key);
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_CREATE, "B站课程库",
                "批量导入 " + session.rows().size() + " 门课程");
        return new CourseImportConfirmResponse(session.rows().size());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_INVALID, "请选择要导入的 Excel 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ErrorCode.FILE_INVALID, "Excel 文件不能超过 10MB");
        }
        String filename = file.getOriginalFilename();
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw new BizException(ErrorCode.FILE_INVALID, "仅支持 .xlsx / .xls 文件");
        }
    }

    private List<Map<Integer, String>> readRows(MultipartFile file) {
        try (InputStream inputStream = new ByteArrayInputStream(file.getBytes())) {
            return FastExcel.read(inputStream)
                    .sheet(0)
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.FILE_INVALID, "Excel 文件读取失败", ex);
        } catch (Exception ex) {
            log.warn("课程 Excel 解析失败", ex);
            throw new BizException(ErrorCode.FILE_INVALID, "Excel 文件解析失败，请确认文件格式");
        }
    }

    private Map<String, Integer> resolveHeaders(Map<Integer, String> headerRow) {
        Map<String, Integer> headers = new HashMap<>();
        headerRow.forEach((index, value) -> {
            String header = normalizeHeader(value);
            if (header.isEmpty()) {
                return;
            }
            if (header.equals("课程名称") || header.equals("课程名")) {
                headers.putIfAbsent("name", index);
            } else if (header.equals("所属学科") || header.equals("学科")) {
                headers.putIfAbsent("subject", index);
            } else if (header.equals("来源")) {
                headers.putIfAbsent("source", index);
            } else if (header.startsWith("时长")) {
                headers.putIfAbsent("duration", index);
            } else if (header.contains("bv")) {
                headers.putIfAbsent("bvid", index);
            } else if (header.contains("封面")) {
                headers.putIfAbsent("cover", index);
            } else if (header.contains("播放链接") || header.contains("课程链接")
                    || header.equals("链接")) {
                headers.putIfAbsent("link", index);
            }
        });
        List<String> missing = new ArrayList<>();
        if (!headers.containsKey("name")) missing.add("课程名称");
        if (!headers.containsKey("subject")) missing.add("所属学科");
        if (!headers.containsKey("source")) missing.add("来源");
        if (!headers.containsKey("duration")) missing.add("时长（分钟）");
        if (!missing.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED,
                    "Excel 表头缺少：" + String.join("、", missing));
        }
        return headers;
    }

    private SubjectParse parseSubjects(String raw, Map<String, SubjectBrief> subjectsByName) {
        List<String> warnings = new ArrayList<>();
        List<Long> matched = new ArrayList<>();
        if (!StringUtils.hasText(raw)) {
            warnings.add("未填写所属学科，按「其他学科」处理");
            return new SubjectParse(null, List.of(), warnings);
        }
        boolean hasOther = false;
        for (String part : SUBJECT_SPLITTER.split(raw)) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            if (name.startsWith("其他学科") || name.equals("其他")) {
                hasOther = true;
                continue;
            }
            SubjectBrief subject = subjectsByName.get(name);
            if (subject == null) {
                warnings.add("学科「" + name + "」不存在，已忽略");
            } else if (!matched.contains(subject.id())) {
                matched.add(subject.id());
            }
        }
        if (matched.isEmpty()) {
            if (!hasOther) {
                warnings.add("未匹配到有效学科，按「其他学科」处理");
            }
            return new SubjectParse(null, List.of(), warnings);
        }
        if (hasOther) {
            warnings.add("「其他学科」与具体学科同时填写，已忽略「其他学科」");
        }
        return new SubjectParse(matched.get(0), matched.subList(1, matched.size()), warnings);
    }

    private Integer parseDuration(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.replace(",", "").trim();
        try {
            if (value.matches("^\\d+(\\.0+)?$")) {
                return new BigDecimal(value).intValueExact();
            }
            if (value.matches("^\\d{1,3}:[0-5]?\\d$")) {
                String[] parts = value.split(":");
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            }
        } catch (ArithmeticException | NumberFormatException ex) {
            return null;
        }
        return null;
    }

    private Set<String> existingBvids(List<Map<Integer, String>> rows,
                                      Map<String, Integer> headers) {
        Integer index = headers.get("bvid");
        if (index == null) {
            return Set.of();
        }
        Set<String> candidates = new LinkedHashSet<>();
        for (Map<Integer, String> row : rows) {
            String value = cell(row, index);
            if (StringUtils.hasText(value) && BVID_PATTERN.matcher(value).matches()) {
                candidates.add(value);
            }
        }
        if (candidates.isEmpty()) {
            return Set.of();
        }
        return courseMapper.selectExistingBvids(candidates).stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private String cell(Map<Integer, String> row, Integer index) {
        if (row == null || index == null) {
            return "";
        }
        String value = row.get(index);
        return value == null ? "" : value.trim();
    }

    private boolean isEmptyRow(Map<Integer, String> row) {
        if (row == null) {
            return true;
        }
        return row.values().stream().allMatch(value -> !StringUtils.hasText(value));
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(" ", "")
                .replace("（", "(")
                .replace("）", ")")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /**
     * 预览缓存：仅保存当前管理员与校验通过的行。
     */
    public record ImportSession(Long adminId, List<CourseImportData> rows) {
    }

    private record SubjectParse(Long primarySubjectId,
                                List<Long> extraSubjectIds,
                                List<String> warnings) {
    }
}
