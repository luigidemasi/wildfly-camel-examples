/*
 * #%L
 * Wildfly Camel :: Example :: Camel ActiveMQ
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
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
 * #L%
 */
package org.wildfly.camel.examples.activemq;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;

@ApplicationScoped
@ContextName("camel-activemq-context")
public class ActiveMQRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        /**
         * This route generates a random order every 5 seconds
         */
        from("timer:order?period=5s&delay=0")
        .bean("orderGenerator", "generateOrder")
        .setHeader(Exchange.FILE_NAME).method("orderGenerator", "generateFileName")
        .to("file://{{jboss.server.data.dir}}/orders");

        /**
         * This route reads files placed within JBOSS_HOME/standalone/data/orders
         * and sends them to ActiveMQ queue 'ordersQueue'
         */
        from("file://{{jboss.server.data.dir}}/orders")
        .convertBodyTo(String.class)
        // Remove headers to ensure we end up with unique file names being generated in the next route
        .removeHeaders("*")
        .to("activemq:queue:OrdersQueue");

        /**
         * This route consumes messages from the 'ordersQueue'. Then, based on the
         * message payload XML content it uses a content based router to output
         * orders into appropriate country directories
         */
        from("activemq:queue:OrdersQueue")
            .choice()
                .when(xpath("/order/customer/country = 'UK'"))
                    .log("Sending order to the UK")
                    .to("file:{{jboss.server.data.dir}}/orders/processed/UK")
                .when(xpath("/order/customer/country = 'US'"))
                    .log("Sending order to the US")
                    .to("file:{{jboss.server.data.dir}}/orders/processed/US")
                .otherwise()
                    .log("Sending order to another country")
                    .to("file://{{jboss.server.data.dir}}/orders/processed/Other");
    }
}
