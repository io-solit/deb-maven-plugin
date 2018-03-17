package io.solit.deb;

import org.commonmark.node.Code;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author yaga
 * @since 14.03.18
 */
public class MarkdownUtils {
    public static String extractText(Node node) {
        Deque<Node> nodes = new ArrayDeque<>();
        StringBuilder builder = new StringBuilder();
        for (Node n = node; n != null; n = nodes.poll()) {
            if (n instanceof Text)
                builder.append(Text.class.cast(n).getLiteral());
            if (n instanceof Code)
                builder.append(Code.class.cast(n).getLiteral());
            if (n instanceof Link) {
                Link l = (Link) n;
                if (l.getFirstChild() == null) {
                    if (l.getTitle() != null)
                        builder.append(l.getTitle());
                    else if (l.getDestination() != null)
                        builder.append(l.getDestination());
                }
            }
            if (n instanceof SoftLineBreak || n instanceof HardLineBreak)
                builder.append(" ");
            if (n instanceof HtmlInline)
                builder.append(HtmlInline.class.cast(n).getLiteral());
            for (Node c = n.getLastChild(); c != null; c = c.getPrevious())
                nodes.push(c);
        }
        return builder.toString();
    }
}
