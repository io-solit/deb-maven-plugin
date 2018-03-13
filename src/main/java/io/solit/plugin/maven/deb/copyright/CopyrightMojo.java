package io.solit.plugin.maven.deb.copyright;

import io.solit.deb.copyright.Copyright;
import io.solit.deb.copyright.CopyrightFiles;
import io.solit.plugin.maven.deb.dependencies.AbstractDependencyMojo;
import org.apache.commons.compress.utils.Charsets;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.License;
import org.apache.maven.model.Organization;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.time.Year;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Create a machine readable copyright file. File is formatted to follow debian
 * <a href="https://www.debian.org/doc/packaging-manuals/copyright-format/1.0/">copyright specification</a>.
 * File will include information specified in a configuration or extracted from a pom file if no configuration present.
 * Additionally, if dependencies are included into a package, copyright file will include their licences too.
 * <p>
 *     Copyright info is build based on a <code>copyrightPatterns</code> parameter and gathered from a pom file if no copyrightPatterns specified.
 *     <ul>
 *         <li>
 *             Copyright is constructed from a copyright symbol, <code>inceptionYear</code> (if present), current
 *             year and holders:
 *             <ol>
 *                 <li>project organisation, if present</li>
 *                 <li>otherwise, project developers if present</li>
 *                 <li>otherwise, project contributors if present</li>
 *                 <li>otherwise <code>user.name</code> system property is used</li>
 *             </ol>
 *         </li>
 *         <li>
 *             For project license <code>mainLicence</code> parameter is used if specified, otherwise first of project licenses is used.
 *             If no licenses present, project is marked as a public domain.
 *         </li>
 *     </ul>
 * <p>
 *     If <code>dependencyCopyrights</code> parameter is set to true, all dependencies filtered by
 *     <code>traversalExclusions</code> and <code>packageExclusions</code> are grouped by their copyright and licenses
 *      and included as a separate file. Copyright and license strings are constructed the same way as for this project
 * @author yaga
 * @see <a href="https://www.debian.org/doc/packaging-manuals/copyright-format/1.0/">copyright specification</a>
 * @since 23.01.18
 */
@Mojo(name = "copyright", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class CopyrightMojo extends AbstractDependencyMojo<Copyright> {

    /**
     * Upstream name of deb package
     */
    @Parameter(defaultValue = "${project.artifactId}")
    private String upstreamName;

    /**
     * Upstream contact of deb package
     */
    @Parameter(defaultValue = "${project.url}")
    private Set<String> upstreamContact;

    /**
     * Url of upstream source code
     */
    @Parameter(defaultValue = "${project.scm.url}")
    private String source;

    /**
     * Disclaimer (No warranty for instance)
     */
    @Parameter
    private String copyrightDisclaimer;

    /**
     * Comment for a copyright
     */
    @Parameter
    private String copyrightComment;

    /**
     * Licence to use for the project as a whole
     */
    @Parameter
    private String mainLicence;

    /**
     * Copyright for the project as a whole
     */
    @Parameter
    private String copyrightText;

    /**
     * File patterns to specify copyright for. Each pattern includes
     * <ul>
     *     <li><code>files</code> - mandatory set of glob patterns for a file</li>
     *     <li><code>copyright</code> - mandatory copyright string eg <i>Copyright 2018, Some Guy</i></li>
     *     <li><code>licence</code> - mandatory name of license to use</li>
     *     <li><code>licenceContent</code> - an optional licence text</li>
     *     <li><code>comment</code> - an optional comment</li>
     * </ul>
     */
    @Parameter
    private List<CopyrightPatterns> copyrightPatterns;

    /**
     * Licences to include into a copyright file. Each licence consists of
     * <ul>
     *     <li><code>name</code> - mandatory license name</li>
     *     <li><code>file</code> - mandatory file, containing licence full text</li>
     *     <li><code>comment</code> - optional comment</li>
     * </ul>
     */
    @Parameter
    private List<LicenceFile> licesnces;

    /**
     * File to wright copyright to. If not specified will be created
     * at <code>${stageDir}/usr/share/doc/${packageName}/copyright
     */
    @Parameter
    private File copyrightFile;

    @Parameter(readonly = true, defaultValue = "${project.build.sourceEncoding}")
    private String sourceEncoding;

    /**
     * Whether to include copyrights for gathered dependencies
     */
    @Parameter
    private Boolean dependencyCopyrights;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try{
            File copyrightFile = this.copyrightFile;
            if (copyrightFile == null) {
                if (stageDir == null || packageName == null)
                    throw new IllegalArgumentException("Copyright file should be specified");
                copyrightFile = stageDir;
                for (String s : new String[]{"usr", "share", "doc", packageName, "copyright"})
                    copyrightFile = new File(copyrightFile, s);
            }
            Copyright copyright = createCopyright();
            fillCopyright(copyright);
            addLicences(copyright);
            if (Boolean.TRUE.equals(dependencyCopyrights))
                traverseDependencies(copyright);
            if (!copyrightFile.getParentFile().isDirectory() && !copyrightFile.getParentFile().mkdirs())
                throw new IOException("Unable to create directories " + copyrightFile.getParentFile());
            try(OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(copyrightFile), Charsets.UTF_8)) {
                copyright.writeCopyright(writer);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to create copyright " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException("Configuration error " + e.getMessage(), e);
        }
    }

    @Override
    protected void processDependency(DependencyArtifact node, Copyright copyright, File dependencyDir, boolean root) {
        if (root)
            return;
        if (node.getArtifact().getFile() == null)
            return;
        Path dependencyFile = new File(dependencyDir, node.getArtifact().getFile().getName()).toPath();
        String file = StreamSupport.stream(stageDir.toPath().relativize(dependencyFile).spliterator(), false)
                .map(Object::toString).collect(Collectors.joining("/", "/", ""));
        MavenProject project = node.getProject();
        String cpr = getProjectCopyright(project);
        if (cpr == null)
            return;
        licences: for (License l: project.getLicenses()) {
            if (l.getName() == null || l.getName().isEmpty())
                continue;
            for (CopyrightFiles f : copyright.getFiles()) {
                if (f.getLicence().equals(l.getName()) && f.getCopyright().equals(cpr)) {
                    if (!f.getFiles().contains("*"))
                        f.addFile(file);
                    continue licences;
                }
            }
            copyright.addFiles(Collections.singleton(file), cpr, l.getName());
        }
    }

    private void fillCopyright(Copyright copyright) {
        copyright.setComment(copyrightComment);
        copyright.setLicence(mainLicence);
        copyright.setDisclaimer(copyrightDisclaimer);
        if (copyrightText != null)
            copyright.setCopyright(copyrightText);
        copyright.setUpstreamName(upstreamName);
        copyright.setSource(source);
        if (upstreamContact != null)
            upstreamContact.forEach(copyright::addUpstreamContact);
    }

    private void addLicences(Copyright copyright) throws IOException {
        if (licesnces != null)
            for (LicenceFile l: licesnces)
                l.addToCopyright(sourceEncoding, copyright);
    }

    private Copyright createCopyright() {
        Copyright copyright;
        if (copyrightPatterns == null || copyrightPatterns.isEmpty()) {
            String cpr = copyrightText;
            if (cpr == null)
                cpr = getProjectCopyright(project);
            if (cpr == null || cpr.isEmpty())
                cpr = System.getProperty("user.name");
            if (mainLicence != null && mainLicence.isEmpty()) {
                copyright = new Copyright(Collections.singleton("*"), cpr, mainLicence);
                copyright.setCopyright(cpr);
            } else if (!project.getLicenses().isEmpty()) {
                Iterator<License> licenses = project.getLicenses().iterator();
                copyright = new Copyright(Collections.singleton("*"), cpr, licenses.next().getName());
                copyright.setCopyright(cpr);
                while (licenses.hasNext())
                    copyright.addFiles(Collections.singleton("*"), cpr, licenses.next().getName());
            } else {
                copyright = new Copyright(Collections.singleton("*"), cpr, "public-domain");
            }
        } else {
            Iterator<CopyrightPatterns> iterator = copyrightPatterns.iterator();
            CopyrightPatterns p = iterator.next();
            copyright = new Copyright(p.getFiles(), p.getCopyright(), p.getLicence(), p.getComment(), p.getLicenceContent());
            while (iterator.hasNext()) {
                p = iterator.next();
                CopyrightFiles files = copyright.addFiles(p.getFiles(), p.getCopyright(), p.getLicence());
                files.setLicenceContent(p.getLicenceContent());
                files.setComment(p.getComment());
            }
        }
        return copyright;
    }

    private String getProjectCopyright(MavenProject project) {
        String prefix;
        if (project.getInceptionYear() != null)
            prefix = "\u00a9 " + project.getInceptionYear() + "-" + Year.now().getValue() + " ";
        else
            prefix = "\u00a9 " + Year.now().getValue() + " ";
        String cpr = Optional.of(project).map(MavenProject::getOrganization)
                    .map(Organization::getName)
                    .map(prefix::concat)
                    .orElse(null);
        if (cpr == null || cpr.isEmpty())
            cpr = project.getDevelopers().stream()
                    .map(Contributor::getName).map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(prefix::concat)
                    .collect(Collectors.joining("\n"));
        if (cpr == null || cpr.isEmpty())
            cpr = project.getContributors().stream()
                    .map(Contributor::getName).map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(prefix::concat)
                    .collect(Collectors.joining("\n"));
        return cpr;
    }

}
