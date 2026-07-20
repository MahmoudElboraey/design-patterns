package com.learn.stockpricealert.before;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlertSystem {

    private List<Stock> stocks;

    private List<User> users;

    public AlertSystem(){
        stocks = new ArrayList<>();
        users = new ArrayList<>();
    }



    public void addStock(Stock stock) {
        stocks.add(stock);
    }

    public void addUser(User user) {
        users.add(user);
    }

    public void updateStockPrice(Stock stock) {

        for (Stock existingStock : stocks) {
            if (matches(existingStock, stock)) {
                System.out.println("Updating stock price for stock: " + stock);
                notifyUsers(stock.getName() , existingStock.getPrice().getValue() ,  stock.getPrice().getValue());

                existingStock.getPrice().setValue(stock.getPrice().getValue());
                // stock price got updated okay
                // find all the subscribed users and choose the affected ones and then notify them
                break;
            }
        }
    }

    private boolean matches(Stock existingStock, Stock stock) {
        return existingStock.getName().equals(stock.getName())
                && existingStock.getPrice().getSymbol().equals(stock.getPrice().getSymbol());
    }

    public void notifyUsers(String stockName , double oldPrice ,  double newPrice) {
       // logic related to
        // - notifying users by email
        // - notify users by smms
        // - notifying users in-app

        users.forEach(user -> {
            user.getSubscriptions().forEach(subscription -> {

                if (subscription.getStockName().equals(stockName) && subscription.getRule().equals(Rule.ABOVE) && subscription.getRangeValue() < newPrice) {
                    // notification part
                    System.out.println(" yoo " + user.getName() + " notifying stock " + stockName + " for " + subscription.getRule() + " through " + subscription.getChannel() );
                }

                if (subscription.getStockName().equals(stockName) && subscription.getRule().equals(Rule.BELOW) && subscription.getRangeValue() > newPrice) {
                    // notif him
                    System.out.println(" yoo " + user.getName() + " notifying stock " + stockName + " for " + subscription.getRule() + " through " + subscription.getChannel() );
                }

                if (subscription.getStockName().equals(stockName) &&  subscription.getRule().equals(Rule.ANY_CHANGE) && oldPrice != newPrice){
                    // notify
                    System.out.println(" yoo " + user.getName() + " notifying stock " + stockName + " for " + subscription.getRule() + " through " + subscription.getChannel() );
                }
            });
        });
    }



}
