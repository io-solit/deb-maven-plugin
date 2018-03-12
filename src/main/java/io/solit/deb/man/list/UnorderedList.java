package io.solit.deb.man.list;

/**
 * @author yaga
 * @since 05.03.18
 */
public class UnorderedList extends ManList {
    private char marker;

    public UnorderedList(char marker) {
        this.marker = marker;
    }

    @Override
    protected String getMarker(int itemIndex) {
        return String.valueOf(marker);
    }

    public char getMarker() {
        return marker;
    }
}
