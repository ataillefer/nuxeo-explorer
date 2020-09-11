/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.apidoc.snapshot;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

public interface DistributionSnapshotDesc {

    String getVersion();

    String getName();

    Date getCreationDate();

    Date getReleaseDate();

    @JsonIgnore
    boolean isLive();

    // errors management

    /**
     * Returns the errors on this artifact.
     * <p>
     * Should be consistent with {@link #getNumErrors()}
     *
     * @since 20.0.0
     */
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    List<String> getErrors();

    /**
     * Sets errors on this artifact.
     * <p>
     * Should impact {@link #getNumErrors()}
     *
     * @since 20.0.0
     */
    void setErrors(List<String> errors);

    /**
     * Returns the warnings on this artifact.
     * <p>
     * Should be consistent with {@link #getNumWarnings()}
     *
     * @since 20.0.0
     */
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    List<String> getWarnings();

    /**
     * Sets warnings on this artifact.
     * <p>
     * Should impact {@link #getNumWarnings()}
     *
     * @since 20.0.0
     */
    void setWarnings(List<String> warnings);

}
