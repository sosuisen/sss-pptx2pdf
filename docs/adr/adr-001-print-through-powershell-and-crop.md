# ADR 001: PowerShell 経由の印刷と PDFBox による切り抜き

## Status

Accepted

## Context

PowerPoint のデッキを、スライド寸法どおりで、かつ半角スペースが文字として残る PDF にしたい。

- PowerPoint の `ExportAsFixedFormat`（PDF として保存）は、run の境界にあるスペースを文字として書かない。PDF を実際に調べたところ、`git add` は `(gi)6(t)` と `(ad)6(d)` の 2 つの文字列オブジェクトになり、間に空白文字が無かった。
- Microsoft Print to PDF（印刷経路）はスペースを文字として書く。しかし V4 型のクラスドライバーで、「プリント サーバーのプロパティ」で登録した用紙を列挙しない。`.NET` の `PrinterSettings.PaperSizes` で確認した 54 種類はすべて Windows 組み込みの用紙だった。
- PostScript 経由の PDF プリンター（CutePDF など）は任意の用紙を使えるが、PostScript に半透明が無いため表紙の半透明が崩れる。

PowerPoint を動かす方法として、Java から COM を直接呼ぶ（JNA、または Java 22 の FFM API）ことも検討した。FFM で `IDispatch::GetIDsOfNames` / `Invoke` と `VARIANT` の詰め替えを書く手間が大きく、印刷設定の切り替え（`SetPrinter`）も含めて Windows 固有の処理が Java 側に広がる。

## Decision

- **印刷は PowerShell スクリプトに任せる。** `Print-Deck.ps1` をリソースに同梱し、`powershell.exe`（Windows PowerShell 5.1、Windows 標準）で実行する。スクリプトは PowerPoint を COM で開き、`PrintOptions.FitToPage = false` で `PrintOut` を呼び、ファイル名を渡して Microsoft Print to PDF に出力する。
- **用紙は A3 横に、ユーザーごとの印刷設定で切り替える。** `winspool.drv` の `SetPrinter` Level 9 で現在のユーザーの `DEVMODE` を書き換え、印刷後に元へ戻す。Level 9 は管理者権限が要らない（全ユーザー共通の Level 8 は要る）。`DEVMODEW` のオフセットは `dmFields` 72、`dmOrientation` 76、`dmPaperSize` 78。
- **切り抜きは Apache PDFBox で行う。** 各ページの `MediaBox` と `CropBox` を、ページ中央のスライド寸法の矩形に設定する。スライド寸法は pptx 内の `ppt/presentation.xml` の `sldSz`（EMU、12700 で割るとポイント）から読む。
- Java 側は `DeckPrinter`（印刷）、`PdfCropper`（切り抜き）、`SlideSizeReader`（寸法）のインタフェースに分け、`PrintAndCropDeckConverter` が順に呼ぶ。Windows 固有の処理は `PowerShellDeckPrinter` とスクリプトに閉じる。

## Consequences

- 実行環境に PowerPoint、Windows PowerShell 5.1、Microsoft Print to PDF が必要。macOS や Linux では動かない。
- スクリプトは BOM 付き UTF-8 で保存する。PowerShell 5.1 は BOM の無い UTF-8 を日本語 Windows では Shift-JIS として読むため。
- 印刷中は Microsoft Print to PDF の印刷設定が一時的に A3 横になる。スクリプトが異常終了した場合に備え、`finally` で元に戻す。
- 半透明など描画の品質は Microsoft Print to PDF の出力に依存する。PowerPoint の PDF 保存と同じ XPS 経路なので、実用上の差は見られなかった。
- 印刷を伴うテストは PowerPoint が必要なので、環境変数 `SSS_PPTX2PDF_DECK` があるときだけ動く統合テストにした。
