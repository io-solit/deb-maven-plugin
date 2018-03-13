package io.solit.deb.man.text;

import io.solit.deb.man.RoffWriter;

import java.io.IOException;

/**
 * @author yaga
 * @since 06.03.18
 */
public interface TextPart {

    void write(RoffWriter writer) throws IOException;

}
