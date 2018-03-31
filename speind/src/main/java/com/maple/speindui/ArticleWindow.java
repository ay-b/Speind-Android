package com.maple.speindui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.maple.speind.R;
//import ru.lifenews.speind.R;

import java.util.Date;

import me.speind.SpeindAPI;

public class ArticleWindow extends DialogFragment {

    public interface OnDestroyViewListener {
        void onDestroyView();
    };

    private ActionBarActivity mActivity = null;
    private OnDestroyViewListener mOnDestroyListener= null;
    private SpeindAPI.InfoPoint mInfopoint = null;
    private String mProfile = "";
    private int listImageW = 0;

    public ArticleWindow(ActionBarActivity activity, SpeindAPI.InfoPoint infopoint, String profile) {
        mActivity = activity;
        mInfopoint = infopoint;
        mProfile = profile;
        if (mActivity!=null) {
            WindowManager windowManager = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(dm);
            listImageW = Math.min(dm.widthPixels, dm.heightPixels) - (int) (10 * ((float) dm.densityDpi / 160f));
        }
    }

    @Override
    public void onDestroyView() {
        if (mOnDestroyListener!=null) mOnDestroyListener.onDestroyView();
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View articleView=inflater.inflate(R.layout.speind_article_dialog, container, false);

        Toolbar toolbar = (Toolbar) articleView.findViewById(R.id.toolbar);
        if (toolbar!=null) {
            toolbar.setTitle("");
            if (mActivity!=null) mActivity.setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.btn_back);
            toolbar.setNavigationOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View arg0) {
                    ArticleWindow.this.dismiss();
                }
            });
        }
        TextView date=(TextView) articleView.findViewById(R.id.postDate);
        TextView  sender=(TextView) articleView.findViewById(R.id.postSender);
        TextView  text=(TextView) articleView.findViewById(R.id.postText);
        ImageView senderImage=(ImageView) articleView.findViewById(R.id.senderImage);
        ImageView imageView = (ImageView) articleView.findViewById(R.id.image);

        if (mInfopoint!=null) {
            SpeindAPI.InfoPointData data=mInfopoint.getData(mProfile);
            if (data!=null) {
                Bitmap bmp=mInfopoint.getPostBmp(mProfile, listImageW, -1);
                Bitmap senderBmp=mInfopoint.getSenderBmp(mProfile);
                if (senderBmp==null) {
                    if (!data.pluginBmpPath.equals("")) {
                        senderBmp = SpeindAPI.GetScaledBitmap(data.pluginBmpPath, 96, 96);
                    }
                }
                if (senderBmp!=null) {
                    int l = Math.min(senderBmp.getWidth(), senderBmp.getHeight());
                    Bitmap rounder = Bitmap.createBitmap(l, l, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(rounder);
                    Paint xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    xferPaint.setColor(Color.RED);
                    canvas.drawCircle(l / 2, l / 2, (l - 1) / 2, xferPaint); //drawRoundRect(new RectF(0,0,w,h), 20.0f, 20.0f, xferPaint);
                    xferPaint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));

                    Bitmap result = Bitmap.createBitmap(l, l, Bitmap.Config.ARGB_8888);
                    Canvas resultCanvas = new Canvas(result);
                    resultCanvas.drawBitmap(senderBmp, 0, 0, null);
                    resultCanvas.drawBitmap(rounder, 0, 0, xferPaint);
                    senderBmp=result;
                }
                if (imageView!=null) {
                    if (bmp!=null) {
                        imageView.setImageBitmap(bmp);
                        imageView.setVisibility(ImageView.VISIBLE);
                    } else {
                        imageView.setVisibility(ImageView.GONE);
                    }
                }
                if (date!=null) { date.setText(Speind.secoundsToTimeAgo(((new Date()).getTime() - mInfopoint.postTime.getTime())/1000));}
                if (sender!=null) { sender.setText(data.postSender); }
                if (text!=null) {
                    String dt=data.postTitle+"<br>"+data.postOriginText;
                    if (mInfopoint.articleExists) {
                        dt=data.postArticle;
                    }
                    text.setText(Html.fromHtml("<p></p>" + dt));
                    text.setMovementMethod(LinkMovementMethod.getInstance());
                }
                if (senderImage!=null) {
                    if (senderBmp!=null) {
                        senderImage.setImageBitmap(senderBmp);
                        senderImage.setVisibility(ImageView.VISIBLE);
                    } else {
                        senderImage.setVisibility(ImageView.GONE);
                    }
                }
            }

        }

        return articleView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    public void setOnOnDestroyListener(OnDestroyViewListener onDestroyViewListener) {
        mOnDestroyListener=onDestroyViewListener;
    }

}
