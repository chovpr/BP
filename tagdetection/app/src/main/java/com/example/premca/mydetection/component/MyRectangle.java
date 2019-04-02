/*
 * Copyright 2019 Přemysl Chovaneček. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.premca.mydetection.component;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import com.example.premca.mydetection.activity.GalleryActivity;
import com.example.premca.mydetection.fragment.CameraFragment;
import com.example.premca.mydetection.model.DetectionPosition;

/*
 * This class is for drawing squares on the view of the photo.
 */

public class MyRectangle extends View {
    private Paint paint;
    private DetectionPosition position;
    // set the rectangle params.
    public MyRectangle(Context context) {
        super(context);
        paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

    }

    public void setPosition(DetectionPosition position) {
        this.position = position;
    }

    @Override
    public void onDraw(Canvas canvas) {

        // Scaling rectangle for different display sizes.
        canvas.scale(-1, 1, GalleryActivity.PlaceholderFragment.fragWidth/2,GalleryActivity.PlaceholderFragment.fragHeight/2);
        double q2 = ((double)GalleryActivity.PlaceholderFragment.fragHeight)/((double)CameraFragment.usedHeight);
        double q1 = ((double)GalleryActivity.PlaceholderFragment.fragWidth)/((double)CameraFragment.usedWidth);
        canvas.scale((float)q1,(float)q2);
        // Drawing final rect
        canvas.drawRect(position.getTop(), position.getLeft(), position.getBottom(), position.getRight(),paint);
    }
}



