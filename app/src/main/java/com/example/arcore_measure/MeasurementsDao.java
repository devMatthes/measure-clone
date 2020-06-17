package com.example.arcore_measure;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MeasurementsDao {

    // allowing the insert of the same record multiple times by passing a
    // conflict resolution strategy
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Measurements newMeasure);

    @Query("DELETE FROM measurements_table")
    void deleteAll();

    @Query("SELECT * from measurements_table ORDER BY id ASC")
    LiveData<List<Measurements>> getSortedRecords();


}

