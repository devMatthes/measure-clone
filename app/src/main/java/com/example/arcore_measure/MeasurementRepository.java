package com.example.arcore_measure;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class MeasurementRepository {

    private MeasurementsDao measurementsDao;
    private LiveData<List<Measurements>> allMeasurements;

    MeasurementRepository(Application application){
        MeasurementsRoomDatabase db = MeasurementsRoomDatabase.getDatabase(application);
        measurementsDao = db.measurementsDao();
        allMeasurements = measurementsDao.getSortedRecords();
    }

    LiveData<List<Measurements>> getAllMeasurements() {
        return allMeasurements;
    }

    void insert(Measurements measurement)
    {
        MeasurementsRoomDatabase.databaseWriteExecutor.execute(() -> {
            measurementsDao.insert(measurement);
        });
    }

    void clear() {
        MeasurementsRoomDatabase.databaseWriteExecutor.execute(() -> {
            measurementsDao.deleteAll();
        });
    }
}
