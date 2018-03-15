package io.solit.deb.changes;

import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.node.Visitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;

/**
 * @author yaga
 * @since 13.03.18
 */
public class MarkdownChanges implements Changes {
    private final List<Node> content = new ArrayList<>();

    public MarkdownChanges(List<Node> content) {
        if (Objects.requireNonNull(content, "Content should not be null").isEmpty())
            throw new IllegalArgumentException("Content should not be empty");
        this.content.addAll(content);
    }

    public MarkdownChanges(Node... content) {
        this(Arrays.asList(content));
    }

    @Override
    public Iterator<String> iterator() {
        return new ToStringVisitor(this.content);
    }

    public List<Node> getContent() {
        return content;
    }

    private static class ToStringVisitor implements Visitor, Iterator<String> {
        private Queue<String> buffer = new ArrayDeque<>();
        private Deque<Node> front = new ArrayDeque<>();
        private Deque<Prefix> prefixFront = new ArrayDeque<>();
        private StringBuilder lineBuilder;

        public ToStringVisitor(List<Node> nodes) {
            for (ListIterator<Node> i = nodes.listIterator(nodes.size()); i.hasPrevious();) {
                front.push(i.previous());
                prefixFront.push(new Prefix(""));
            }
        }

        private void scheduleInline(Node node, MarkerNode... markers) {
            for(int i = markers.length; i > 0; i--)
                front.push(markers[i - 1]);
            for(Node n = node.getLastChild(); n != null; n = n.getPrevious())
                front.push(n);
        }

        @Override
        public void visit(BlockQuote blockQuote) {
            Prefix p = this.prefixFront.pop().extend("> ");
            if (front.size() != this.prefixFront.size())
                throw new AssertionError("Prefix size de-sync");
            for(Node n = blockQuote.getLastChild(); n != null; n = n.getPrevious()) {
                front.push(n);
                this.prefixFront.push(p);
            }
        }

        @Override
        public void visit(BulletList bulletList) {
            Prefix p = this.prefixFront.pop().extend(bulletList.getBulletMarker() + "   ", "    ");
            for(Node n = bulletList.getLastChild(); n != null; n = n.getPrevious()) {
                front.push(n);
                this.prefixFront.push(p);
            }
        }

        @Override
        public void visit(Code code) {
            getLineBuilder().append('`').append(code.getLiteral()).append('`');
        }

        @Override
        public void visit(Document document) {
            Prefix p = this.prefixFront.pop();
            if (front.size() != this.prefixFront.size())
                throw new AssertionError("Prefix size de-sync");
            for (Node n = document.getLastChild(); n != null; n = n.getPrevious()) {
                front.push(n);
                this.prefixFront.push(p);
            }
        }

        @Override
        public void visit(Emphasis emphasis) {
            getLineBuilder().append(emphasis.getOpeningDelimiter());
            scheduleInline(emphasis, getClosingNode(emphasis.getClosingDelimiter()));
        }

        public void writeCodeBlock(String literal, CharSequence header, CharSequence footer, CharSequence infix) {
            Prefix prefix = this.prefixFront.pop();
            int index;
            if (header != null) {
                buffer.add(prefix.firstLine().append(header).toString());
                index = 1;
            } else
                index = 0;
            for(String s: literal.split("\r\n|[\r\n]"))
                if (index++ == 0)
                    buffer.add(prefix.firstLine().append(infix).append(s).toString());
                else
                    buffer.add(prefix.continuationLine().append(infix).append(s).toString());
            if (footer != null)
                if (index == 0)
                    buffer.add(prefix.firstLine().append(footer).toString());
                else
                    buffer.add(prefix.continuationLine().append(footer).toString());
        }

        @Override
        public void visit(FencedCodeBlock fencedCodeBlock) {
            StringBuilder header = new StringBuilder(fencedCodeBlock.getFenceLength() + fencedCodeBlock.getInfo().length());
            StringBuilder footer = new StringBuilder(fencedCodeBlock.getFenceLength());
            for(int i = 0; i < fencedCodeBlock.getFenceLength(); i++) {
                header.append(fencedCodeBlock.getFenceChar());
                footer.append(fencedCodeBlock.getFenceChar());
            }
            writeCodeBlock(fencedCodeBlock.getLiteral(), header.append(fencedCodeBlock.getInfo()), footer, "");
        }

        @Override
        public void visit(HardLineBreak hardLineBreak) {
            getLineBuilder().append("  ");
            flush(prefixFront.peek().continuationLine());
        }

        @Override
        public void visit(Heading heading) {
            prefixFront.push(prefixFront.pop().extend(String.join("", Collections.nCopies(heading.getLevel(), "#")) + " "));
            scheduleInline(heading, getFlushNode());
        }

        @Override
        public void visit(ThematicBreak thematicBreak) {
            flush(null);
            if (thematicBreak.getPrevious() instanceof Paragraph)
               buffer.add("");
            buffer.add("---");
        }

        @Override
        public void visit(HtmlInline htmlInline) {
            getLineBuilder().append(htmlInline.getLiteral());
        }

        @Override
        public void visit(HtmlBlock htmlBlock) {
            writeCodeBlock(htmlBlock.getLiteral(), null, null, "");
        }

        private void visitLink(Node node, String destination, String title, String open) {
            if (destination != null) {
                getLineBuilder().append(open).append("[");
                scheduleInline(node, getClosingNode("](" + destination + (title != null ? " \"" + title + "\"" : "") + ")"));
            } else
                scheduleInline(node);
        }

        @Override
        public void visit(Image image) {
            visitLink(image, image.getDestination(), image.getTitle(), "!");
        }

        @Override
        public void visit(IndentedCodeBlock indentedCodeBlock) {
            writeCodeBlock(indentedCodeBlock.getLiteral(), null, null, "    ");
        }

        @Override
        public void visit(Link link) {
            visitLink(link, link.getDestination(), link.getTitle(), "");
        }

        @Override
        public void visit(ListItem listItem) {
            Prefix p = this.prefixFront.pop(), c = p.continuation();
            if (front.size() != prefixFront.size())
                throw new AssertionError("Prefix size de-sync");
            for (Node n = listItem.getLastChild(); n != null; n = n.getPrevious()) {
                front.push(n);
                this.prefixFront.push(n.getPrevious() == null ? p : c);
            }
        }

        @Override
        public void visit(OrderedList orderedList) {
            Deque<Prefix> prefixes = new ArrayDeque<>();
            Prefix cur = prefixFront.pop();
            if (front.size() != prefixFront.size())
                throw new AssertionError("Prefix size de-sync");
            int index = 0;
            for (Node n = orderedList.getFirstChild(); n != null; n = n.getNext()) {
                StringBuilder fl = new StringBuilder(), cl = new StringBuilder();
                fl.append(orderedList.getStartNumber() + index++).append(".");
                for (int i = fl.length(); i > 0; i--)
                    cl.append(' ');
                for (int i = 4 - fl.length(); i > 0; i--) {
                    fl.append(' ');
                    cl.append(' ');
                }
                prefixes.push(cur.extend(fl, cl));
            }
            for (Node n = orderedList.getLastChild(); n != null; n = n.getPrevious()) {
                front.push(n);
                this.prefixFront.push(prefixes.pop());
            }
        }

        @Override
        public void visit(Paragraph paragraph) {
            if (paragraph.getPrevious() instanceof Paragraph || paragraph.getPrevious() instanceof ListBlock)
                buffer.add(prefixFront.peek().firstLine().toString().trim());
            scheduleInline(paragraph, getFlushNode());
        }

        @Override
        public void visit(SoftLineBreak softLineBreak) {
            flush(prefixFront.peek().continuationLine());
        }

        @Override
        public void visit(StrongEmphasis strongEmphasis) {
            getLineBuilder().append(strongEmphasis.getOpeningDelimiter());
            String closing = strongEmphasis.getClosingDelimiter();
            scheduleInline(strongEmphasis, getClosingNode(closing));
        }

        @Override
        public void visit(Text text) {
            getLineBuilder().append(text.getLiteral());
        }

        @Override
        public void visit(CustomBlock customBlock) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(CustomNode customNode) {
            if (customNode instanceof MarkerNode) {
               MarkerNode.class.cast(customNode).getRunnable().run();
            } else
                throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            while (buffer.isEmpty() && !front.isEmpty()) {
                front.pop().accept(this);
            }
            return !buffer.isEmpty();
        }

        @Override
        public String next() {
            if (!hasNext())
                throw new NoSuchElementException();
            return buffer.poll();
        }

        private MarkerNode getClosingNode(String closing) {
            return new MarkerNode(() -> getLineBuilder().append(closing));
        }

        private MarkerNode getFlushNode() {
            return new MarkerNode(() -> {
                flush(null);
                prefixFront.pop();
            });
        }

        private void flush(StringBuilder replacement) {
            if (lineBuilder != null) {
                buffer.add(lineBuilder.toString());
                lineBuilder = replacement;
            }
        }

        private StringBuilder getLineBuilder() {
            if (lineBuilder == null)
                lineBuilder = prefixFront.peek().firstLine();
            return lineBuilder;
        }
    }

    private static class MarkerNode extends CustomNode {
        private final Runnable runnable;

        private MarkerNode(Runnable runnable) {
            this.runnable = runnable;
        }

        public Runnable getRunnable() {
            return runnable;
        }
    }

    private static class Prefix {
        private final String prefix;
        private final String continuationPrefix;

        public Prefix(String prefix, String continuationPrefix) {
            this.prefix = prefix;
            this.continuationPrefix = continuationPrefix;
        }

        public Prefix(String prefix) {
            this.prefix = prefix;
            this.continuationPrefix = prefix;
        }

        public Prefix extend(CharSequence extension) {
            return new Prefix(prefix + extension);
        }

        public Prefix extend(CharSequence extension, CharSequence continuation) {
            return new Prefix(prefix + extension, prefix + continuation);
        }

        public Prefix continuation() {
            return new Prefix(continuationPrefix);
        }

        public StringBuilder firstLine() {
            return new StringBuilder(prefix);
        }

        public StringBuilder continuationLine() {
            return new StringBuilder(continuationPrefix);
        }
    }
}
