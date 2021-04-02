package com.example.bluetootharduino;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ListOfCommands extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_commands);

        String[] commands = {"[ - char for stepper enabling", "\\ - char for buffer reset",
                "] - char for stepper disabling", "^ - char for buffer filling",
                "_ - char for sending actual position", "` - char for sending signature",
                "h - char for home all axes", "p - char for program pause", "# - absolute position"};

        ListAdapter commandAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, commands);
        ListView commandList = (ListView)findViewById(R.id.commandList);
        commandList.setAdapter(commandAdapter);
    }
}