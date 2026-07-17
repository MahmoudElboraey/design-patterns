package com.learn.interfacecsegregation;

public class MultiFunctionPrinter implements Printer , Scanner , Stapler{
    @Override
    public void print(Document d) {

    }

    @Override
    public void scan(Document d) {

    }

    @Override
    public void staple(Document d) {

    }
}
