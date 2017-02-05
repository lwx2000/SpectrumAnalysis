package com.github.lwx2000.spectrumanalysis;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.icu.text.AlphabeticIndex;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by LX on 2016/12/27.
 */

public class ColourMatchingFunction
{
    double  x, y, z;  // 0.0 ~ 1.0
    double  r, g, b;  // 0 ~ 1
    int     R, G, B;  // 0 ~ 255
    int     wavelen = 0;

    static ColourMatchingFunction FindRecord(List<ColourMatchingFunction> recs, int R, int G, int B)
    {
        ColourMatchingFunction rec = null, reci;
        int     t2, tolerance2 = (255 * 255) * 3;

        for (int i = 0; i < recs.size(); i++)
        {
            // 寻找最合适的波长：距离最短（将来改进：用最小二乘法）
            reci = recs.get(i);
            t2 = (reci.R - R) * (reci.R - R) + (reci.G - G) * (reci.G - G) + (reci.B - B) * (reci.B - B);
            if (t2 < tolerance2)
            {
                tolerance2 = t2;
                rec = reci;
            }
        }

        return rec;
    }

    // Colour Matching Functions（波长与CIE XYZ颜色的映射表，可输出为XML、CSV、表格、图形等格式）
    // http://cvrl.ioo.ucl.ac.uk/cmfs.htm
    //static List<ColourMatchingFunction> GetRecords(XmlResourceParser parser, int start_wavelen, int end_wavelen)
    static List<ColourMatchingFunction> GetRecords(Context context, int start_wavelen, int end_wavelen)
    {
        List<ColourMatchingFunction> records = new LinkedList<>();
        ColourMatchingFunction rec = null;
        XmlResourceParser parser = context.getResources().getXml(com.github.lwx2000.spectrumanalysis.R.xml.lin2012xyz10e_1_7sf);

        try
        {
            int event = parser.getEventType();

            while (event != XmlResourceParser.END_DOCUMENT)
            {
                switch (event)
                {
                    case XmlResourceParser.START_TAG:  // 开始元素事件
                        String name = parser.getName();
                        if (name.equals("Record"))
                        {
                            rec = new ColourMatchingFunction();
                        }
                        else if (name.equals("Field1"))
                        {
                            event = parser.next();
                            rec.wavelen = Integer.parseInt(parser.getText());
                        }
                        else if (name.equals("Field2"))
                        {
                            event = parser.next();
                            rec.x = Double.parseDouble(parser.getText());
                        }
                        else if (name.equals("Field3"))
                        {
                            event = parser.next();
                            rec.y = Double.parseDouble(parser.getText());
                        }
                        else if (name.equals("Field4"))
                        {
                            event = parser.next();
                            rec.z = Double.parseDouble(parser.getText());
                        }
                        break;

                    case XmlResourceParser.END_TAG:  // 结束元素事件
                        if (parser.getName().equals("Record"))
                        {
                            if ((rec.wavelen >= start_wavelen) && (rec.wavelen <= end_wavelen))
                                records.add(rec);
                        }
                        break;
                }

                event = parser.next();
            }
        }
        catch(XmlPullParserException e)
        {
        }
        catch (IOException e)
        {
        }

        // XYZ to RGB(sRGB color space?)
        // https://www.cs.rit.edu/~ncs/color/t_spectr.html
        // 更精确的计算：http://stackoverflow.com/questions/1472514/convert-light-frequency-to-rgb
        final double xyz2rgb_mat[][] = {{ 3.240479, -1.537150, -0.498535},
                                         {-0.969256,  1.875992,  0.041556},
                                         { 0.055648, -0.204043,  1.057311}};

        for (int i = 0; i < records.size(); i++)
        {
            rec = records.get(i);

            rec.r = xyz2rgb_mat[0][0] * rec.x + xyz2rgb_mat[0][1] * rec.y + xyz2rgb_mat[0][2] * rec.z;
            rec.g = xyz2rgb_mat[1][0] * rec.x + xyz2rgb_mat[1][1] * rec.y + xyz2rgb_mat[1][2] * rec.z;
            rec.b = xyz2rgb_mat[2][0] * rec.x + xyz2rgb_mat[2][1] * rec.y + xyz2rgb_mat[2][2] * rec.z;

            rec.R = (int)(255 * rec.r);
            rec.G = (int)(255 * rec.g);
            rec.B = (int)(255 * rec.b);
        }

        return records;
    }
}
