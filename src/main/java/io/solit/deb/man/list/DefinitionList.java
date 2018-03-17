package io.solit.deb.man.list;

import io.solit.deb.man.ManPart;
import io.solit.deb.man.RoffWriter;
import io.solit.deb.man.block.ManParagraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yaga
 * @since 05.03.18
 */
public class DefinitionList implements ManPart {
    private List<DefinitionItem> items = new ArrayList<>();

    public List<DefinitionItem> getItems() {
        return items;
    }

    public void addItem(ManParagraph term, ManPart definition) {
        getItems().add(new DefinitionItem(term, new ListElement(definition)));
    }

    @Override
    public void write(RoffWriter writer) throws IOException {
        for (DefinitionItem item: items) {
            writer.writeStructureLine();
            writer.startTaggedParagraph(INDENT_STEP);
            item.getTerm().write(writer);
            writer.write("\n");
            item.getDefinition().write(writer);
        }
    }

    public static class DefinitionItem {
        final ManParagraph term;
        final ListElement definition;

        public DefinitionItem(ManParagraph term, ListElement definition) {
            this.term = Objects.requireNonNull(term);
            this.definition = Objects.requireNonNull(definition);
        }

        public ManParagraph getTerm() {
            return term;
        }

        public ListElement getDefinition() {
            return definition;
        }
    }

}
