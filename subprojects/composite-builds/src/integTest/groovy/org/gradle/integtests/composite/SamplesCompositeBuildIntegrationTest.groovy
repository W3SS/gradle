/*
 * Copyright 2016 the original author or authors.
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


package org.gradle.integtests.composite

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.Sample
import org.gradle.integtests.fixtures.UsesSample
import org.junit.Rule

class SamplesCompositeBuildIntegrationTest extends AbstractIntegrationSpec {

    @Rule public final Sample sample = new Sample(temporaryFolder)

    @UsesSample('compositeBuilds/basic')
    def "can run app with command-line composite"() {
        when:
        executer.inDirectory(sample.dir.file("my-app")).withArguments("--include-build", "../my-utils")
        succeeds(':run')

        then:
        executed ":my-utils:number-utils:jar", ":my-utils:string-utils:jar", ":run"
        outputContains("The answer is 42")
    }

    @UsesSample('compositeBuilds/basic')
    def "can run app when modified to be a composite"() {
        when:
        sample.dir.file("my-app/settings.gradle") << """
    includeBuild '../my-utils'
"""
        executer.inDirectory(sample.dir.file("my-app"))
        succeeds(':run')

        then:
        executed ":my-utils:number-utils:jar", ":my-utils:string-utils:jar", ":run"
        outputContains("The answer is 42")
    }

    @UsesSample('compositeBuilds/basic')
    def "can run app when included in a composite"() {
        when:
        executer.inDirectory(sample.dir.file("composite"))
        succeeds(':run')

        then:
        executed ":my-utils:number-utils:jar", ":my-utils:string-utils:jar", ":my-app:run", ":run"
        outputContains("The answer is 42")
    }

    @UsesSample('compositeBuilds/hierarchical')
    def "can run app in hierarchical composite"() {
        when:
        executer.inDirectory(sample.dir.file("my-app"))
        succeeds(':run')

        then:
        executed ":number-utils:jar", ":string-utils:jar", ":run"
        outputContains("The answer is 42")
    }

    @UsesSample('compositeBuilds/hierarchical')
    def "can publish locally and remove submodule from hierarchical composite"() {
        when:
        executer.inDirectory(sample.dir.file("my-app"))
        succeeds(':publishDeps')

        then:
        executed ":number-utils:uploadArchives", ":string-utils:uploadArchives"
        sample.dir.file('local-repo/org.sample/number-utils/1.0').assertContainsDescendants("ivy-1.0.xml", "number-utils-1.0.jar")
        sample.dir.file('local-repo/org.sample/string-utils/1.0').assertContainsDescendants("ivy-1.0.xml", "string-utils-1.0.jar")

        when:
        sample.dir.file("my-app/submodules/string-utils").deleteDir()

        and:
        executer.inDirectory(sample.dir.file("my-app"))
        succeeds(":run")

        then:
        executed ":number-utils:jar", ":run"
        notExecuted ":string-utils:jar"
        outputContains("The answer is 42")
    }
}
