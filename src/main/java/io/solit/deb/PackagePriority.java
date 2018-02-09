package io.solit.deb;

/**
 * @author yaga
 * @since 19.01.18
 */
public enum PackagePriority {
    required,
    important,
    standard,
    optional,
    @Deprecated
    extra
}
