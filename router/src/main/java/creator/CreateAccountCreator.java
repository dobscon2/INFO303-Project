/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package creator;

import domain.Account;
import domain.Customer;

/**
 *
 * @author cdobson
 */
public class CreateAccountCreator {
    
    public Account createAccount(Customer customer) {
        Account account = new Account();
        account.setId(customer.getId());
        account.setUsername(customer.getCustomerCode());
        account.setFirstName(customer.getFirstName());
        account.setLastName(customer.getLastName());
        account.setGroup(customer.getGroup());
        account.setEmail(customer.getEmail());
        
        return account;
    }
    
}
