# SSS pptx2pdf

A small JavaFX tool that turns PowerPoint decks (`.pptx`) into PDF files whose
pages have exactly the slide size (for example 16:9).

- Drop `.pptx` files on the window, or pick them with the file chooser.
- Each deck is printed through the Windows printer "Microsoft Print to PDF"
  at 100 % on A3 landscape, and every page is then cropped to the slide size.
- The PDF is written next to the deck with the same name.

Why print instead of "Save as PDF"? PowerPoint's own PDF export writes the
spaces at run boundaries (for example between `git` and `add` when the words
have different formatting) as positions instead of characters, so they are
lost when the text is copied from the PDF. The print path keeps them. Why
crop? "Microsoft Print to PDF" is a V4 class driver and cannot use custom
paper sizes, so a 16:9 paper cannot be registered for it.

## Requirements

- Windows with PowerPoint (the print is done by COM automation)
- Windows PowerShell 5.1 (`powershell.exe`, part of Windows)
- The "Microsoft Print to PDF" printer (part of Windows)
- Java 25 and Maven for building

JavaFX libraries are downloaded by Maven, so no extra install is needed.

## How to Run

```bash
mvn javafx:run
```

## How to Test

```bash
mvn test
```

The end-to-end print test needs PowerPoint and runs only when the environment
variable `SSS_PPTX2PDF_DECK` names a `.pptx` file:

```powershell
$env:SSS_PPTX2PDF_DECK = 'C:\path\to\deck.pptx'
mvn test -Dtest=PowerShellDeckPrinterIntegrationTest
```

## How to Package

```bash
mvn package
```

This creates a native application image with jpackage under `target/jpackage/`.

## How it works

1. `PptxSlideSizeReader` reads the slide size from `ppt/presentation.xml`
   inside the pptx file.
2. `PowerShellDeckPrinter` runs the bundled `scripts/Print-Deck.ps1`, which
   switches the user's printing preferences of "Microsoft Print to PDF" to A3
   landscape (per-user, no admin rights), prints the deck at actual size to a
   temporary PDF, and restores the preferences.
3. `PdfBoxPdfCropper` sets the media box and crop box of every page to the
   slide rectangle centred on the page.
