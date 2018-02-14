package io.solit.plugin.maven.deb.pack;

import io.solit.deb.Control;
import io.solit.deb.PackagePriority;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yaga
 * @since 22.01.18
 */
public class Attributes {
    private String section;
    private PackagePriority priority = PackagePriority.optional;
    private String source;
    private Boolean essential;
    private String builtUsing;
    private Set<String> depends = new HashSet<>();
    private Set<String> preDepends = new HashSet<>();
    private Set<String> recommends = new HashSet<>();
    private Set<String> suggests = new HashSet<>();
    private Set<String> enhances = new HashSet<>();
    private Set<String> breaks = new HashSet<>();
    private Set<String> conflicts = new HashSet<>();
    private Set<String> provides = new HashSet<>();

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public PackagePriority getPriority() {
        return priority;
    }

    public void setPriority(PackagePriority priority) {
        this.priority = priority;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Boolean getEssential() {
        return essential;
    }

    public void setEssential(Boolean essential) {
        this.essential = essential;
    }

    public String getBuiltUsing() {
        return builtUsing;
    }

    public void setBuiltUsing(String builtUsing) {
        this.builtUsing = builtUsing;
    }

    public Set<String> getDepends() {
        return depends;
    }

    public Set<String> getPreDepends() {
        return preDepends;
    }

    public Set<String> getRecommends() {
        return recommends;
    }

    public Set<String> getSuggests() {
        return suggests;
    }

    public Set<String> getEnhances() {
        return enhances;
    }

    public Set<String> getBreaks() {
        return breaks;
    }

    public Set<String> getConflicts() {
        return conflicts;
    }

    public Set<String> getProvides() {
        return provides;
    }

    void fillControl(Control control) {
        control.setSection(section);
        control.setPriority(priority);
        control.setSource(source);
        control.setEssential(essential);
        control.setBuildUsing(builtUsing);
        depends.stream().map(String::trim).forEach(control::addDepends);
        preDepends.stream().map(String::trim).forEach(control::addPreDepends);
        recommends.stream().map(String::trim).forEach(control::addRecommends);
        suggests.stream().map(String::trim).forEach(control::addSuggests);
        enhances.stream().map(String::trim).forEach(control::addEnhances);
        breaks.stream().map(String::trim).forEach(control::addBreaks);
        conflicts.stream().map(String::trim).forEach(control::addConflicts);
        provides.stream().map(String::trim).forEach(control::addProvides);
    }
}
