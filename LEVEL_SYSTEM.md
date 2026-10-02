# Level System — 1000 مرحلة

## البنية
كل مرحلة تُشتق حتمياً من رقمها (seed = id) — لا ملفات بيانات ولا إدخال يدوي:
- LevelCatalog.getLevel(id) → LevelDefinition (id, world, objective, targets, moves/time, prefill, specialChance, reward)

## العوالم (10 × 100)
| العالم | الميكانيكا المقدَّمة | بوابة النجوم |
|---|---|---|
| 1 Green Valley | الأساسيات | 0 |
| 2 Golden Desert | مسح خطوط أكثر | 30 |
| 3 Coral Ocean | عوائق Pre-fill | 70 |
| 4 Frozen Peaks | تحدي الوقت | 120 |
| 5 Ember Volcano | أهداف الكومبو | 170 |
| 6 Deep Forest | عوائق أكثف | 220 |
| 7 Neon City | قطع خاصة 10%+ | 270 |
| 8 Candy Wonderland | وقت أضيق | 320 |
| 9 Cosmic Space | كومبو أعلى | 370 |
| 10 Mystery Realm | مزيج الكل + زعماء | 420 |

## أنواع الأهداف
SCORE (نقاط بميزانية حركات) / LINES (مسح خطوط) / TIME (نقاط قبل انتهاء الوقت) / COMBO (كومبو xN)
كل 100 مستوى = مرحلة زعيم (Boss).

## النجوم
1★ إكمال الهدف، 2★ ≥20% من الميزانية متبقية، 3★ ≥40%.
العملات تُمنع عند تحسّن النجوم فقط: rewardCoins × النجوم/3 + مكافأة يومية 25.

## التحقق
LevelCatalogTest (JUnit) يعمل في كل Build:
توليد الألف + حتمية + قابلية الوصول للأهداف + منحنى الصعوبة + البوابات + الزعماء.
