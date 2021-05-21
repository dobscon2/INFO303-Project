/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package builder;

import creator.CreateAccountCreator;
import creator.CreateCustomerCreator;
import domain.Account;
import domain.Customer;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

/**
 *
 * @author cdobson
 */
public class CustomerBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("jetty:http://localhost:9000/createaccount?enableCORS=true")
                .setExchangePattern(ExchangePattern.InOnly)
                .log("Account created: ${body}")
                .unmarshal().json(JsonLibrary.Gson, Account.class)
                .log("Send to create-account queue: ${body}")
                .to("jms:queue:create-account");

        from("jms:queue:create-account")
                .bean(CreateCustomerCreator.class, "createCustomer({body})")
                .to("jms:queue:send-to-vend");

        from("jms:queue:send-to-vend")
                .log("Received Customer pre-marshal: ${body}")
                .removeHeaders("*")
                .setHeader("Authorization", constant("Bearer KiQSsELLtocyS2WDN5w5s_jYaBpXa0h2ex1mep1a"))
                .marshal().json(JsonLibrary.Gson)
                .setHeader(Exchange.CONTENT_TYPE).constant("application/json")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("https://info303otago.vendhq.com/api/2.0/customers?throwExceptionOnFailure=false")
                .choice()
                .when().simple("${header.CamelHttpResponseCode} == '201'")
                .convertBodyTo(String.class)
                .to("jms:queue:vend-reponse")
                .otherwise()
                .log("ERROR RESPONSE ${header.CamelHttpResponseCode} ${body}")
                .convertBodyTo(String.class)
                .to("jms:queue:vend-error")
                .endChoice();
        
        from("jms:queue:vend-response")
                .setBody().jsonpath("$.data")
                .marshal().json(JsonLibrary.Gson)
                .unmarshal().json(JsonLibrary.Gson, Customer.class)
                .bean(CreateAccountCreator.class, "createAccount(${body})")
                .marshal().json(JsonLibrary.Gson)
                .to("jms:queue:account");
        
        from("jms:queue:account")
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("http://localhost:8086/api/account");
    }

}
