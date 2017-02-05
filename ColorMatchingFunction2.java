package com.github.lwx2000.spectrumanalysis;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by LX on 2017/1/7.
 * 参考资料：Simple Analytic Approximations to the CIE XYZ Color Matching Functions
 * http://stackoverflow.com/questions/1472514/convert-light-frequency-to-rgb/34581745#34581745
 * http://jcgt.org/published/0002/02/01/paper.pdf
 * 本地磁盘：蓝光资料\Simple Analytic Approximations to the CIE XYZ Color Matching Functions.pdf
 */

// 波长与RGB之间的映射表
public class ColorMatchingFunction2
{
    static int mStep = 1;  // GetRecords()中使用的波长间隔(nm)：缺省1，如果计算速度太慢或精度要求不高，可设为5
    int wavelen = 0;
    int c = 0;  // RGB
    int r, g, b;

    // 用rgb比较查找：这个函数的缺陷是RGB各分量的影响分不清，以致于(255,0,0)和(0,0,255)返回同一条记录
    static ColorMatchingFunction2 FindRecord(List<ColorMatchingFunction2> recs, int r, int g, int b)
    {
        ColorMatchingFunction2 rec = null, reci;
        int tolerance2 = 0xFF * 0xFF * 3;

        for (int i = 0; i < recs.size(); i++)
        {
            reci = recs.get(i);
            int rgb[] = splitRGB(reci.c);
            int t2 = ((rgb[0] - r) * (rgb[0] - r) + (rgb[0] - g) * (rgb[0] - g) + (rgb[0] - b) * (rgb[0] - b));

            if (t2 == 0)  // 完美匹配
            {
                return reci;
            }
            else if (t2 <= tolerance2)  // 最佳匹配
            {
                tolerance2 = t2;
                rec = reci;
            }
        }

        return rec;
    }

    // 用rgb合并后的c来比较查找：这个函数的缺陷是R的影响太大，以致于(255,255,255)和(255,0,0)返回同一条记录
    static ColorMatchingFunction2 FindRecord(List<ColorMatchingFunction2> recs, int c)
    {
        ColorMatchingFunction2 rec = null, reci;
        int     t2, tolerance2 = 0xFFFFFF;

        for (int i = 0; i < recs.size(); i++)
        {
            // 寻找最合适的波长：距离最短（将来改进：用二分法）
            reci = recs.get(i);
            t2 = Math.abs(reci.c - c);
            if (t2 == 0)
            {
                return reci;
            }
            else if (t2 < tolerance2)
            {
                tolerance2 = t2;
                rec = reci;
            }
        }

        return rec;
    }

    public static List<ColorMatchingFunction2> GetRecords(int start_wavelen, int end_wavelen)
    {
        List<ColorMatchingFunction2> records = new LinkedList<>();
        ColorMatchingFunction2 rec = null;

        for (int wavelen = start_wavelen; wavelen <= end_wavelen; wavelen += mStep)
        {
            rec = new ColorMatchingFunction2();
            rec.wavelen = wavelen;
            rec.c = ColorMatchingFunction2.wavelengthToRGB(wavelen);
            rec.r = (rec.c >> 16) & 0xFF;
            rec.g = (rec.c >> 8) & 0xFF;
            rec.b = rec.c & 0xFF;

            records.add(rec);
        }

        return records;
    }

    public static int mergeRGB(int R, int G, int B)
    {
        int c = 0;

        c |= (R & 0xFF) << 16;
        c |= (G & 0xFF) << 8;
        c |= (B & 0xFF) << 0;
        return c;
    }

    public static int[] splitRGB(int c)
    {
        return new int[] { ((c>> 16) & 0xFF), ((c >> 8) & 0xFF), (c & 0xFF)};
    }

    /**
     * Convert a wavelength in the visible light spectrum to a RGB color value that is suitable to be displayed on a
     * monitor
     *
     * @param wavelength wavelength in nm
     * @return RGB color encoded in int. each color is represented with 8 bits and has a layout of
     * 00000000RRRRRRRRGGGGGGGGBBBBBBBB where MSB is at the leftmost
     */
    public static int wavelengthToRGB(double wavelength){
        double[] xyz = cie1931WavelengthToXYZFit(wavelength);
        double[] rgb = srgbXYZ2RGB(xyz);

        int c = 0;
        c |= (((int) (rgb[0] * 0xFF)) & 0xFF) << 16;
        c |= (((int) (rgb[1] * 0xFF)) & 0xFF) << 8;
        c |= (((int) (rgb[2] * 0xFF)) & 0xFF) << 0;

        return c;
    }

    /**
     * Convert XYZ to RGB in the sRGB color space
     * <p>
     * The conversion matrix and color component transfer function is taken from http://www.color.org/srgb.pdf, which
     * follows the International Electrotechnical Commission standard IEC 61966-2-1 "Multimedia systems and equipment -
     * Colour measurement and management - Part 2-1: Colour management - Default RGB colour space - sRGB"
     *
     * @param xyz XYZ values in a double array in the order of X, Y, Z. each value in the range of [0.0, 1.0]
     * @return RGB values in a double array, in the order of R, G, B. each value in the range of [0.0, 1.0]
     */
    public static double[] srgbXYZ2RGB(double[] xyz) {
        double x = xyz[0];
        double y = xyz[1];
        double z = xyz[2];

        double rl =  3.2406255 * x + -1.537208  * y + -0.4986286 * z;
        double gl = -0.9689307 * x +  1.8757561 * y +  0.0415175 * z;
        double bl =  0.0557101 * x + -0.2040211 * y +  1.0569959 * z;

        return new double[] {
                srgbXYZ2RGBPostprocess(rl),
                srgbXYZ2RGBPostprocess(gl),
                srgbXYZ2RGBPostprocess(bl)
        };
    }

    /**
     * helper function for {@link #srgbXYZ2RGB(double[])}
     */
    private static double srgbXYZ2RGBPostprocess(double c) {
        // clip if c is out of range
        c = c > 1 ? 1 : (c < 0 ? 0 : c);

        // apply the color component transfer function
        c = c <= 0.0031308 ? c * 12.92 : 1.055 * Math.pow(c, 1. / 2.4) - 0.055;

        return c;
    }

    /**
     * A multi-lobe, piecewise Gaussian fit of CIE 1931 XYZ Color Matching Functions by Wyman el al. from Nvidia. The
     * code here is adopted from the Listing 1 of the paper authored by Wyman et al.
     * <p>
     * Reference: Chris Wyman, Peter-Pike Sloan, and Peter Shirley, Simple Analytic Approximations to the CIE XYZ Color
     * Matching Functions, Journal of Computer Graphics Techniques (JCGT), vol. 2, no. 2, 1-11, 2013.
     *
     * @param wavelength wavelength in nm
     * @return XYZ in a double array in the order of X, Y, Z. each value in the range of [0.0, 1.0]
     */
    public static double[] cie1931WavelengthToXYZFit(double wavelength) {
        double wave = wavelength;

        double x;
        {
            double t1 = (wave - 442.0) * ((wave < 442.0) ? 0.0624 : 0.0374);
            double t2 = (wave - 599.8) * ((wave < 599.8) ? 0.0264 : 0.0323);
            double t3 = (wave - 501.1) * ((wave < 501.1) ? 0.0490 : 0.0382);

            x =   0.362 * Math.exp(-0.5 * t1 * t1)
                    + 1.056 * Math.exp(-0.5 * t2 * t2)
                    - 0.065 * Math.exp(-0.5 * t3 * t3);
        }

        double y;
        {
            double t1 = (wave - 568.8) * ((wave < 568.8) ? 0.0213 : 0.0247);
            double t2 = (wave - 530.9) * ((wave < 530.9) ? 0.0613 : 0.0322);

            y =   0.821 * Math.exp(-0.5 * t1 * t1)
                    + 0.286 * Math.exp(-0.5 * t2 * t2);
        }

        double z;
        {
            double t1 = (wave - 437.0) * ((wave < 437.0) ? 0.0845 : 0.0278);
            double t2 = (wave - 459.0) * ((wave < 459.0) ? 0.0385 : 0.0725);

            z =   1.217 * Math.exp(-0.5 * t1 * t1)
                    + 0.681 * Math.exp(-0.5 * t2 * t2);
        }

        return new double[] { x, y, z };
    }
}
