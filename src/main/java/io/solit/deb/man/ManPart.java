package io.solit.deb.man;

import java.io.IOException;

/**
 * @author yaga
 * @since 05.03.18
 */
public interface ManPart {
    int INDENT_STEP = 4;

    void write(RoffWriter writer) throws IOException;

}
