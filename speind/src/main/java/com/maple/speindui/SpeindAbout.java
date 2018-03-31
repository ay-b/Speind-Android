package com.maple.speindui;

import com.maple.speind.R;
//import ru.lifenews.speind.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.maple.speind.SpeindTaifuno;

public class SpeindAbout extends ActionBarActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.speind_about);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar!=null) {
	        toolbar.setTitle("");
	        setSupportActionBar(toolbar);
	        toolbar.setNavigationIcon(R.drawable.btn_back);
	        toolbar.setNavigationOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					SpeindAbout.this.finish();
				}				
			});
		}

		TextView write_mail_text=(TextView) findViewById(R.id.write_mail_text);
		if (write_mail_text!=null) {
            write_mail_text.setText(Html.fromHtml(getString(R.string.write_mail_text), null, null));
		}

        TextView open_chat_text=(TextView) findViewById(R.id.open_chat_text);
        if (open_chat_text!=null) {
            open_chat_text.setText(Html.fromHtml(getString(R.string.open_chat_text), null, null));
        }

		Button open_chat=(Button) findViewById(R.id.open_chat_button);
		Button write_mail=(Button) findViewById(R.id.write_mail_button);
		
		if (open_chat!=null) {
            open_chat.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {

                    Intent intent=new Intent(SpeindAbout.this, SpeindTaifuno.class);
                    SpeindAbout.this.startActivity(intent);
					
					SpeindAbout.this.finish();
				}
				
			});
		}
		if (write_mail!=null) {
            write_mail.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setType("plain/text");
					intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "feedback@speind.me" });
					intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feed_back_link));
					startActivity(Intent.createChooser(intent, getString(R.string.send_mail)));
					SpeindAbout.this.finish();										
				}				
			});
		} 			
	}

}
