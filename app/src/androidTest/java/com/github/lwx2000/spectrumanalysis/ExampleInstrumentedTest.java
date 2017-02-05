package com.github.lwx2000.spectrumanalysis;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest
{
    @Test
    public void useAppContext() throws Exception
    {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.github.lwx2000.spectrumanalysis", appContext.getPackageName());

        // 测试ColorMatchingFunction：错误的结果
        List<ColourMatchingFunction> recs = ColourMatchingFunction.GetRecords(
                appContext, MainActivity.WAVELEN_START, MainActivity.WAVELEN_END);
        int R= 132, G = 128, B = 128;
        R = 255; G = 0; B = 0;
        //ColourMatchingFunction rec = ColourMatchingFunction.FindRecord(recs, R, G, B);

        // 测试ColorMatchingFunction2：正确的结果
        double wavelen = 555;
        int rgb[];
        int c;

        List<ColorMatchingFunction2> recs2 = ColorMatchingFunction2.GetRecords(
                MainActivity.WAVELEN_START, MainActivity.WAVELEN_END);
        ColorMatchingFunction2 rec2;

        // 波长397: c=60, rgb(0,0,60)
        // 波长398: c=196672, rgb(3,0,64)
        // 波长555: c=6815232, rgb(103,254,0)
        // 波长473: c=20734, rgb(0,80,254)
        // 波长600: c=16675072, rgb(254,113,0)

        rec2 = ColorMatchingFunction2.FindRecord(recs2, ColorMatchingFunction2.mergeRGB(128, 128, 128));    // 669nm
        rec2 = ColorMatchingFunction2.FindRecord(recs2, ColorMatchingFunction2.mergeRGB(255, 255, 255));    // 570nm
        rec2 = ColorMatchingFunction2.FindRecord(recs2, ColorMatchingFunction2.mergeRGB(255, 0, 0));        // 570nm
        rec2 = ColorMatchingFunction2.FindRecord(recs2, ColorMatchingFunction2.mergeRGB(0, 0, 0));          // 734nm
        rec2 = ColorMatchingFunction2.FindRecord(recs2, ColorMatchingFunction2.mergeRGB(132, 128, 128));    // 436nm
        rec2 = ColorMatchingFunction2.FindRecord(recs2, ColorMatchingFunction2.mergeRGB(0, 0, 255));        // 460nm

        rec2 = ColorMatchingFunction2.FindRecord(recs2, 128, 128, 128);    // 669nm
        rec2 = ColorMatchingFunction2.FindRecord(recs2, 255, 255, 255);    // 644nm
        rec2 = ColorMatchingFunction2.FindRecord(recs2, 255, 0, 0);        // 680nm
        rec2 = ColorMatchingFunction2.FindRecord(recs2, 0, 0, 0);          // 276nm
        rec2 = ColorMatchingFunction2.FindRecord(recs2, 132, 128, 128);    // 669nm
        rec2 = ColorMatchingFunction2.FindRecord(recs2, 0, 0, 255);        // 680nm

        //
    }
}
