/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.emitter;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ReactiveMessagingAndStreamsOperatorsTestCase {
    @ArquillianResource
    URL url;

    @Deployment(testable = false)
    public static Archive<?> getDeployment(){
        final WebArchive war = create(WebArchive.class, "messaging-rso.war");
        war.addPackage(ReactiveMessagingAndStreamsOperatorsTestCase.class.getPackage());
        war.setWebXML(ReactiveMessagingAndStreamsOperatorsTestCase.class.getPackage(), "web.xml");

        return war;
    }

    @Test
    public void testPublishMessages() throws Exception {
        pushValue("One");
        pushValue("Two");
        pushValue("Three");

        String values = readValues();
        Assert.assertEquals("One;Two;Three;", values);

        pushValue("Four");

        values = readValues();
        Assert.assertEquals("One;Two;Three;Four;", values);
    }

    private String pushValue(String value) throws Exception {
        URL url = new URL(this.url.toExternalForm() + "push/" + value);
        return HttpRequest.post(url.toExternalForm(), "",10, TimeUnit.SECONDS);
    }

    private String readValues() throws Exception {
        URL url = new URL(this.url.toExternalForm() + "values");
        return HttpRequest.get(url.toExternalForm(),10, TimeUnit.SECONDS);
    }
}
