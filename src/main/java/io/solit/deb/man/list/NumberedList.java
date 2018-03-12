package io.solit.deb.man.list;

/**
 * @author yaga
 * @since 05.03.18
 */
public class NumberedList extends ManList {
    @Override
    protected String getMarker(int itemIndex) {
        return Integer.toString(itemIndex + 1) + ".";
    }
}
