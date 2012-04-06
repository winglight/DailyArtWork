package net.yihabits.artwork;

import java.io.InputStream;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.widget.ImageView;

public class GIFView extends ImageView {  
    private static final String TAG="GIFView";  
     
    private Movie mMovie;     
    private long mMovieStart;  
     
    public GIFView(Context context) {
//		super(context);
		this(context, null, 0);
	}
    
    public GIFView(Context context, AttributeSet attrs) {
//		super(context, attrs);
		this(context, attrs, 0);
	}
    
    //主要的构造函数  
    public GIFView(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
        // TODO Auto-generated constructor stub  
         
        mMovie=null;  
        mMovieStart=0;  
         
        //从描述文件中读出gif的值，创建出Movie实例  
        TypedArray a = context.obtainStyledAttributes(attrs,  
                R.styleable.GIFView, defStyle, 0);  
         
        int srcID=a.getResourceId(R.styleable.GIFView_gif, 0);  
        if(srcID>0){  
            InputStream is = context.getResources().openRawResource(srcID);  
            mMovie = Movie.decodeStream(is);  
        }  
         
        a.recycle();  
    }  
    //主要的工作是重载onDraw  
    @Override  
    protected void onDraw(Canvas canvas) {  
        // TODO Auto-generated method stub  
        //super.onDraw(canvas);  
         
        //当前时间  
        long now = android.os.SystemClock.uptimeMillis();  
        //如果第一帧，记录起始时间  
        if (mMovieStart == 0) {   // first time  
              mMovieStart = now;  
        }  
        if (mMovie != null) {  
                  //取出动画的时长  
            int dur = mMovie.duration();  
                  if (dur == 0) {  
                      dur = 1000;  
                  }  
                  //算出需要显示第几帧  
            int relTime = (int)((now - mMovieStart) % dur);  
           
                  //Log.d(TAG,"---onDraw..."+mMovie.toString()+",,,,"+relTime);  
             //设置要显示的帧，绘制即可  
                  mMovie.setTime(relTime);  
            mMovie.draw(canvas,0,0);  
                  invalidate();  
        }         
    }  
         
}  