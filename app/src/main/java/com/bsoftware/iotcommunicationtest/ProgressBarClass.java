package com.bsoftware.iotcommunicationtest;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import java.util.zip.Inflater;

public class ProgressBarClass extends AsyncTask<Void,Void,Void> {

    private Context context;
    private AlertDialog progressDialog;

    public ProgressBarClass(Context context){
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        View view = LayoutInflater.from(context).inflate(R.layout.progress_dialog,null);
        builder.setView(view);
        progressDialog = builder.create();
        progressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        return null;
    }
}
