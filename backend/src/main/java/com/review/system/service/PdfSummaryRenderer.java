package com.review.system.service;

import com.review.system.entity.ReviewTask;
import com.review.system.service.SummaryService.OpinionVO;
import com.review.system.service.SummaryService.ProjectSummaryVO;
import com.review.system.util.StringUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** 正式汇总 PDF：多页、水印、页眉页脚、电子签章确认区。 */
public final class PdfSummaryRenderer {

    private static final float MARGIN = 50;
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float CONTENT_TOP = PAGE_HEIGHT - MARGIN - 28;
    private static final float CONTENT_BOTTOM = MARGIN + 48;

    private PdfSummaryRenderer() {
    }

    public static byte[] render(ReviewTask task, List<ProjectSummaryVO> summaries, boolean mask) throws Exception {
        String exportedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String docNo = "SUM-" + task.getId() + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        List<TextLine> bodyLines = buildBodyLines(task, summaries, mask, docNo);
        List<String> signatureLines = buildSignatureLines(exportedAt, docNo);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont font = loadFont(document);
            List<List<TextLine>> pages = paginate(bodyLines);
            for (int i = 0; i < pages.size(); i++) {
                document.addPage(new PDPage(PDRectangle.A4));
            }
            int total = pages.size();
            for (int i = 0; i < total; i++) {
                PDPage page = document.getPage(i);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    drawWatermark(cs, font);
                    drawHeader(cs, font, exportedAt, docNo);
                    float y = CONTENT_TOP;
                    for (TextLine tl : pages.get(i)) {
                        y = writeLine(cs, font, tl.size, MARGIN, y, tl.text);
                    }
                    drawFooter(cs, font, i + 1, total, docNo);
                }
            }
            if (!pages.isEmpty()) {
                appendSignature(document, document.getPage(total - 1), font, signatureLines);
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static List<TextLine> buildBodyLines(ReviewTask task, List<ProjectSummaryVO> summaries,
                                                 boolean mask, String docNo) {
        List<TextLine> lines = new ArrayList<>();
        lines.add(new TextLine(16, "评审结果汇总报告"));
        lines.add(new TextLine(10, "任务：" + StringUtil.nullToEmpty(task.getName())));
        lines.add(new TextLine(10, "类型：" + StringUtil.nullToEmpty(task.getReviewType())));
        lines.add(new TextLine(10, "批次：" + StringUtil.nullToEmpty(task.getBatchName())));
        lines.add(new TextLine(10, "文档编号：" + docNo));
        if (mask) {
            lines.add(new TextLine(10, "（领导视图：专家信息已脱敏）"));
        }
        lines.add(new TextLine(10, ""));
        for (ProjectSummaryVO s : summaries) {
            lines.add(new TextLine(12, "项目：" + s.getProjectName()));
            lines.add(new TextLine(10, String.format("申请人:%s  单位:%s  提交:%d/%d",
                    StringUtil.nullToEmpty(s.getApplicant()), StringUtil.nullToEmpty(s.getOrganization()),
                    s.getSubmittedCount(), s.getMinReviewers())));
            lines.add(new TextLine(10, String.format("平均分:%s  最高:%s  最低:%s  结论:%s",
                    score(s.getAvgScore()), score(s.getMaxScore()), score(s.getMinScore()),
                    StringUtil.nullToEmpty(s.getRecommendation()))));
            if (s.getOpinions() != null) {
                for (OpinionVO o : s.getOpinions()) {
                    lines.add(new TextLine(10, "  · " + o.getExpertName() + " | " + score(o.getTotalScore())
                            + " | " + StringUtil.nullToEmpty(o.getConclusion())));
                    if (o.getOpinion() != null && !o.getOpinion().isBlank()) {
                        lines.add(new TextLine(10, "    意见：" + o.getOpinion().replace('\n', ' ')));
                    }
                }
            }
            lines.add(new TextLine(10, ""));
        }
        return lines;
    }

    private static List<String> buildSignatureLines(String exportedAt, String docNo) {
        return List.of(
                "【电子签章确认区】",
                "签发单位：外审专家管理系统",
                "文档编号：" + docNo,
                "签发时间：" + exportedAt,
                "说明：本报告由系统自动生成并含水印防伪标识，具有正式汇总效力。"
        );
    }

    private static List<List<TextLine>> paginate(List<TextLine> lines) {
        List<List<TextLine>> pages = new ArrayList<>();
        List<TextLine> current = new ArrayList<>();
        float y = CONTENT_TOP;
        pages.add(current);
        for (TextLine tl : lines) {
            float need = tl.size + 6;
            if (y - need < CONTENT_BOTTOM && !current.isEmpty()) {
                current = new ArrayList<>();
                pages.add(current);
                y = CONTENT_TOP;
            }
            current.add(tl);
            y -= need;
        }
        return pages;
    }

    private static void appendSignature(PDDocument document, PDPage page, PDFont font,
                                        List<String> signatureLines) throws Exception {
        try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
            float y = CONTENT_BOTTOM + 72;
            cs.setStrokingColor(120f / 255f, 120f / 255f, 120f / 255f);
            cs.moveTo(MARGIN, y + 8);
            cs.lineTo(PAGE_WIDTH - MARGIN, y + 8);
            cs.stroke();
            for (String line : signatureLines) {
                float size = line.startsWith("【") ? 11 : 9;
                y = writeLine(cs, font, size, MARGIN, y, line);
            }
            cs.setStrokingColor(180f / 255f, 50f / 255f, 50f / 255f);
            cs.addRect(PAGE_WIDTH - MARGIN - 100, CONTENT_BOTTOM + 8, 90, 32);
            cs.stroke();
            writeLine(cs, font, 9, PAGE_WIDTH - MARGIN - 88, CONTENT_BOTTOM + 22, "电子签章");
        }
    }

    private static void drawWatermark(PDPageContentStream cs, PDFont font) throws Exception {
        cs.saveGraphicsState();
        cs.setNonStrokingColor(220f / 255f, 220f / 255f, 220f / 255f);
        cs.beginText();
        cs.setFont(font, 42);
        cs.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(35), 80, 380));
        cs.showText("外审专家管理系统 · 正式稿");
        cs.endText();
        cs.restoreGraphicsState();
    }

    private static void drawHeader(PDPageContentStream cs, PDFont font, String exportedAt, String docNo) throws Exception {
        writeLine(cs, font, 8, MARGIN, PAGE_HEIGHT - MARGIN,
                "专家评审系统 · 正式汇总稿  |  " + docNo + "  |  导出：" + exportedAt);
    }

    private static void drawFooter(PDPageContentStream cs, PDFont font, int page, int total, String docNo) throws Exception {
        writeLine(cs, font, 8, MARGIN, MARGIN + 8,
                "第 " + page + " / " + total + " 页  |  " + docNo + "  |  外审专家管理系统");
    }

    private static float writeLine(PDPageContentStream cs, PDFont font, float size,
                                   float x, float y, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(30f / 255f, 30f / 255f, 30f / 255f);
        cs.newLineAtOffset(x, y);
        cs.showText(truncate(text != null ? text : "", 130));
        cs.endText();
        return y - size - 4;
    }

    private static PDFont loadFont(PDDocument doc) throws IOException {
        for (String path : List.of(
                "C:\\Windows\\Fonts\\simhei.ttf",
                "C:\\Windows\\Fonts\\msyh.ttf",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc")) {
            File file = new File(path);
            if (!file.exists()) {
                continue;
            }
            if (path.endsWith(".ttc")) {
                PDFont font = loadFromCollection(doc, file);
                if (font != null) {
                    return font;
                }
            } else {
                return PDType0Font.load(doc, file);
            }
        }
        for (String path : List.of("C:\\Windows\\Fonts\\msyh.ttc", "C:\\Windows\\Fonts\\simsun.ttc")) {
            File file = new File(path);
            if (file.exists()) {
                PDFont font = loadFromCollection(doc, file);
                if (font != null) {
                    return font;
                }
            }
        }
        try (InputStream in = PdfSummaryRenderer.class.getResourceAsStream("/fonts/NotoSansSC-Regular.otf")) {
            if (in != null) {
                return PDType0Font.load(doc, in);
            }
        }
        throw new IOException("未找到可用的中文字体，请安装 simhei.ttf 或微软雅黑字体");
    }

    private static PDFont loadFromCollection(PDDocument doc, File file) throws IOException {
        try (TrueTypeCollection collection = new TrueTypeCollection(file)) {
            for (String name : List.of("Microsoft YaHei", "SimSun", "SimHei", "WenQuanYi Micro Hei", "Noto Sans CJK SC")) {
                TrueTypeFont ttf = collection.getFontByName(name);
                if (ttf != null) {
                    return PDType0Font.load(doc, ttf, true);
                }
            }
            final TrueTypeFont[] first = new TrueTypeFont[1];
            collection.processAllFonts(ttf -> {
                if (first[0] == null) {
                    first[0] = ttf;
                }
            });
            if (first[0] != null) {
                return PDType0Font.load(doc, first[0], true);
            }
        }
        return null;
    }

    private static String score(Object s) {
        return s != null ? s.toString() : "-";
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private record TextLine(float size, String text) {
    }
}
