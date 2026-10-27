package ro.mateiavram.pm;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClipboardHelper {

    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    private String lastCopied;

    public void copyAndClearLater(String text, int seconds) {
        clipboard.setContents(new StringSelection(text), null);
        lastCopied = text;
        service.schedule(() -> {
            try {
                String current = (String) clipboard.getData(DataFlavor.stringFlavor);
                if (current.equals(text)) {
                    clipboard.setContents(new StringSelection(""), null);
                }
            } catch (UnsupportedFlavorException | IOException e) {

            }
        }, seconds, TimeUnit.SECONDS);
    }

    public void clearNow() {
        if (lastCopied != null) {
            try {
                String actual = (String) clipboard.getData(DataFlavor.stringFlavor);
                if (actual.equals(lastCopied)) {
                    clipboard.setContents(new StringSelection(""), null);
                }
            } catch (UnsupportedFlavorException | IOException e) {

            } finally {
                service.shutdownNow();
            }


        }

    }
}
