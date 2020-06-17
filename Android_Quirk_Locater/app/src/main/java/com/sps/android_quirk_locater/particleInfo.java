package com.sps.android_quirk_locater;

import android.graphics.drawable.ShapeDrawable;

import java.util.ArrayList;

public class particleInfo {
    private ShapeDrawable shape;
    private ArrayList<Integer> cell;

    public particleInfo(ShapeDrawable coordinates, ArrayList<Integer> cell) {
        this.shape = coordinates;
        this.cell = cell;
    }

    public particleInfo(ShapeDrawable coordinates) {
        this.shape = coordinates;
        this.cell = new ArrayList<>();
    }

    public ShapeDrawable getShape() {
        return this.shape;
    }

    public ArrayList<Integer> getCell() {
        return this.cell;
    }

    public void setShape(ShapeDrawable shape) {
        this.shape = shape;
    }

    public void setCell(ArrayList<Integer> cell) {
        this.cell = cell;
    }
}
