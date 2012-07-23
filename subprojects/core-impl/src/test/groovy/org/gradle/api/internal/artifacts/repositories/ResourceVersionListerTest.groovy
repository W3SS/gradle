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

package org.gradle.api.internal.artifacts.repositories

import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.core.module.id.ModuleRevisionId
import spock.lang.Specification
import static org.junit.Assert.*

class ResourceVersionListerTest extends Specification {

    def repo = Mock(ExternalResourceRepository)
    def artifact = Mock(Artifact)
    def moduleRevisionId = ModuleRevisionId.newInstance("org.acme", "testproject", "1.0")


    def setup() {
        1 * repo.getFileSeparator() >> "/"
    }

    def "getVersionList returns NULL when IOException occures"() {
        setup:
        def testPattern = "/a/pattern/with/[revision]/"
        1 * repo.list(_) >> { throw new IOException("Test IO Exception") }
        1 * repo.standardize(testPattern) >> testPattern
        def lister = new ResourceVersionLister(repo)
        expect:
        assertNull lister.getVersionList(moduleRevisionId, testPattern, artifact)
    }

    def "pattern without revision returns empty list"() {
        setup:
        def testPattern = "/some/pattern/without/rev"
        1 * repo.standardize(testPattern) >> testPattern
        def lister = new ResourceVersionLister(repo)
        expect:
        lister.getVersionList(moduleRevisionId, testPattern, artifact).versionStrings == []
    }

    def "getVersionList resolves versions from pattern with custom version directory name"() {
        setup:
        def testPattern = "/some/version-[revision]/somethingelse"
        1 * repo.list("/some") >> ["/some/version-1.5", "/some/shouldnotmatch-1.5", "/some/justanotherdirectory"]
        1 * repo.standardize(testPattern) >> testPattern
        def lister = new ResourceVersionLister(repo)
        when:
        def versionList = lister.getVersionList(moduleRevisionId, testPattern, artifact)
        then:
        versionList.versionStrings == ["1.5"]
    }
}
