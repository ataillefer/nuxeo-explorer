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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.introspection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.apidoc.documentation.SecureXMLHelper;
import org.nuxeo.common.utils.Path;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ComponentInfoImpl extends BaseNuxeoArtifact implements ComponentInfo {

    private static final Logger log = LogManager.getLogger(ComponentInfoImpl.class);

    protected final BundleInfo bundle;

    protected final String name;

    protected final List<ExtensionPointInfo> extensionPoints = new ArrayList<>();

    protected final List<ServiceInfo> services = new ArrayList<>();

    protected final List<ExtensionInfo> extensions = new ArrayList<>();

    /** @since 11.1 */
    protected final List<String> requirements = new ArrayList<>();

    /** @since 20.0.0 */
    protected Long resolutionOrder;

    /** @since 20.0.0 */
    protected Long startOrder;

    /** @since 20.0.0 */
    protected Long declaredStartOrder;

    protected URL xmlFileUrl;

    protected String xmlFileContent;

    protected String componentClass;

    protected String documentation;

    protected List<OperationInfo> operations = new ArrayList<>();

    public ComponentInfoImpl(BundleInfo bundle, String name) {
        this.bundle = bundle;
        this.name = name;
    }

    @JsonCreator
    private ComponentInfoImpl() {
        this.bundle = null;
        this.name = null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BundleInfo getBundle() {
        return bundle;
    }

    @Override
    public List<ExtensionPointInfo> getExtensionPoints() {
        return Collections.unmodifiableList(extensionPoints);
    }

    @Override
    public List<ExtensionInfo> getExtensions() {
        return Collections.unmodifiableList(extensions);
    }

    public void addExtensionPoint(ExtensionPointInfoImpl xp) {
        extensionPoints.add(xp);
    }

    public void addExtension(ExtensionInfoImpl xt) {
        extensions.add(xt);
    }

    @Override
    public String getDocumentation() {
        return documentation;
    }

    @Override
    public String getDocumentationHtml() {
        return DocumentationHelper.getHtml(getDocumentation());
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public void addService(String serviceName, boolean overriden) {
        ServiceInfo si = new ServiceInfoImpl(serviceName, overriden, this);
        services.add(si);
    }

    @Override
    public String getComponentClass() {
        return componentClass;
    }

    public void setComponentClass(String componentClass) {
        this.componentClass = componentClass;
    }

    @Override
    public boolean isXmlPureComponent() {
        return componentClass == null;
    }

    @Override
    public URL getXmlFileUrl() {
        return xmlFileUrl;
    }

    public void setXmlFileUrl(URL xmlFileUrl) {
        this.xmlFileUrl = xmlFileUrl;
    }

    @Override
    public String getXmlFileName() {
        if (xmlFileUrl == null) {
            return "";
        }
        String path = xmlFileUrl.getPath();
        String[] parts = path.split("!");
        if (parts.length == 2) {
            return parts[1];
        } else {
            return path;
        }
    }

    @Override
    public String getXmlFileContent() {
        if (xmlFileContent != null) {
            return xmlFileContent;
        }
        if (xmlFileUrl == null) {
            xmlFileContent = "";
            return xmlFileContent;
        }
        String path = xmlFileUrl.getPath();
        String[] parts = path.split("!");

        File jar = new File(parts[0].replace("file:", ""));
        if (!jar.exists()) {
            return "Unable to locate Bundle :" + parts[0];
        }

        try {
            String xml;
            if (jar.getAbsolutePath().endsWith(".xml")) {
                try (InputStream in = new FileInputStream(jar)) {
                    xml = IOUtils.toString(in, StandardCharsets.UTF_8);
                }
            } else if (jar.isDirectory()) {
                File file = new File(new Path(jar.getAbsolutePath()).append(parts[1]).toString());
                if (!file.exists()) {
                    return "Unable to locate file :" + file.getAbsolutePath();
                }
                xml = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            } else {
                try (ZipFile jarArchive = new ZipFile(jar)) {
                    ZipEntry entry = jarArchive.getEntry(parts[1].substring(1));
                    xml = IOUtils.toString(jarArchive.getInputStream(entry), StandardCharsets.UTF_8);
                }
            }
            xmlFileContent = SecureXMLHelper.secure(xml);
        } catch (IOException e) {
            log.error("Error while getting XML file", e);
            xmlFileContent = "";
        }
        return xmlFileContent;
    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getVersion() {
        return bundle.getVersion();
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public List<ServiceInfo> getServices() {
        return Collections.unmodifiableList(services);
    }

    @Override
    public String getHierarchyPath() {
        return getBundle().getHierarchyPath() + "/" + getId();
    }

    @Override
    public List<String> getRequirements() {
        return Collections.unmodifiableList(requirements);
    }

    public void addRequirement(String requirement) {
        requirements.add(requirement);
    }

    @Override
    public Long getResolutionOrder() {
        return resolutionOrder;
    }

    @Override
    public void setResolutionOrder(Long resolutionOrder) {
        this.resolutionOrder = resolutionOrder;
    }

    @Override
    public List<OperationInfo> getOperations() {
        return Collections.unmodifiableList(operations);
    }

    public void setOperations(List<OperationInfo> operations) {
        if (operations != null) {
            this.operations.clear();
            this.operations.addAll(operations);
        }
    }

    @Override
    public Long getStartOrder() {
        return startOrder;
    }

    @Override
    public void setStartOrder(Long order) {
        this.startOrder = order;
    }

    @Override
    public Long getDeclaredStartOrder() {
        return declaredStartOrder;
    }

    @Override
    public void setDeclaredStartOrder(Long order) {
        this.declaredStartOrder = order;
    }

}
