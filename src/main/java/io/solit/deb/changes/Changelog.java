package io.solit.deb.changes;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author yaga
 * @since 13.03.18
 */
public class Changelog {
    private List<ChangeSet> changes;

    public Changelog(List<ChangeSet> changeSet) {
        if (changeSet == null || changeSet.isEmpty())
            throw new IllegalArgumentException("ChangeSet can not be emtpy");
        changes = changeSet.stream().sorted(Comparator.comparing(ChangeSet::getDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    public Changelog(ChangeSet... changeSet) {
        this(Arrays.asList(changeSet));
    }

    public List<ChangeSet> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    public void addChangeSet(ChangeSet changeSet) {
        Objects.requireNonNull(changeSet, "Change set should not be null");
        int ind = Collections.binarySearch(changes, changeSet, Comparator.comparing(ChangeSet::getDate, Comparator.reverseOrder()));
        if (ind < 0)
            ind = -ind - 1;
        changes.add(ind, changeSet);
    }

    public void write(Writer writer) throws IOException {
        String separator = "";
        for (ChangeSet set: changes) {
            writer.write(separator);
            set.write(writer);
            separator = "\n\n";
        }
    }
}
