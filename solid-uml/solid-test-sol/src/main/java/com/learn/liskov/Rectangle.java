package com.learn.liskov;

import java.awt.*;

public class Rectangle implements Shape {
    private int width;
    private int height;
    public void setWidth(int w)  { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() {
        return this.width * this.height;
    }
}
