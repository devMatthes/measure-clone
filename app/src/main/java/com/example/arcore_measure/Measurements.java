package com.example.arcore_measure;


import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "measurements_table")
public class Measurements {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    private String nameOfSurface;

    @NonNull
    private Float surface;

    public Measurements(String nameOfSurface, Float surface)
    {
        //this.id = id;
        this.nameOfSurface = nameOfSurface;
        this.surface = surface;
    }

    @NonNull
    public String getNameOfSurface()
    {
        return this.nameOfSurface;
    }

    public int getId()
    {
        return this.id;
    }

    public String getSurfaceStr()
    {
        return Float.toString(this.surface);
    }

    public Float getSurface()
    {
        return surface;
    }
}

