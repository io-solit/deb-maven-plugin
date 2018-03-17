package io.solit.deb.changes;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * @author yaga
 * @since 13.03.18
 */
public class StringChanges implements Changes {
    public static final StringChanges YANKED = new StringChanges("yanked");
    private final String changes;

    public StringChanges(String changes) {
        this.changes = Objects.requireNonNull(changes, "Changes can not be null");
    }

    @Override
    public Iterator<String> iterator() {
        return Arrays.stream(changes.split("\\r\\n|[\\r\\n]"))
                .map(String::trim)
                .iterator();
    }
}
