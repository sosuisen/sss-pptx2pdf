package com.sosuisha.pptx2pdf.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.service.DeckPrinter;

/**
 * Prints a deck through Windows PowerShell: the bundled script opens the deck
 * in PowerPoint (COM automation), switches the user's preferences of the
 * "Microsoft Print to PDF" printer to A3 landscape for the duration of the
 * print, and prints the slides at actual size to the PDF file. The script is
 * extracted from the resources to a temporary file on first use.
 */
public class PowerShellDeckPrinter implements DeckPrinter {
    private static final String SCRIPT_RESOURCE = "/scripts/Print-Deck.ps1";
    private static final String POWERSHELL = "powershell.exe";
    private static final long TIMEOUT_MINUTES = 15;

    private final String printerName;
    private @Nullable Path script;

    /**
     * Creates the printer that prints through "Microsoft Print to PDF".
     */
    public PowerShellDeckPrinter() {
        this("Microsoft Print to PDF");
    }

    /**
     * Creates the printer that prints through the given Windows printer. The
     * printer must write a PDF file when it is given an output file name.
     *
     * @param printerName name of the Windows printer
     * @throws NullPointerException     if printerName is null
     * @throws IllegalArgumentException if printerName is blank
     */
    public PowerShellDeckPrinter(String printerName) {
        Objects.requireNonNull(printerName, "printerName must not be null");
        if (printerName.isBlank()) {
            throw new IllegalArgumentException("printerName must not be blank");
        }
        this.printerName = printerName;
    }

    @Override
    public void print(Path deck, Path pdf) throws ConversionException {
        Objects.requireNonNull(deck, "deck must not be null");
        Objects.requireNonNull(pdf, "pdf must not be null");
        var command = List.of(
            POWERSHELL, "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
            "-File", scriptPath().toString(),
            "-Path", deck.toAbsolutePath().toString(),
            "-Out", pdf.toAbsolutePath().toString(),
            "-Printer", printerName
        );
        var output = run(command, deck);
        if (!Files.exists(pdf)) {
            throw new ConversionException(
                "The printer wrote no PDF for " + deck + ".\n" + output, null
            );
        }
    }

    private String run(List<String> command, Path deck) {
        try {
            var process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output;
            try (var in = process.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new ConversionException(
                    "Printing " + deck + " did not finish in " + TIMEOUT_MINUTES + " minutes.",
                    null
                );
            }
            if (process.exitValue() != 0) {
                throw new ConversionException(
                    "Printing " + deck + " failed (exit code " + process.exitValue() + ").\n"
                        + output,
                    null
                );
            }
            return output;
        } catch (IOException e) {
            throw new ConversionException("Cannot start " + POWERSHELL + " to print " + deck, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConversionException("Printing " + deck + " was interrupted.", e);
        }
    }

    private synchronized Path scriptPath() {
        var extracted = script;
        if (extracted != null && Files.exists(extracted)) { return extracted; }
        try (var in = PowerShellDeckPrinter.class.getResourceAsStream(SCRIPT_RESOURCE)) {
            if (in == null) {
                throw new ConversionException(
                    "The print script is missing from the application: " + SCRIPT_RESOURCE, null
                );
            }
            var file = Files.createTempFile("sss-pptx2pdf-", ".ps1");
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            file.toFile().deleteOnExit();
            script = file;
            return file;
        } catch (IOException e) {
            throw new ConversionException("Cannot extract the print script.", e);
        }
    }
}
