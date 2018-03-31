package com.maple.speindui;


import android.content.SharedPreferences;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.maple.speind.R;

public class DialogWindow extends DialogFragment {
	private int mlayoutId=0;
	private int pId=0;
	private View mView=null;
	private FragmentActivity mActivity;
	
	public DialogWindow(FragmentActivity activity, int parent, int content) {
		mActivity=activity;
		pId=parent;
		mlayoutId=content;
		LayoutInflater inflater = mActivity.getLayoutInflater();//(LayoutInflater) mActivity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mView = inflater.inflate(mlayoutId, null);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return mView;
	}

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }
	
	public View findViewById(int id) {
		if (mView!=null) 
			return mView.findViewById(id);
		else
			return null;
	}
	 
	public void show() {
		FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(pId, this);
        transaction.commit();
    }

	public void hide() {
		dismiss();
	}

	public static void showConfirmDialog(final FragmentActivity activity, int parent, int content, int messageId, final Button.OnClickListener confirm, final Button.OnClickListener cancel, final String keep_settingsName) {
		final DialogWindow cdw=new DialogWindow(activity, parent, content);
		Button okButton=(Button) cdw.findViewById(R.id.ok_button);
		Button cancelButton=(Button) cdw.findViewById(R.id.cancell_button);
		TextView message=(TextView) cdw.findViewById(R.id.message);
		RelativeLayout keep_wrap = (RelativeLayout) cdw.findViewById(R.id.keep_wrap);
		final ToggleButton keep = (ToggleButton) cdw.findViewById(R.id.keep);

		if (message!=null) {
			message.setText(activity.getString(messageId));
		}
		if (okButton!=null) {
			okButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (keep!=null&&keep.isChecked()) {
						SharedPreferences.Editor editor = activity.getSharedPreferences(SpeindPinboardSettings.PREFS_NAME, 0).edit();
						editor.putBoolean(keep_settingsName, false);
						editor.apply();
					}
					confirm.onClick(arg0);
					cdw.hide();
				}
			});
		}
		if (cancelButton!=null) {
			if (cancel!=null) {
				if (okButton!=null) okButton.setText(R.string.yes);
				cancelButton.setText(R.string.no);
			}
			cancelButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (cancel != null) cancel.onClick(arg0);
					cdw.hide();
				}
			});
		}

		if (keep_settingsName!=null&&!keep_settingsName.isEmpty()) {
			if (keep_wrap!=null) {
				keep_wrap.setVisibility(View.VISIBLE);
			}
		}
		cdw.show();
	}

}

