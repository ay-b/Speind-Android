package com.maple.speindui;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.maple.speind.R;
//import ru.lifenews.speind.R;

import me.speind.SpeindAPI;

public class SpeindPinboardSettings extends ActionBarActivity {
    public static String PREFS_NAME = "pinboard_settings";
    public static String EMAIL_NANE = "email";
    public static String FORMAT_NANE = "format";
    public static String DELETE_CONFIRM_NANE = "delete_confirm";
    public static String CLEAR_CONFIRM_NANE = "clear_confirm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speind_pinboard_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar!=null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.btn_back);
            toolbar.setNavigationOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View arg0) {
                    SpeindPinboardSettings.this.finish();
                }
            });
        }

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        final SharedPreferences.Editor editor = settings.edit();

        final EditText mail = (EditText) findViewById(R.id.mail);
        Spinner format = (Spinner) findViewById(R.id.mail_format);
        ToggleButton confirm_delete = (ToggleButton) findViewById(R.id.confirm_delete);
        ToggleButton confirm_clear = (ToggleButton) findViewById(R.id.confirm_clear);

        if (mail!=null) {
            final int standartColor = mail.getCurrentTextColor();
            mail.setText(settings.getString(EMAIL_NANE, ""));
            mail.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isValidEmail(mail.getText())) {
                        mail.setTextColor(standartColor);
                        editor.putString(EMAIL_NANE, mail.getText().toString());
                        editor.apply();
                    } else {
                        mail.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                }
            });
        }
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.mail_formats, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (format!=null) {
            format.setAdapter(adapter);
            format.setSelection(settings.getInt(FORMAT_NANE, SpeindAPI.SPEIND_EMAIL_FORMAT_HTML));
            format.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    editor.putInt(FORMAT_NANE, position);
                    editor.apply();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
        if (confirm_delete!=null) {
            confirm_delete.setChecked(settings.getBoolean(DELETE_CONFIRM_NANE, true));
            confirm_delete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    editor.putBoolean(DELETE_CONFIRM_NANE, isChecked);
                    editor.apply();
                }
            });
        }
        if (confirm_clear!=null) {
            confirm_clear.setChecked(settings.getBoolean(CLEAR_CONFIRM_NANE, true));
            confirm_clear.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    editor.putBoolean(CLEAR_CONFIRM_NANE, isChecked);
                    editor.apply();
                }
            });
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.pinboard_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    public final static boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

}
