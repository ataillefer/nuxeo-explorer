/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.adapters;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @since 11.1
 */
public class PackageInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements PackageInfo {

    // cache lists

    protected Map<String, BundleInfo> bundles;

    protected List<ComponentInfo> components;

    protected List<ExtensionPointInfo> extensionPoints;

    protected List<ServiceInfo> services;

    protected List<ExtensionInfo> extensions;

    public static PackageInfoDocAdapter create(PackageInfo pkg, CoreSession session, String containerPath) {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);
        String name = computeDocumentName("pkg-" + pkg.getId());
        String targetPath = new Path(containerPath).append(name).toString();

        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, pkg.getTitle());
        doc.setPropertyValue(PROP_PACKAGE_ID, pkg.getId());
        doc.setPropertyValue(PROP_PACKAGE_NAME, pkg.getName());
        doc.setPropertyValue(PROP_VERSION, pkg.getVersion());
        doc.setPropertyValue(PROP_PACKAGE_TYPE, pkg.getPackageType());
        doc.setPropertyValue(PROP_BUNDLES, (Serializable) pkg.getBundles());
        doc.setPropertyValue(PROP_DEPENDENCIES, (Serializable) pkg.getDependencies());
        doc.setPropertyValue(PROP_OPTIONAL_DEPENDENCIES, (Serializable) pkg.getOptionalDependencies());
        doc.setPropertyValue(PROP_CONFLICTS, (Serializable) pkg.getConflicts());

        fillContextData(doc);
        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }
        return new PackageInfoDocAdapter(doc);
    }

    public PackageInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getId() {
        return safeGet(PROP_PACKAGE_ID);
    }

    @Override
    public String getName() {
        return safeGet(PROP_PACKAGE_NAME);
    }

    @Override
    public String getTitle() {
        return safeGet(TITLE_PROPERTY_PATH);
    }

    @Override
    public String getVersion() {
        return safeGet(PROP_VERSION);
    }

    @Override
    public String getPackageType() {
        return safeGet(PROP_PACKAGE_TYPE);
    }

    @Override
    public List<String> getBundles() {
        return safeGet(PROP_BUNDLES);
    }

    @Override
    public List<String> getDependencies() {
        return safeGet(PROP_DEPENDENCIES);
    }

    @Override
    public List<String> getOptionalDependencies() {
        return safeGet(PROP_OPTIONAL_DEPENDENCIES);
    }

    @Override
    public List<String> getConflicts() {
        return safeGet(PROP_CONFLICTS);
    }

    @Override
    public Map<String, BundleInfo> getBundleInfo() {
        if (bundles == null) {
            bundles = new LinkedHashMap<>();
            DistributionSnapshot snap = getParentNuxeoArtifact(DistributionSnapshot.class);
            getBundles().forEach(b -> {
                bundles.put(b, snap.getBundle(b));
            });
        }
        return Collections.unmodifiableMap(bundles);
    }

}
