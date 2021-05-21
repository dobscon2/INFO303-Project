/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package creator;

import domain.Summary;

/**
 *
 * @author cdobson
 */
public class ChangeGroup {
    
    public Summary changeGroup(Summary summary) {
        Summary newSummary = new Summary();
        newSummary.setNumberOfSales(summary.getNumberOfSales());
        newSummary.setTotalPayment(summary.getTotalPayment());
        if (summary.getGroup().equals("Regular Customer")) {
            newSummary.setGroup("0afa8de1-147c-11e8-edec-2b197906d816");
        } else {
            newSummary.setGroup("0afa8de1-147c-11e8-edec-201e0f00872c");
        }
        
        return newSummary;
        
    }
}
