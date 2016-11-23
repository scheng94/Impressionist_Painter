package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    float oldx, oldy, startTime;
    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        //TODO
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
        invalidate();
    }

    public void savePainting(){
        try{
            MediaStore.Images.Media.insertImage(getContext().getContentResolver(), _offScreenBitmap, "picture1" ,"");
        } catch(Exception e){

        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //TODO
        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location


        //comments
        float touchX = motionEvent.getX();
        float touchY = motionEvent.getY();

        int currX = (int) touchX;
        int currY = (int) touchY;

        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                oldx = motionEvent.getX();
                oldy = motionEvent.getY();
                startTime = motionEvent.getEventTime();
                break;
            case MotionEvent.ACTION_MOVE:
                double distance = Math.sqrt(((currX-oldx) * (currX-oldx)) + ((currY-oldy) * (currY-oldy)));
                float endTime = motionEvent.getEventTime();
                float velocity = (float) distance/(endTime - startTime);
                float radius = ((95/6)*velocity) + _minBrushRadius;
                int historySize = motionEvent.getHistorySize();

                for(int i = 0; i < historySize; i++){
                    float X = motionEvent.getHistoricalX(i);
                    float Y = motionEvent.getHistoricalY(i);

                    try {
                        Bitmap imageViewBitmap = _imageView.getDrawingCache();
                        int color = imageViewBitmap.getPixel((int) X, (int) Y);
                        _paint.setColor(color);
                        _paint.setAlpha(_alpha);
                    } catch (Exception e){
                        return false;
                    }

                    if(_brushType == BrushType.Circle){
                        _offScreenCanvas.drawCircle(X, Y, radius, _paint);
                    } else if(_brushType == BrushType.Square){
                        _offScreenCanvas.drawRect(X, Y, X + radius, Y + radius, _paint);
                    } else if(_brushType == BrushType.CircleSplatter) {
                        for(int j = 0; j < 7; j++){
                            float randomX = (float) Math.random();
                            float randomY = (float) Math.random();
                            randomX *= (Math.random() > .5) ? 1: -1;
                            randomY *= (Math.random() > .5) ? 1: -1;
                            _offScreenCanvas.drawCircle(X + (randomX*radius), Y + (randomY*radius), radius, _paint);
                        }
                    } else if(_brushType == BrushType.Line){
                        _offScreenCanvas.drawLine(X, Y, X + radius, Y+radius,_paint);
                    } else if(_brushType == BrushType.LineSplatter) {
                        float randomX = (float) Math.random();
                        float randomY = (float) Math.random();
                        randomX *= (Math.random() > .5) ? 1: -1;
                        randomY *= (Math.random() > .5) ? 1: -1;
                        _offScreenCanvas.drawLine(X, Y, X + (randomX*radius), Y + (randomY*radius), _paint);
                    } else{
                        _offScreenCanvas.drawPoint(X, Y, _paint);
                    }

                }

                try {
                    Bitmap imageViewBitmap = _imageView.getDrawingCache();
                    int color = imageViewBitmap.getPixel(currX, currY);
                    _paint.setColor(color);
                    _paint.setAlpha(_alpha);
                } catch (Exception e){
                    return false;
                }
                if(_brushType == BrushType.Circle){
                    _offScreenCanvas.drawCircle(currX, currY, radius, _paint);
                } else if(_brushType == BrushType.Square){
                    _offScreenCanvas.drawRect(currX, currY, currX + radius, currY + radius, _paint);
                } else if(_brushType == BrushType.CircleSplatter) {
                    for(int j = 0; j < 7; j++){
                        float randomX = (float) Math.random();
                        float randomY = (float) Math.random();
                        randomX *= (Math.random() > .5) ? 1: -1;
                        randomY *= (Math.random() > .5) ? 1: -1;
                        _offScreenCanvas.drawCircle(currX + (randomX*radius), currY + (randomY*radius), radius, _paint);
                    }
                }  else if(_brushType == BrushType.Line) {
                    _offScreenCanvas.drawLine(currX, currY, currX+radius, currY+radius, _paint);
                } else if(_brushType == BrushType.LineSplatter) {
                    float randomX = (float) Math.random();
                    float randomY = (float) Math.random();
                    randomX *= (Math.random() > .5) ? 1: -1;
                    randomY *= (Math.random() > .5) ? 1: -1;
                    _offScreenCanvas.drawLine(currX, currY, currX + (randomX*radius), currY + (randomY*radius), _paint);
                } else{
                    _offScreenCanvas.drawPoint(currX, currY, _paint);
                }
                oldx = touchX;
                oldy = touchY;
                startTime = motionEvent.getEventTime();
                invalidate();

                break;
        }
        return true;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

