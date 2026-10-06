# Architecture — Star Puzzle (World Puzzle Adventure)

## الطبقات
1. **LevelCatalog / LevelDefinition / LevelProgress** (game/): طبقة المراحل. توليد حتمي خالص بدون libGDX لاختباره رأسياً. التقدم محفوظ في Preferences كسلسلة نجوم 1000 خانة.
2. **Screens** (screens/): MainMenuScreen ← WorldMapScreen ← GameScreen(GAME_MODE_LEVEL) + ResultDialog / ProfileScreen. القوائم Stage+Table واللعب Screen يدوي الرسم.
3. **Game core** (game/): Board/PieceHolder/Piece/Scorer كما في المرجع المفتوح مع إضافات: Board.setCell/isEmpty/clearArea/clearCross، Piece.specialChance.
4. **GameStore**: الإعدادات والعملات والصوت، كلها عبر Preferences.

## أنماط مهمة
- التدرّج بين الشاشات: game.transitionTo() يمر عبر TransitionScreen ويلغي السابقة.
- قوائم الإيقاف: PauseMenuStage (Stage) فوق GameScreen، وشاشة النتيجة Dialog على نفس الـStage.
- الأنماط بدون ملف CSS: LevelCatalog.update/... كل شيء من id المرحلة (بذرة ثابتة = نفس المرحلة على كل جهاز).
- الاقتصاد: عملات المراحل تُمنح فقط عند تحسّن النجوم (لا مزارعة).

## ما هو مؤجل بوعي (Roadmap)
- i18n متعدد اللغات: يحتاج FreeType + تشكيل عربي (reshaping) — البنية الحالية نصوص ثابتة إنجليزية.
- AdMob: بنية جاهزة للإضافة (انظر memory/المحفوظات) عند توفر App ID.
- Leaderboards/Backend: اللعبة Offline بالكامل حالياً.
