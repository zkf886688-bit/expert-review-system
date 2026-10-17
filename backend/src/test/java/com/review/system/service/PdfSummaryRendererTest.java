package com.review.system.service;

import com.review.system.entity.ReviewTask;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfSummaryRendererTest {

    @Test
    void renderChinesePdf() throws Exception {
        ReviewTask task = new ReviewTask();
        task.setId(1L);
        task.setName("【演示】结题验收汇总样例");
        task.setReviewType("ACCEPTANCE");
        task.setBatchName("2026-Q1");

        byte[] pdf = PdfSummaryRenderer.render(task, Collections.emptyList(), false);
        assertTrue(pdf.length > 1000, "PDF should contain content");
        assertTrue(new String(pdf, 0, Math.min(8, pdf.length)).startsWith("%PDF"), "Should be valid PDF header");
    }
}
