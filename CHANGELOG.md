## [1.3.1] — 2026-10-03 — P0 Power-up Crash Fix & Power-up Rebuild

### BUG (P0 CRITICAL): crash when using Bomb / Lightning / Star
REPRODUCTION:
1. A special power-up piece appears in the bottom piece tray.
2. Player drags it onto the board and releases.
3. App closes (crash), most often when dropping near the left column or bottom row, or on the next frame after the effect.

ROOT CAUSE (two defects, both fixed at the source):
1. Board.putPiece stored the special piece's color index (100..102) inside the board cell. On the next frame Board.draw -> Cell.draw -> Theme.getCellColor(101) -> cells[101] -> ArrayIndexOutOfBoundsException. The app closed instantly.
2. GameScreen.handleSpecialPiece recomputed the target cell from the dragged piece's on-screen center. That position lags behind the finger (lerp) and the formula had an off-by-one, so the target was shifted (-1,-1) — out of bounds at the board edges (immediate crash) and simply wrong everywhere else.

FIX:
1. Special pieces never write their special color index into the board. The board only stores normal colors (-1..7).
2. The true landing cell is recorded inside Board.putPiece (lastPutCellX/Y). Power-ups execute on that exact cell — the same code path that validated the drop.
3. Canonical screen-to-grid conversion (Board.screenToCellX/Y) shared by drop, preview and targeting.
4. Defensive bounds clamps in clearArea / clearCross / setCell / cellCenter / isEmpty — no index can ever go out of range.
5. Save migration: old saves containing stuck special cells are sanitized to empty cells on load.

TEST: 12 new JUnit power-up safety tests — bomb/lightning at all 4 corners and edges, empty and full boards, out-of-range targets, rapid power-up spam, serialization round-trips, old-save sanitization. All 23 core tests green in CI.

STATUS: FIXED

### POWER-UP REBUILD (game feel)
- Targeting preview: while dragging a bomb, the exact 3x3 blast area is highlighted live; lightning highlights the full row + column; star highlights the landing cell. Golden tint = valid drop, soft red = blocked.
- Selected-state glow: soft pulsing halo behind the dragged power-up.
- Screen shake tuned: satisfying but never violent (bomb 0.30s, lightning 0.25s, reduced amplitude).
- Star now converts into a random normal color instead of always the same one.

# Changelog — Star Puzzle

## v1.3.0 — World Puzzle Adventure (أكتوبر 2026)
- نظام مراحل كامل: 1000 مرحلة موزعة على 10 عوالم (Data-driven، توليد حتمي قابل للاختبار)
- خريطة عوالم تفاعلية: عُقد مراحل قابلة للعب + نجوم + أقفال
- 4 أنواع أهداف: نقاط، مسح خطوط، تحدي الوقت، الكومبو
- عوائق Board pre-fill من العالم الثالث، وقطع خاصة أعلى من السابع
- نظام نجوم 3 نجوم مع مكافآت عملات (بدون تكرار للمزارعة)
- تحدي يومي: مرحلة ثابتة لكل يوم + مكافأة إضافية
- إنجازات حقيقية (11 إنجاز) مع إحصائيات حية وشاشة بروفايل
- شاشة نتيجة المرحلة: نجوم + عملات + التالي/إعادة/الخريطة
- اختبارات JUnit تفحص المراحل الألف في كل بناء (GitHub Actions)
- واجهة رئيسية معاد تنظيمها مع تسميات واضحة تحت كل زر
- قائمة الإيقاف تدرك وضع المراحل (خريطة/إعادة نفس المرحلة)

## v1.2.1
- إصلاح الانهيارات (loadPng على المُضاعِفات المفقودة عند كثافات الشاشة غير القياسية)
- إعادة توقيع APK بـ v1+v2+v3 للتوافق الأقصى

## v1.2.0
- كومبو + هزّة شاشة + اهتزاز، قطع خاصة (نجمة/قنبلة/برق)
- مهام يومية، عجلة الحظ، هدية يومية، أصوات وموسيقى

## v1.0.0
- التحول الأول من Klooni إلى لغز النجوم (ثيم/اسم/أيقونة/CI)
