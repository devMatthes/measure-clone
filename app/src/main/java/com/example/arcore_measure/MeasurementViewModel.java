package com.example.arcore_measure;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class MeasurementViewModel extends AndroidViewModel {

    private MeasurementRepository repository;

    private LiveData<List<Measurements>> allMeasurements;

    public MeasurementViewModel(@NonNull Application application) {
        super(application);
        repository = new MeasurementRepository(application);
        allMeasurements = repository.getAllMeasurements();
    }

    LiveData<List<Measurements>> getAllMeasurements() {
        return allMeasurements;
    }

    public void insert(Measurements measurement){
        repository.insert(measurement);
    }

    public void clear() { repository.clear(); }
}