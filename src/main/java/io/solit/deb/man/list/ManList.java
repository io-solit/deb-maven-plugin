package io.solit.deb.man.list;

import io.solit.deb.man.ManPart;
import io.solit.deb.man.RoffWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yaga
 * @since 05.03.18
 */
public abstract class ManList implements ManPart {
    private final List<ListElement> items = new ArrayList<>();

    public void addItem(ManPart part) {
        getItems().add(new ListElement(part));
    }

    public List<ListElement> getItems() {
        return items;
    }

    @Override
    public void write(RoffWriter writer) throws IOException {
        int index = 0;
        for (ListElement item: items) {
            writer.startIndentedParagraph(getMarker(index++), INDENT_STEP);
            item.write(writer);
        }
    }

    protected abstract String getMarker(int itemIndex);
}
