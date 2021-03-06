/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.snapshot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.export.api.Exporter;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.security.SecurityHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.runtime.RuntimeServiceException;

public interface SnapshotManager {

    /**
     * Name alias for the current "live" distribution retrieval.
     *
     * @since 20.0.0
     */
    public static String DISTRIBUTION_ALIAS_CURRENT = "current";

    /**
     * Name alias for the current "live" distribution retrieval.
     * <p>
     * Alternative to {@link #DISTRIBUTION_ALIAS_CURRENT} for the distribution to be displayed in "admin" embedded mode
     * without showing explorer headers and such.
     *
     * @since 20.0.0
     */
    public static String DISTRIBUTION_ALIAS_ADM = "adm";

    /**
     * Name alias for the "latest" available distribution.
     * <p>
     * Distributions are filtered with name starting with "nuxeo platform" (case-insensitive check).
     *
     * @since 20.0.0
     */
    public static String DISTRIBUTION_ALIAS_LATEST = "latest";

    /** @since 20.0.0 */
    public static String DISTRIBUTION_ALIAS_LATEST_LTS = "latestLTS";

    /** @since 20.0.0 */
    public static String DISTRIBUTION_ALIAS_LATEST_FT = "latestFT";

    public static String PROPERTY_SITE_MODE = "org.nuxeo.apidoc.site.mode";

    public static String PROPERTY_USE_ES = "org.nuxeo.apidoc.use.elasticsearch";

    static Comparator<DistributionSnapshotDesc> DISTRIBUTION_COMPARATOR = Comparator.comparing(
            DistributionSnapshotDesc::getVersion, new VersionComparator())
                                                                                    .reversed()
                                                                                    .thenComparing(
                                                                                            DistributionSnapshotDesc::getName);

    /**
     * Initializes the web context, as potentially needed by plugins.
     *
     * @since 11.1
     */
    void initWebContext(HttpServletRequest request);

    /**
     * Returns the current runtime live snapshot.
     *
     * @throws RuntimeServiceException if the runtime live snapshot should not be made available (see
     *             {@link #isSiteMode()}.
     */
    DistributionSnapshot getRuntimeSnapshot();

    /**
     * Returns the distribution with given key or alias.
     * <p>
     * Can return null if distribution with given key is not found.
     *
     * @throws RuntimeServiceException if key is one of the live runtime aliases, while it should not be made available
     *             (see {@link #isSiteMode()}.
     */
    DistributionSnapshot getSnapshot(String key, CoreSession session);

    /**
     * Returns all persistent distributions, sorted.
     * <p>
     * Hidden distributions are included.
     * <p>
     * Trashed distributions are not included.
     */
    List<DistributionSnapshot> listPersistentSnapshots(CoreSession session);

    /**
     * Returns all persistent distributions, including aliases as keys.
     * <p>
     * Hidden distributions are included.
     * <p>
     * Trashed distributions are not included.
     */
    Map<String, DistributionSnapshot> getPersistentSnapshots(CoreSession session);

    /**
     * Returns persistent distributions with given key, potentially including aliases, sorted.
     * <p>
     * Hidden distributions are included.
     * <p>
     * Trashed distributions are not included.
     *
     * @since 20.0.0
     */
    List<DistributionSnapshot> getPersistentSnapshots(CoreSession session, String key, boolean includeAliases);

    /**
     * Returns available distributions, for user display, sorted.
     * <p>
     * Hidden distributions are not included.
     * <p>
     * Live distribution may be included, see
     * {@link SecurityHelper#canSnapshotLiveDistribution(org.nuxeo.ecm.core.api.NuxeoPrincipal)}
     */
    List<DistributionSnapshotDesc> getAvailableDistributions(CoreSession session);

    List<String> getAvailableVersions(CoreSession session, NuxeoArtifact nxItem);

    /**
     * Imports given snapshot as a nuxeo tree.
     * <p>
     * Corresponding distribution will be hidden until
     * #{@link #validateImportedSnapshot(CoreSession, String, Map, List)} is called (or until distribution is
     * unhidden).
     */
    DocumentModel importTmpSnapshot(CoreSession session, InputStream is)
            throws IOException, DocumentValidationException;

    /**
     * Imports given snapshot as a nuxeo tree.
     *
     * @param properties contains additional updates to be performed on distribution root document.
     * @param reservedKeys contains a list of reserved keywords for distribution names.
     * @since 20.0.0
     */
    void importSnapshot(CoreSession session, InputStream is, Map<String, Serializable> properties,
            List<String> reservedKeys) throws IOException, DocumentValidationException;

    /**
     * Validates the uploaded snapshot persistence with given properties.
     * <p>
     * Corresponding distribution should lready exist with given doc id. Distribution will be unhidden on validation.
     *
     * @param reservedKeys contains a list of reserved keywords for distribution names.
     * @since 20.0.0
     */
    void validateImportedSnapshot(CoreSession session, String distribDocId, Map<String, Serializable> properties,
            List<String> reservedKeys) throws DocumentValidationException;

    void exportSnapshot(CoreSession session, String key, OutputStream out) throws IOException;

    /**
     * Persists the runtime snapshot.
     *
     * @throws RuntimeServiceException if the runtime snapshot should not be accessed (see {@link #isSiteMode()} and
     *             {@link SecurityHelper#canSnapshotLiveDistribution(org.nuxeo.ecm.core.api.NuxeoPrincipal)}
     */
    DistributionSnapshot persistRuntimeSnapshot(CoreSession session);

    /**
     * Persists the runtime snapshot with given properties and filter.
     *
     * @throws RuntimeServiceException if the runtime snapshot should not be accessed (see {@link #isSiteMode()} and
     *             {@link SecurityHelper#canSnapshotLiveDistribution(org.nuxeo.ecm.core.api.NuxeoPrincipal)}
     * @since 20.0.0
     */
    DistributionSnapshot persistRuntimeSnapshot(CoreSession session, String name, Map<String, Serializable> properties,
            List<String> reservedKeys, SnapshotFilter filter) throws DocumentValidationException;

    /**
     * Returns all registered plugins.
     *
     * @since 11.1
     */
    List<Plugin<?>> getPlugins();

    /**
     * Returns the plugin with the given id.
     *
     * @since 11.1
     */
    Plugin<?> getPlugin(String id);

    /**
     * Returns true if site mode is enabled: this will prevent access to the live distribution.
     *
     * @since 20.0.0
     */
    boolean isSiteMode();

    /**
     * Returns exporters list.
     *
     * @since 20.0.0
     */
    List<Exporter> getExporters();

    /**
     * Returns exporter with given name.
     *
     * @since 20.0.0
     */
    Exporter getExporter(String id);

}
