# SMRITI Fit: JVM (AWT) text widths vs Android's text stack

Ratio = AWT width / Android Paint width for the same string and bold font (1.00 = identical). Strings and their words, both languages.

| Language | n | median | p5 | p95 | min | max |
|---|---|---|---|---|---|---|
| TAMIL | 722 | 0.999 | 0.997 | 1.002 | 0.991 | 1.088 |
| ENGLISH | 702 | 1.000 | 0.996 | 1.009 | 0.992 | 1.098 |

The table of Android widths is written to `fit/src/main/resources/app/smriti/fit/android_widths.tsv`.
