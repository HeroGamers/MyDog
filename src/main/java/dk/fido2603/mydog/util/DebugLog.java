package dk.fido2603.mydog.util;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

/** Logs only while {@code debug} is on in config.yml. */
public final class DebugLog {
    private final Logger log;
    private final BooleanSupplier enabled;

    public DebugLog(Logger log, BooleanSupplier enabled) {
        this.log = log;
        this.enabled = enabled;
    }

    public void log(Supplier<String> message) {
        if (enabled.getAsBoolean()) {
            log.info("[Debug] " + message.get());
        }
    }
}
