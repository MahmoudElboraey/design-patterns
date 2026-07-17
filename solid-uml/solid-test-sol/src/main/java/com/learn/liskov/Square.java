package com.learn.liskov;

import java.awt.*;

public class Square implements Shape{
    private  int side;
    @Override
    public int area() {
       return side*side;
    }

    public void setSide(int side) {
        this.side = side;
    }
}
