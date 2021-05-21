/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package builder;

import creator.ChangeGroup;
import creator.CreateAccountCreator;
import creator.UpdateCustomerCreator;
import domain.Sale;
import domain.Summary;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

/**
 *
 * @author cdobson
 */
public class SalesBuilder extends RouteBuilder{

    @Override
    public void configure() throws Exception {
        from("jms:queue:new-sale")
                .unmarshal().json(JsonLibrary.Gson, Sale.class)
                .log("Formatted sale: ${body}")
                .setProperty("Customer_ID").simple("${body.customer.id}")
                .setProperty("Customer_Group").simple("${body.customer.group}")
                .setProperty("Customer_Email").simple("${body.customer.email}")
                .setProperty("Customer_FirstName").simple("${body.customer.firstName}")
                .setProperty("Customer_LastName").simple("${body.customer.lastName}")
                .to("jms:queue:sale-data");
        
        from("jms:queue:sale-data")
                .removeHeaders("*")
                .marshal().json(JsonLibrary.Gson)
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("http://localhost:8081/api/sales")
                .to("jms:queue:get-sale-summary");
        
        from("jms:queue:get-sale-summary")
                .removeHeaders("*")
                .setBody(constant(null))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .recipientList().simple("http://localhost:8081/api/sales/customer/${exchangeProperty.Customer_ID}/summary")
                .to("jms:queue:sale-summary-response");
        
        from("jms:queue:sale-summary-response")
                .unmarshal().json(JsonLibrary.Gson, Summary.class)
                .bean(ChangeGroup.class, "changeGroup(${body})")
                .choice()
                .when().simple("${body.group} == ${exchangeProperty.Customer_Group}")
                .to("jms:queue:sale-group-unchanged")
                .otherwise()
                .to("jms:queue:sale-group-updated");
        
        from("jms:queue:sale-group-updated")
                .bean(UpdateCustomerCreator.class, "updateGroup(${exchangeProperty.Customer_ID},"
                        + "${exchangeProperty.Customer_FirstName},"
                        + "${exchangeProperty.Customer_LastName}"
                        + "${exchangeProperty.Customer_Email})")
                .multicast().to("jms:queue:to-vend", "jms:queue:to-account-service");
                
        from("jms:queue:to-vend")
                .removeHeader("*")
                .setHeader("Authorization", constant("Bearer KiQSsELLtocyS2WDN5w5s_jYaBpXa0h2ex1mep1a"))
                .marshal().json(JsonLibrary.Gson)
                .setHeader(Exchange.CONTENT_TYPE).constant("application/json")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .recipientList().simple("https://info303otago.vendhq.com/api/2.0/customers/${exchangeProperty.Customer_ID}")
                .to("jms:queue:vend-updated");
        
        from("jms:queue:to-account-service")
                .log("account body: ${body}")
                .bean(CreateAccountCreator.class, "createAccount(${body})")
                .marshal().json(JsonLibrary.Gson)
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .recipientList().simple("http://localhost:8086/api/accounts/account/${exchangeProperty.Customer_ID}")
                .to("jms:queue:account-service-response");
    }
    
}
