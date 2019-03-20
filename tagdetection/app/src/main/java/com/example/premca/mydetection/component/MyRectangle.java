package com.example.premca.mydetection.component;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.example.premca.mydetection.activity.GalleryActivity;
import com.example.premca.mydetection.model.DetectionPosition;

public class MyRectangle extends View {
    private Paint paint;
    private DetectionPosition position;


    public MyRectangle(Context context) {
        super(context);
        paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);

    }

    public void setPosition(DetectionPosition position) {
        this.position = position;
    }

    @Override
    public void onDraw(Canvas canvas) {
        float deviceWidth;
        float deviceHeight;

        deviceWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        deviceHeight = getContext().getResources().getDisplayMetrics().heightPixels;

        float q1 = deviceWidth/480;
        float q2 = 1280/ 640;


        //RectF rect = new RectF(position.getLeft()*q1, position.getTop()*q2, position.getRight()*q1, position.getBottom()*q2);
        //RectF rect = new RectF(18*1.5f, 213*1.88f, 433*1.5f, 433*1.88f);
        canvas.scale(-1, 1, 480 / 2, 640/ 2);
        RectF rect = new RectF(position.getTop(), position.getLeft(), position.getBottom(), position.getRight());

        Log.e("KURVA0",String.valueOf(position.getRight()));
        Log.e("KURVA1",String.valueOf(position.getLeft()));
        Log.e("KURVA2",String.valueOf(position.getTop()));
        Log.e("KURVA3",String.valueOf(position.getBottom()));

        canvas.drawRect(rect, paint);
    }
}

