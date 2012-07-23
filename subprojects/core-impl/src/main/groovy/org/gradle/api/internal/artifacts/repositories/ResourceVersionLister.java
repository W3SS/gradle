/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.repositories;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceVersionLister implements VersionLister {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceVersionLister.class);

    private ExternalResourceRepository repository;

    public ResourceVersionLister(ExternalResourceRepository repository) {
        this.repository = repository;
    }

    public VersionList getVersionList(ModuleRevisionId moduleRevisionId, String pattern, Artifact artifact) {
        ModuleRevisionId idWithoutRevision = ModuleRevisionId.newInstance(moduleRevisionId, IvyPatternHelper.getTokenString(IvyPatternHelper.REVISION_KEY));
        String partiallyResolvedPattern = IvyPatternHelper.substitute(pattern, idWithoutRevision, artifact);
        LOGGER.debug("Listing all in {}", partiallyResolvedPattern);
        try {
            List<String> versionStrings = listRevisionToken(partiallyResolvedPattern);
            return new DefaultVersionList(versionStrings);
        } catch (IOException e) {
            LOGGER.warn("problem while listing resources in {} with {}", partiallyResolvedPattern, repository);
            LOGGER.debug("Error when listing resources", e);
            return null;
        }
    }

    // lists all the values a token can take in a pattern, as listed by a given url lister
    private List<String> listRevisionToken(String pattern) throws IOException {
        String fileSep = repository.getFileSeparator();
        pattern = repository.standardize(pattern);
        String tokenString = IvyPatternHelper.getTokenString(IvyPatternHelper.REVISION_KEY);
        int index = pattern.indexOf(tokenString);
        if (index == -1) {
            LOGGER.info("Unable to find revision token in pattern {}.", pattern);
        } else if (tokenIsWholeDirectoryName(pattern, fileSep, tokenString, index)) {
            String root = pattern.substring(0, index);
            return listAll(root);
        } else {
            int slashIndex = pattern.substring(0, index).lastIndexOf(fileSep);
            String root = slashIndex == -1 ? "" : pattern.substring(0, slashIndex);
            LOGGER.debug("using {} to list all in {} ", repository, root);
            List all = repository.list(root);
            if (all != null) {
                LOGGER.debug("found {} urls", all.size());
                List ret = new ArrayList(all.size());
                int endNameIndex = pattern.indexOf(fileSep, slashIndex + 1);
                String namePattern;
                if (endNameIndex != -1) {
                    namePattern = pattern.substring(slashIndex + 1, endNameIndex);
                } else {
                    namePattern = pattern.substring(slashIndex + 1);
                }
                namePattern = namePattern.replaceAll("\\.", "\\\\.");
                String acceptNamePattern = ".*?"
                        + IvyPatternHelper.substituteToken(namePattern, IvyPatternHelper.REVISION_KEY, "([^" + fileSep
                        + "]+)") + "($|" + fileSep + ".*)";
                Pattern p = Pattern.compile(acceptNamePattern);
                for (Iterator iter = all.iterator(); iter.hasNext();) {
                    String path = (String) iter.next();
                    Matcher m = p.matcher(path);
                    if (m.matches()) {
                        String value = m.group(1);
                        ret.add(value);
                    }
                }
                LOGGER.debug("{} matched {}" + pattern, ret.size(), pattern);
                return ret;
            }
        }
        return Collections.emptyList();
    }

    private boolean tokenIsWholeDirectoryName(String pattern, String fileSep, String tokenString, int index) {
        return ((pattern.length() <= index + tokenString.length()) || fileSep.equals(pattern.substring(index + tokenString.length(), index + tokenString.length() + 1)))
                && (index == 0 || fileSep.equals(pattern.substring(index - 1, index)));
    }

    List<String> listAll(String parent) throws IOException {
        LOGGER.debug("using {} to list all in {}", repository, parent);
        List all = repository.list(parent);
        if (all != null) {
            LOGGER.debug("found {} resources", all.size());
            List<String> names = new ArrayList(all.size());
            for (Iterator iter = all.iterator(); iter.hasNext();) {
                names.add(getVersionFromPath((String) iter.next()));
            }
            return names;
        } else {
            LOGGER.debug("no resources found");
        }

        return Collections.emptyList();
    }

    public String getVersionFromPath(String path) {
        final String fileSeparator = repository.getFileSeparator();
        if (path.endsWith(fileSeparator)) {
            path = path.substring(0, path.length() - 1);
        }
        int slashIndex = path.lastIndexOf(fileSeparator);
        return path.substring(slashIndex + 1);
    }
}
