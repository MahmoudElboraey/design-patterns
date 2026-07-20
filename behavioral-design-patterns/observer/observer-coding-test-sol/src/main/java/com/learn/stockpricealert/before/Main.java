package com.learn.stockpricealert.before;

public class Main {

    public static void main(String[] args) {

        AlertSystem alertSystem = new AlertSystem();
        Price TslaPrice = new Price("$" , 300.0);

        Price ApplePrice = new Price("$" , 500.0);


        Stock TSLA = new Stock("TSLA" , TslaPrice);

        Stock APPLE = new Stock("APPLE" , ApplePrice);

        alertSystem.addStock(TSLA);
        alertSystem.addStock(APPLE);


        User mahmoud = new User("mahmod" , "mahmoud@gmial.com" , "02020343");
        alertSystem.addUser(mahmoud);


        User ahmed = new User("ahmed" , "ahmed@gmail.com" , "0210343");
        alertSystem.addUser(ahmed);

        mahmoud.addSubscription(new Subscription(Rule.ABOVE , 1000 , Channel.EMAIL , "APPLE" ));
        ahmed.addSubscription(new Subscription(Rule.BELOW , 200  , Channel.SMS , "TSLA" ));

        alertSystem.updateStockPrice(new Stock("TSLA" , new Price("$" , 100.0)));
        alertSystem.updateStockPrice(new Stock("APPLE" , new Price("$" , 1100.0)));



    }
}
