package com.github.lwx2000.spectrumanalysis;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Camera;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.Vector;

import static android.R.attr.bitmap;

public class MainActivity extends AppCompatActivity
{
    class RecValue
    {
        ColorMatchingFunction2 rec = null;
        float value = 0;  // 光强
    }

    // 蓝光波段440nm ~ 510nm
    public static final int WAVELEN_START = 380;    // 390;
    public static final int WAVELEN_END = 780;      // 830;
    public static final int WAVELEN_BLUE_START = 400; // 440
    public static final int WAVELEN_BLUE_END = 510;

    public static final int TAKE_PHOTO = 1;
    public static final int SETTINGS = 2;

    private String mImagePath;  // 照片文件路径
    private int mSamplingIntervalX = 20;  // 图片采样点间隔（横向）
    private int mSamplingIntervalY = 20;  // 图片采样点间隔（纵向）
    private boolean mShowOnlyBluewave = true;  // 仅显示蓝光波段的光谱
    List<ColorMatchingFunction2> mAllRecs = ColorMatchingFunction2.GetRecords(WAVELEN_START, WAVELEN_END);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 指定Camera应用程序捕获的大图存放在外部设备(SDCard)（参见http://blog.csdn.net/hai_qing_xu_kong/article/details/45696873）
        // 实测保存为/mnt/sdcard/temp.jpg
        mImagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp.jpg";

        // 拍照(测光)按钮
        final Button detect = (Button)findViewById(R.id.detect);
        detect.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // 使用MediaStore.ACTION_IMAGE_CAPTURE比"android.media.action.IMAGE_CAPTURE"更有利于未来的变化
                // 但MediaStore.ACTION_IMAGE_CAPTURE是程序指定文件名，所以调试时方便一点
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                File file = new File(mImagePath);
                Uri imageUri = Uri.fromFile(file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);  // 注意：在模拟器中运行，加了这句后，启动相机（startActivityForResult()）时，程序会出错退出
                if (intent.resolveActivity(getPackageManager()) != null)
                    startActivityForResult(intent, TAKE_PHOTO); // 启动相机程序
            }
        });

        // "光谱分析"按钮
        Button analyze = (Button)findViewById(R.id.analyze);
        analyze.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ImageView   imageview = (ImageView)findViewById(R.id.imageview);
                Canvas  canvas = new Canvas();
                Bitmap  spectrumBmp;
                Paint   paint = new Paint();
                final int   width = imageview.getWidth(), height = imageview.getHeight(), yGap = height / 10, xGap = width / 10;
                int     x, y, idx;

                spectrumBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                spectrumBmp.eraseColor(Color.WHITE);
                canvas.setBitmap(spectrumBmp);

                List<ColorMatchingFunction2> recs = new LinkedList<>();
                RecValue recsValue[] = null;

                for (idx = 0; idx < mAllRecs.size(); idx++)
                {
                    if (mShowOnlyBluewave && ((mAllRecs.get(idx).wavelen < WAVELEN_BLUE_START) || (mAllRecs.get(idx).wavelen > WAVELEN_BLUE_END)))
                        continue;
                    recs.add(mAllRecs.get(idx));
                }

                // 初始化recValues
                recsValue = new RecValue[recs.size()];
                for (int i = 0; i < recsValue.length; i++)
                {
                    recsValue[i] = new RecValue();
                    recsValue[i].rec = recs.get(i);
                }

                // 底部光谱条
                for (x = 0; x < width; x++)
                {
                    if ((idx = (int)(1.0 * x / width * recs.size())) < recs.size())
                    {
                        paint.setColor(Color.rgb(recs.get(idx).r, recs.get(idx).g, recs.get(idx).b));
                        canvas.drawLine(x, height, x, height - yGap * (float)0.4, paint);
                    }
                }

                // 波长刻度
                //paint.setTextSize(30);
                paint.setTextSize(yGap / 4);
                paint.setColor(Color.LTGRAY);
                for (x = 0; x < width; x += xGap)
                {
                    if ((idx = (int)(1.0 * x / width * recs.size())) < recs.size())
                    {
                        y = height - yGap * 3 / 4;
                        canvas.rotate(45, x, y);
                        canvas.drawText(Integer.toString(recs.get(idx).wavelen), x, y, paint);
                        canvas.rotate(-45, x, y);
                    }
                }

                // 网格线
                paint.setColor(Color.LTGRAY);
                for (y = height - yGap; y >= 0; y -= yGap)
                    canvas.drawLine(0, y, imageview.getWidth(), y, paint);  // 横线
                for (x = 0; x < width; x += xGap)
                    canvas.drawLine(x, height - yGap, x, 0, paint);  // 竖线

                // 分析并统计拍照图片的每个像素
                BitmapFactory.Options op = new BitmapFactory.Options();
                op.inJustDecodeBounds = false;
                op.inScaled = false;  // 不缩放，取真实大小

                // decodeFile()在真机中成功，但在模拟器中解码失败（所以下一句取资源中的图片，以便可以在模拟器中进行测试）
                Bitmap  cameraBmp = BitmapFactory.decodeFile(mImagePath, op);
                if (cameraBmp == null)
                    cameraBmp = BitmapFactory.decodeResource(MainActivity.this.getResources(), R.drawable.test_decodefile, op);

                if (cameraBmp != null)
                {
                    float maxValue = 0;
                    int[] px = new int[cameraBmp.getWidth() * cameraBmp.getHeight()];
                    cameraBmp.getPixels(px, 0, cameraBmp.getWidth(), 0, 0, cameraBmp.getWidth(), cameraBmp.getHeight());

                    for (idx = 0; idx < recsValue.length; idx++)
                        recsValue[idx].value = 0;
                    //for (int i = 0; i < cameraBmp.getWidth() * cameraBmp.getHeight(); i++)
                    for (int row = 0; row < cameraBmp.getHeight(); row += mSamplingIntervalX)
                    {
                        for (int col = 0; col < cameraBmp.getWidth(); col += mSamplingIntervalY)
                        {
                            idx = row * cameraBmp.getWidth() + col;
                            int r = Color.red(px[idx]);
                            int g = Color.green(px[idx]);
                            int b = Color.blue(px[idx]);
                            int a = Color.alpha(px[idx]);

                            ColorMatchingFunction2 rec = null;
                            if ((rec = ColorMatchingFunction2.FindRecord(mAllRecs, r, g, b)) != null)
                            {
                                if (mShowOnlyBluewave && ((rec.wavelen < WAVELEN_BLUE_START) || (rec.wavelen > WAVELEN_BLUE_END)))
                                    continue;

                                for (idx = 0; idx < recsValue.length; idx++)
                                {
                                    if (recsValue[idx].rec == rec)
                                    {
                                        float hsv[] = {0, 0, 0};

                                        Color.RGBToHSV(r, g, b, hsv);
                                        recsValue[idx].value += hsv[2];

                                        if (maxValue < recsValue[idx].value)
                                            maxValue = recsValue[idx].value;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // 将recsValue画到specturmBmp上
                    for (x = 0; x < width; x++)
                    {
                        if ((idx = (int)(1.0 * x / width * recs.size())) < recs.size())
                        {
                            paint.setColor(Color.rgb(recsValue[idx].rec.r, recsValue[idx].rec.g, recsValue[idx].rec.b));
                            int h = (int)((height - yGap) * recsValue[idx].value / maxValue * 0.9);  // 0.9：免得光谱线直达屏幕顶端
                            canvas.drawLine(x, height - yGap, x, height - yGap - h, paint);
                        }
                    }
                }

                // 显示spectrumBmp到imageview上
                imageview.setImageBitmap(spectrumBmp);
            }
        });

        // "数据报告"按钮
        Button save = (Button)findViewById(R.id.report);
        save.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        String str;
        switch (requestCode)
        {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK)
                {
                    ImageView   imageview = (ImageView)findViewById(R.id.imageview);
                    Bitmap      bitmap = null;

                    // 计算图像缩放比例，以便解码大图时缩放，节约手机内存
                    BitmapFactory.Options op = new BitmapFactory.Options();
                    op.inJustDecodeBounds = true;  // 通知BitmapFactory.decodeFile()只返回该图像的范围，无须解码图像本身
                    op.inScaled = false;
                    BitmapFactory.decodeFile(mImagePath, op);
                    op.inSampleSize = Math.min(op.outHeight / imageview.getHeight(), op.outWidth / imageview.getWidth());

                    // 解码大图并显示在imageview上
                    op.inJustDecodeBounds = false;
                    if ((bitmap = BitmapFactory.decodeFile(mImagePath, op)) != null)
                        imageview.setImageBitmap(bitmap);
                    else
                        imageview.setImageResource(R.drawable.cover);  // 因为在模拟器里decodeFile()失败，所以显示一幅图表示一下意思
                }
                break;
            case SETTINGS:
                if (resultCode == RESULT_OK)
                {
                    mSamplingIntervalX = data.getIntExtra("SamplingIntervalX", 20);
                    mSamplingIntervalY = data.getIntExtra("SamplingIntervalY", 20);
                    if ((mSamplingIntervalX <= 0) || (mSamplingIntervalX > 100))
                        mSamplingIntervalX = 20;
                    if ((mSamplingIntervalY <= 0) || (mSamplingIntervalY > 100))
                        mSamplingIntervalY = 20;

                    mShowOnlyBluewave = data.getBooleanExtra("ShowOnlyBluewave", false);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.putExtra("SamplingIntervalX", mSamplingIntervalX);
                intent.putExtra("SamplingIntervalY", mSamplingIntervalY);
                intent.putExtra("ShowOnlyBluewave", mShowOnlyBluewave);
                startActivityForResult(intent, SETTINGS);
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
