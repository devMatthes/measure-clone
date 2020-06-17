package com.example.arcore_measure;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MeasurementListAdapter extends RecyclerView.Adapter<MeasurementListAdapter.ViewHolder> {

    private static final String TAG = "MeasurementListAdapter";

    private final LayoutInflater inflater;
    private  List<Measurements> measurements;


    public MeasurementListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.recyclerview_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: called");
        if(measurements != null){
            Measurements current = measurements.get(position);
            holder.surfName.setText(current.getNameOfSurface());
            holder.surface.setText(current.getSurfaceStr()+ " cm");
        }
        else{
            holder.surfName.setText("No name");
            holder.surface.setText("No measure");
        }

    }

    void setMeasurements(List<Measurements> measurementsG){
        measurements = measurementsG;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if(measurements != null){
            return measurements.size();
        }
        else{
            return 0;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView surfName;
        TextView surface;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            surfName = (TextView) itemView.findViewById(R.id.measureName);
            surface = (TextView) itemView.findViewById(R.id.measureValue);
        }
    }


}
