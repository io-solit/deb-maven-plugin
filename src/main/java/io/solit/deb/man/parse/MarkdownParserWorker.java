package io.solit.deb.man.parse;

import io.solit.deb.MarkdownUtils;
import io.solit.deb.man.ManPage;
import io.solit.deb.man.ManPart;
import io.solit.deb.man.Section;
import io.solit.deb.man.block.Example;
import io.solit.deb.man.block.Quote;
import io.solit.deb.man.block.Subheader;
import io.solit.deb.man.list.DefinitionList;
import io.solit.deb.man.list.DefinitionList.DefinitionItem;
import io.solit.deb.man.list.ListElement;
import io.solit.deb.man.list.NumberedList;
import io.solit.deb.man.list.UnorderedList;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Delimited;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yaga
 * @since 06.03.18
 */
class MarkdownParserWorker extends AbstractVisitor {
    private final static Pattern HEADER_PATTERN = Pattern.compile("^\\s*(\\S+)\\((\\d+)\\)\\s*-+(.+)$");
    private final String source;
    private final String manual;
    private final String defaultName;
    private final int defaultSection;
    private final String defaultDescription;
    private final Consumer<String> warningHandler;
    private Node currentNode;
    private ManPage manPage;
    private Deque<Consumer<ManPart>> containers = new ArrayDeque<>();
    private CurrentParagraph currentParagraph;

    public MarkdownParserWorker(
            String source, String manual, String defName, int defSection, String defDesc, Consumer<String> warningHandler
    ) {
        this.source = source;
        this.manual = manual;
        this.defaultName = defName;
        this.defaultSection = defSection;
        this.defaultDescription = defDesc;
        this.warningHandler = warningHandler;
    }

    public ManPage parse(Reader reader) throws IOException {
        Node root = Parser.builder()
                .build().parseReader(reader);
        manPage = createManPage(root);
        while (currentNode != null)
            readSection();
        return manPage;
    }

    private ManPage createManPage(Node root) {
        ManPage result = null;
        Node c;
        boolean skippedNodes = false;
        for (c = root.getFirstChild(); c != null; c = c.getNext()) {
            if (c instanceof Heading) {
                Heading h = (Heading) c;
                if (h.getLevel() != 1)
                    continue;
                String headerText;
                Matcher header = HEADER_PATTERN.matcher(headerText = MarkdownUtils.extractText(c));
                if (header.matches()) {
                    result = new ManPage(header.group(1).trim(), header.group(3).trim(), Integer.parseInt(header.group(2)));
                    if (skippedNodes)
                        warningHandler.accept("Encountered content before manual header. This part will be ignored");
                    break;
                } else
                    warningHandler.accept(
                            "Manual header '" + headerText + "'does not match a required pattern, looking for another one"
                    );
            } else
                skippedNodes = true;
        }
        if (result == null) {
            if (defaultName == null)
                throw new ManParseException("Unable to find manual header, check heading is present and is correct");
            warningHandler.accept("Unable to find manual header, falling back to defaults");
            result = new ManPage(defaultName, defaultDescription, defaultSection);
            currentNode = root.getFirstChild();
        } else {
            currentNode = c.getNext();
        }
        result.setSource(source);
        result.setManual(manual);
        return result;
    }

    private void readSection() {
        Section section;
        if (!(currentNode instanceof Heading) || Heading.class.cast(currentNode).getLevel() > 2) {
            section = new Section("\u00a0");
            warningHandler.accept("Encountered content without heading, using nameless section");
        } else {
            section = new Section(MarkdownUtils.extractText(currentNode));
            currentNode = currentNode.getNext();
        }
        manPage.getAdditionalSections().add(section);
        containers.push(section.getParts()::add);
        for (; currentNode != null; currentNode = currentNode.getNext()) {
            if (currentNode instanceof Heading && Heading.class.cast(currentNode).getLevel() <= 2)
                break;
            currentNode.accept(this);
        }
        containers.pop();
    }

    private CurrentParagraph getParagraph() {
        if (currentParagraph == null)
            throw new ManParseException("Text outside a paragraph");
        return currentParagraph;
    }

    private CurrentParagraph visitParagraph(Paragraph paragraph, boolean expectTerm) {
        CurrentParagraph cp = currentParagraph = new CurrentParagraph(expectTerm);
        containers.push(c -> {
            throw new ManParseException("Unexpected node in a paragraph");
        });
        visitChildren(paragraph);
        containers.pop();
        currentParagraph = null;
        return cp;
    }

    @Override
    public void visit(BlockQuote blockQuote) {
        Quote quote = new Quote();
        containers.peek().accept(quote);
        containers.push(quote.getParts()::add);
        visitChildren(blockQuote);
        containers.pop();
    }

    @Override
    public void visit(BulletList bulletList) {
        new BulletListVisitor(bulletList.getBulletMarker() == '*' ? '\u00b7' : '\u2014').visit(bulletList);
    }

    @Override
    public void visit(Code code) {
        getParagraph().bold(true);
        getParagraph().monospace(true);
        getParagraph().appendText(code.getLiteral());
        getParagraph().monospace(false);
        getParagraph().bold(false);
    }

    private <T extends Node&Delimited> void delimited(T node) {
        if (node.getOpeningDelimiter().charAt(0) == '*') {
            getParagraph().bold(true);
            visitChildren(node);
            getParagraph().bold(false);
        } else {
            getParagraph().italic(true);
            visitChildren(node);
            getParagraph().italic(false);
        }
    }

    @Override
    public void visit(Emphasis emphasis) {
        delimited(emphasis);
    }

    @Override
    public void visit(FencedCodeBlock fencedCodeBlock) {
        if(fencedCodeBlock.getLiteral() != null && !fencedCodeBlock.getLiteral().isEmpty())
            containers.peek().accept(new Example(fencedCodeBlock.getLiteral()));
    }

    @Override
    public void visit(HardLineBreak hardLineBreak) {
       getParagraph().breakLine();
    }

    @Override
    public void visit(Heading heading) {
        String text = MarkdownUtils.extractText(heading).trim();
        if (!text.isEmpty())
            containers.peek().accept(new Subheader(text));
    }

    @Override
    public void visit(HtmlInline htmlInline) {
        getParagraph().italic(true);
        String literal = htmlInline.getLiteral();
        int from = 0, to = literal.length();
        if (literal.startsWith("<", from))
            from += 1;
        if (literal.endsWith(">"))
            to --;
        literal = literal.substring(from, to);
        getParagraph().appendText(literal);
        getParagraph().italic(false);
    }

    @Override
    public void visit(HtmlBlock htmlBlock) {
        containers.peek().accept(new Example(htmlBlock.getLiteral()));
    }

    @Override
    public void visit(Image image) {
        if (image.getDestination() != null)
            getParagraph().beginLink(image.getDestination());
        visitChildren(image);
        if (image.getDestination() != null)
            getParagraph().completeLink();
    }

    @Override
    public void visit(IndentedCodeBlock indentedCodeBlock) {
        if(indentedCodeBlock.getLiteral() != null && !indentedCodeBlock.getLiteral().isEmpty())
            containers.peek().accept(new Example(indentedCodeBlock.getLiteral()));
    }

    @Override
    public void visit(Link link) {
        if (link.getDestination() != null)
            getParagraph().beginLink(link.getDestination());
        visitChildren(link);
        if (link.getDestination() != null)
            getParagraph().completeLink();
    }

    @Override
    public void visit(OrderedList orderedList) {
        NumberedList part = new NumberedList();
        containers.peek().accept(part);
        for (Node n = orderedList.getFirstChild(); n != null; n = n.getNext()) {
            ListElement item = new ListElement();
            part.getItems().add(item);
            containers.push(item.getParts()::add);
            visitChildren(n);
            containers.pop();
        }
    }

    @Override
    public void visit(Paragraph paragraph) {
        containers.peek().accept(visitParagraph(paragraph, false).returnParagraph());
    }

    @Override
    public void visit(SoftLineBreak softLineBreak) {
        getParagraph().feedLine();
    }

    @Override
    public void visit(StrongEmphasis strongEmphasis) {
        delimited(strongEmphasis);
    }

    @Override
    public void visit(Text text) {
        getParagraph().appendText(text.getLiteral());
    }

    private class BulletListVisitor {
        private final char marker;
        private UnorderedList list;
        private DefinitionList definitions;

        private BulletListVisitor(char marker) {
            this.marker = marker;
        }

        private void addItem(ListElement item) {
            if (list == null) {
                containers.peek().accept(list = new UnorderedList(marker));
                definitions = null;
            }
            list.getItems().add(item);
        }

        private void addDefinition(DefinitionItem item) {
            if (definitions == null) {
                containers.peek().accept(definitions = new DefinitionList());
                list = null;
            }
            definitions.getItems().add(item);
        }

        private ListElement fillItem(ListElement item, Node contentNode) {
            containers.push(item.getParts()::add);
            for (Node n = contentNode; n != null; n = n.getNext())
                n.accept(MarkdownParserWorker.this);
            containers.pop();
            return item;
        }

        public void visit(BulletList bulletList) {
            for (Node n = bulletList.getFirstChild(); n != null; n = n.getNext()) {
                if (n.getFirstChild() instanceof Paragraph) {
                    CurrentParagraph p = visitParagraph((Paragraph) n.getFirstChild(), true);
                    DefinitionItem di = p.returnDefinitionItem();
                    if (di != null && (!di.getDefinition().getParts().isEmpty() || n.getFirstChild().getNext() != null)) {
                        fillItem(di.getDefinition(), n.getFirstChild().getNext());
                        addDefinition(di);
                    } else if (di != null)
                        addItem(fillItem(new ListElement(di.getTerm()), n.getFirstChild().getNext()));
                    else
                        addItem(fillItem(new ListElement(p.returnParagraph()), n.getFirstChild().getNext()));
                } else
                    addItem(fillItem(new ListElement(), n.getFirstChild()));
            }
        }

    }

}
