package com.lifestats.screen;

import com.lifestats.data.ItemRegistry;
import com.lifestats.data.PriceManager;
import com.lifestats.data.StatsStore;
import com.lifestats.model.Category;
import com.lifestats.model.ItemDef;
import com.lifestats.model.Job;
import com.lifestats.model.Period;
import com.lifestats.track.ExpTracker;
import com.lifestats.track.InventoryTracker;
import com.lifestats.util.Fmt;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 반투명 레드 테마가 적용된 생활 통계 화면
 */
public class StatsScreen extends Screen {
    // ================= [반투명 레드 테마 팔레트] =================
    // 슬롯 및 패널: 반투명 다크 크림슨 레드 (A: CC = 약 80% 불투명도)
    private static final int C_BG_SLOT      = 0xCC380606; 
    // 테두리: 선명한 루비 레드
    private static final int C_BORDER_MAIN  = 0xFFB71C1C; 
    private static final int C_BORDER_LIGHT = 0xFFFF4444; 
    
    // 탭 버튼 색상
    private static final int C_TAB_ACTIVE   = 0xE6D32F2F; // 선택된 탭: 밝고 선명한 레드
    private static final int C_TAB_INACTIVE = 0xAA2B0404; // 미선택 탭: 어두운 반투명 흑적색
    private static final int C_TAB_BORDER   = 0xFFFF5252; // 선택 탭 하이라이트 테두리
    
    // 텍스트 색상
    private static final int C_TEXT_WHITE   = 0xFFFFFFFF; // 흰색 글씨
    private static final int C_TEXT_GOLD    = 0xFFFFD700; // 수량/수익 황금색
    private static final int C_TEXT_DIM     = 0xFFBFA0A0; // 미확인/비활성 연한 붉은 회색
    private static final int C_CYAN_BAR     = 0xFF00E5FF; // 상단 그래프 아이콘 청록색
    // ==========================================================

    private static Period lastPeriod = Period.DAY;
    private static Job lastJob = Job.MINER;

    private Period period = lastPeriod;
    private Job job = lastJob;
    private double scroll;
    private int maxScroll;

    private record Clickable(int x, int y, int w, int h, Runnable action) {}
    private final List<Clickable> buttons = new ArrayList<>();

    public StatsScreen() {
        super(Text.literal("생활 통계"));
    }

    private int panelW() {
        return Math.min(width - 24, 720);
    }

    private int panelX() {
        return (width - panelW()) / 2;
    }

    @Override
    protected void init() {
        buttons.clear();
        int w = panelW();
        int x0 = panelX();

        // 1. 최상단 "시세 입력" 버튼
        buttons.add(new Clickable(x0 + 180, 10, 60, 16, () -> client.setScreen(new PriceScreen(this, job))));

        // 2. [일간] [주간] [월간] 기간 탭
        int px = x0 + 4;
        int pw = 52;
        for (Period p : Period.values()) {
            Period target = p;
            buttons.add(new Clickable(px, 32, pw, 17, () -> {
                period = target;
                lastPeriod = target;
                scroll = 0;
            }));
            px += pw + 3;
        }

        // 3. [광부] [농부] [어부] [나무꾼] [사냥꾼] 직업 탭
        Job[] tabs = Job.tabs();
        int jx = px + 8;
        int jw = (x0 + w - 4 - jx - (tabs.length - 1) * 3) / tabs.length;
        for (Job j : tabs) {
            Job target = j;
            buttons.add(new Clickable(jx, 32, jw, 17, () -> {
                job = target;
                lastJob = target;
                scroll = 0;
            }));
            jx += jw + 3;
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 인게임 화면이 비치는 은은한 반투명 다크 레드 배경
        ctx.fill(0, 0, width, height, 0x662B0303);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        int w = panelW();
        int x0 = panelX();

        Map<String, Long> mine = StatsStore.totals(period, job.name());
        Map<String, Long> common = StatsStore.totals(period, Job.COMMON.name());

        // 1. 최상단 헤더 바
        drawRedPanelBox(ctx, x0, 8, w, 20);
        drawSignalIcon(ctx, x0 + 8, 12);
        ctx.drawText(textRenderer, "생활 통계", x0 + 26, 14, C_TEXT_WHITE, true);

        drawCustomButton(ctx, x0 + 180, 10, 60, 16, "시세 입력", false, mouseX, mouseY);

        String timeStr = "생직 시간 " + Fmt.duration(StatsStore.seconds(period, job.name()));
        ctx.drawText(textRenderer, timeStr, x0 + 260, 14, C_TEXT_GOLD, true);

        drawRedPanelBox(ctx, x0 + w - 145, 10, 140, 16);
        String incomeVal = Fmt.won(income(mine, common));
        ctx.drawText(textRenderer, "예상 수익  |", x0 + w - 140, 14, C_TEXT_WHITE, true);
        int valW = textRenderer.getWidth(incomeVal);
        ctx.drawText(textRenderer, incomeVal, x0 + w - 10 - valW, 14, C_TEXT_GOLD, true);

        // 2. 탭 외곽 테두리 및 버튼들
        drawRedPanelBox(ctx, x0, 30, w, 21);
        int px = x0 + 4;
        int pw = 52;
        for (Period p : Period.values()) {
            drawCustomButton(ctx, px, 32, pw, 17, p.label, p == period, mouseX, mouseY);
            px += pw + 3;
        }

        Job[] tabs = Job.tabs();
        int jx = px + 8;
        int jw = (x0 + w - 4 - jx - (tabs.length - 1) * 3) / tabs.length;
        for (Job j : tabs) {
            drawCustomButton(ctx, jx, 32, jw, 17, j.label, j == job, mouseX, mouseY);
            jx += jw + 3;
        }

        // 3. 서브 타이틀 바
        drawRedPanelBox(ctx, x0, 53, w, 18);
        String subTitle = job.label + " 통계   |   " + period.label + " 수집 결과 <" + job.label + ">";
        ctx.drawText(textRenderer, subTitle, x0 + 10, 58, C_TEXT_WHITE, true);

        // 4. 본문 스크롤 영역
        int top = 74;
        int bottom = height - 38;
        ctx.enableScissor(x0, top, x0 + w, bottom);

        int y = top - (int) scroll;
        // 원소 구역
        y = renderSection(ctx, x0, w, y, "원소", mine, job, 3, false, Category.ELEMENT);
        // 부산물 구역 (세로 우선 3열 정렬)
        y = renderSection(ctx, x0, w, y, "부산물", mine, job, 3, true,
                Category.BYPRODUCT_RARE, Category.BYPRODUCT_EPIC, Category.CRYSTAL);
        // 유물 구역
        int relicCols = job == Job.MINER ? 7 : 5;
        y = renderSection(ctx, x0, w, y, "유물", mine, job, relicCols, false, Category.RELIC);
        // 공통 구역
        y = renderSection(ctx, x0, w, y, "공통", common, Job.COMMON, 3, false, Category.COMMON);

        ctx.disableScissor();

        int contentHeight = y - (top - (int) scroll);
        maxScroll = Math.max(0, contentHeight - (bottom - top));
        if (scroll > maxScroll) scroll = maxScroll;

        // 5. 하단 경험치 바
        renderExpFooter(ctx, x0, height - 34, w);
    }

    private int renderSection(DrawContext ctx, int x0, int w, int y, String label,
                              Map<String, Long> counts, Job source, int cols, boolean columnFirst, Category... cats) {
        List<ItemDef> list = new ArrayList<>();
        for (ItemDef d : ItemRegistry.items(source)) {
            for (Category c : cats) {
                if (d.category() == c) {
                    list.add(d);
                    break;
                }
            }
        }
        if (list.isEmpty()) return y;

        int labelW = 44;
        int contentW = w - labelW - 3;
        int rowH = 17;
        int rows = Math.max(1, (list.size() + cols - 1) / cols);
        int totalH = Math.max(22, rows * rowH + 6);

        // 좌측 라벨 박스
        drawRedPanelBox(ctx, x0, y, labelW, totalH);
        int lx = x0 + (labelW - textRenderer.getWidth(label)) / 2;
        int ly = y + (totalH - 8) / 2;
        ctx.drawText(textRenderer, label, lx, ly, C_TEXT_WHITE, true);

        // 우측 반투명 레드 아이템 슬롯 컨테이너
        int cx0 = x0 + labelW + 3;
        drawRedPanelBox(ctx, cx0, y, contentW, totalH);

        // 아이템 배치
        int colW = (contentW - 12) / cols;
        for (int i = 0; i < list.size(); i++) {
            ItemDef d = list.get(i);
            int col = columnFirst ? (i / rows) : (i % cols);
            int row = columnFirst ? (i % rows) : (i / cols);

            int ix = cx0 + 6 + col * colW;
            int iy = y + 4 + row * rowH;

            // 아이콘
            ItemStack icon = InventoryTracker.ICONS.get(d.name());
            if (icon != null) {
                ctx.drawItem(icon, ix, iy - 1);
            }

            // 수량 / 미확인
            long n = counts.getOrDefault(d.name(), 0L);
            String valStr = n > 0 ? Fmt.count(n) : (d.category() == Category.RELIC || d.category() == Category.COMMON ? "0개" : "미확인");
            int vw = textRenderer.getWidth(valStr);

            // 이름 및 수량
            int nameMaxW = Math.max(20, colW - 20 - vw - 6);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(d.name(), nameMaxW), ix + 19, iy + 4, C_TEXT_WHITE, false);
            ctx.drawText(textRenderer, valStr, ix + colW - 4 - vw, iy + 4, n > 0 ? C_TEXT_GOLD : C_TEXT_DIM, false);
        }

        return y + totalH + 3;
    }

    private void renderExpFooter(DrawContext ctx, int x0, int y, int w) {
        int labelW = 44;
        int contentW = w - labelW - 3;
        int h = 24;

        drawRedPanelBox(ctx, x0, y, labelW, h);
        int lx = x0 + (labelW - textRenderer.getWidth("경험치")) / 2;
        ctx.drawText(textRenderer, "경험치", lx, y + (h - 8) / 2, C_TEXT_WHITE, true);

        int cx0 = x0 + labelW + 3;
        drawRedPanelBox(ctx, cx0, y, contentW, h);

        if (client != null && client.player != null) {
            ExpTracker.ensureStarted(client);
            String expMain = "시작 Lv." + ExpTracker.startLevel() + " " + Fmt.percent(ExpTracker.startProgress())
                    + "   ➔   현재 Lv." + ExpTracker.currentLevel(client) + " " + Fmt.percent(ExpTracker.currentProgress(client));
            ctx.drawText(textRenderer, expMain, cx0 + 8, y + 4, C_TEXT_GOLD, false);
            ctx.drawText(textRenderer, "레벨 곡선 가정값", cx0 + 8, y + 14, C_TEXT_DIM, false);

            String status = "획득 경험치  +측정중";
            ctx.drawText(textRenderer, status, cx0 + contentW - 10 - textRenderer.getWidth(status), y + 8, C_TEXT_GOLD, false);
        }
    }

    /**
     * 반투명 레드 배경 + 붉은색 테두리 박스 그리기
     */
    private void drawRedPanelBox(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, C_BG_SLOT);
        ctx.fill(x, y, x + w, y + 1, C_BORDER_MAIN);
        ctx.fill(x, y + h - 1, x + w, y + h, C_BORDER_MAIN);
        ctx.fill(x, y, x + 1, y + h, C_BORDER_MAIN);
        ctx.fill(x + w - 1, y, x + w, y + h, C_BORDER_MAIN);
    }

    /**
     * 반투명 레드 탭/버튼
     */
    private void drawCustomButton(DrawContext ctx, int x, int y, int w, int h, String text, boolean active, int mx, int my) {
        boolean hover = (mx >= x && mx < x + w && my >= y && my < y + h);
        int bg = active ? C_TAB_ACTIVE : (hover ? 0xDD5A0E0E : C_TAB_INACTIVE);
        int border = active ? C_TAB_BORDER : (hover ? C_BORDER_LIGHT : C_BORDER_MAIN);

        ctx.fill(x, y, x + w, y + h, bg);
        ctx.fill(x, y, x + w, y + 1, border);
        ctx.fill(x, y + h - 1, x + w, y + h, border);
        ctx.fill(x, y, x + 1, y + h, border);
        ctx.fill(x + w - 1, y, x + w, y + h, border);

        int tx = x + (w - textRenderer.getWidth(text)) / 2;
        int ty = y + (h - 8) / 2;
        ctx.drawText(textRenderer, text, tx, ty, active ? C_TEXT_WHITE : (hover ? C_TEXT_WHITE : C_TEXT_DIM), true);
    }

    private void drawSignalIcon(DrawContext ctx, int x, int y) {
        ctx.fill(x, y + 6, x + 2, y + 10, C_CYAN_BAR);
        ctx.fill(x + 3, y + 3, x + 5, y + 10, C_CYAN_BAR);
        ctx.fill(x + 6, y, x + 8, y + 10, C_CYAN_BAR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Clickable c : buttons) {
                if (mouseX >= c.x && mouseX < c.x + c.w && mouseY >= c.y && mouseY < c.y + c.h) {
                    c.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static long income(Map<String, Long> mine, Map<String, Long> common) {
        double sum = 0;
        for (Map.Entry<String, Long> e : mine.entrySet()) sum += e.getValue() * PriceManager.perPiece(e.getKey());
        for (Map.Entry<String, Long> e : common.entrySet()) sum += e.getValue() * PriceManager.perPiece(e.getKey());
        return Math.round(sum);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 16));
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}