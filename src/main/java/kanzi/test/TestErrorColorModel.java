/*
Copyright 2011 Frederic Langlet
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
you may obtain a copy of the License at

                http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kanzi.test;

/**
 *
 * @author flanglet
 */
public class TestErrorColorModel
{

   public static void main(String[] args)
   {
       double sum1 = 0;
       double sum2 = 0;
       double sum3 = 0;
       double sum4 = 0;

      for (int r=0; r<256; r++)
      {
         for (int g=0; g<256; g++)
         {
            for (int b=0; b<256; b++)
            {
               // Float YSbSr
               float y1 = 0.6460f*r + 0.6880f*g + 0.6660f*b;
               float sb = -r + 0.2120f*g + 0.7880f*b;
               float sr = -0.3220f*r + g - 0.6780f*b;
               float rr = 0.5f*y1 - 0.6077f*sb - 0.2152f*sr;
               float gg = 0.5f*y1 + 0.1200f*sb + 0.6306f*sr;
               float bb = 0.5f*y1 + 0.4655f*sb - 0.4427f*sr;
               float val1, val2, val3;
               val1 = rr - (float) r;
               val1 = (val1 > 0) ? val1 : -val1;
               val2 = gg - (float) g;
               val2 = (val2 > 0) ? val2 : -val2;
               val3 = bb - (float) b;
               val3 = (val3 > 0) ? val3 : -val3;
               sum1 += (val1 + val2 + val3);

               // Float YCrCb
               float y2 =  16 + ( 65.738f*r + 129.057f*g + 25.064f*b) / 256.0f;
               float cb = 128 + (-37.945f*r - 74.494f*g + 112.439f*b) / 256.0f;
               float cr = 128 + (112.439f*r - 94.154f*g -  18.285f*b) / 256.0f;
               rr = (298.082f*y2 + 408.583f*cr) / 256.0f - 222.921f;
               gg = (298.082f*y2 - 100.291f*cb - 208.120f*cr) / 256.0f + 135.576f;
               bb = (298.082f*y2 + 516.412f*cb) / 256.0f - 276.836f;
               val1 = rr - (float) r;
               val1 = (val1 > 0) ? val1 : -val1;
               val2 = gg - (float) g;
               val2 = (val2 > 0) ? val2 : -val2;
               val3 = bb - (float) b;
               val3 = (val3 > 0) ? val3 : -val3;
               sum2 += (val1 + val2 + val3);

               // Int YSbSr
               int yVal1 = (( 83*r   + 88*g    +  85*b) + 64) >> 7;
               int bVal1 = ((-(r<<7) + 27*g    + 101*b) + 64) >> 7;
               int rVal1 = ((-41*r   + (g <<7) -  87*b) + 64) >> 7;
//               yVal1 = ((1323*r + 1409*g + 1364*b) + 1024) >> 11;
//               bVal1 = ((-(r<<11) + 434*g + 1614*b) + 1024) >> 11;
//               rVal1 = ((-1319*r + (g<<12) - 2777*b) + 2048) >> 12;

               int yVal, uVal, vVal, bVal, rVal;
               int rrr, ggg, bbb;

               yVal = yVal1 << 7;
               bVal = bVal1;
               rVal = rVal1;
               rrr = ((yVal - 156*bVal -  55*rVal) + 128) >> 8;
               ggg = ((yVal +  31*bVal + 161*rVal) + 128) >> 8;
               bbb = ((yVal + 119*bVal - 113*rVal) + 128) >> 8;
               //yVal = yVal1 << 5;
               //rrr = ((yVal -  39*bVal - 14*rVal) + 32) >> 6;
               //ggg = ((yVal +   8*bVal + 40*rVal) + 32) >> 6;
               //bbb = ((yVal +  30*bVal - 28*rVal) + 32) >> 6;
               rrr = rrr & ((-rrr) >> 31);
               ggg = ggg & ((-ggg) >> 31);
               bbb = bbb & ((-bbb) >> 31);

               if (rrr >= 255) rrr = 255;
               if (ggg >= 255) ggg = 255;
               if (bbb >= 255) bbb = 255;

               val1 = (r > rrr) ? r - rrr : rrr - r;
               val2 = (g > ggg) ? g - ggg : ggg - g;
               val3 = (b > bbb) ? b - bbb : bbb - b;
               sum3 += (val1 + val2 + val3);
               
               // Int YCrCb
               yVal =  66*r + 129*g +  25*b;
               uVal = -38*r -  74*g + 112*b;
               vVal = 112*r -  94*g -  18*b;

               yVal = ((yVal + 128) >> 8) + 16;

               if (uVal >= 32384) // ((255-128) << 8) - 128
                    uVal = 255;
               else
               {
                    uVal = ((uVal + 128) >> 8) + 128;
                    uVal = uVal & ((-uVal) >> 31); // (uVal <= 0) ? 0 : uVal;
               }

               if (vVal >= 32384) // ((255-128) << 8) - 128
                    vVal = 255;
               else
               {
                    vVal = ((vVal + 128) >> 8) + 128;
                    vVal= vVal & ((-vVal) >> 31); // (vVal <= 0) ? 0 : vVal;
               }

               yVal = 298 * (yVal-16); 
               uVal = uVal - 128;
               vVal = vVal- 128;
               rrr = yVal + (409*vVal);
               ggg = yVal - (100*uVal) - (208*vVal);
               bbb = yVal + (516*uVal);

               if (rrr >= 65408) rrr = 255;
               else { rrr = (rrr + 128) >> 8; rrr = rrr & (-rrr >> 31); }

               if (ggg >= 65408) ggg = 255;
               else { ggg = (ggg + 128) >> 8; ggg = ggg & (-ggg >> 31); }

               if (bbb >= 65408) bbb = 255;
               else { bbb = (bbb + 128) >> 8; bbb = bbb & (-bbb >> 31); }

               val1 = (r > rrr) ? r - rrr : rrr - r;
               val2 = (g > ggg) ? g - ggg : ggg - g;
               val3 = (b > bbb) ? b - bbb : bbb - b;
               sum4 += (val1 + val2 + val3);               
            }
         }

      }



       System.out.println("Mean error float YSbSr: "+sum1/(256*256*256));
       System.out.println("Mean error float YCrCb: "+sum2/(256*256*256));
       System.out.println("Mean error int   YSbSr: "+sum3/(256*256*256));
       System.out.println("Mean error int   YCrCb: "+sum4/(256*256*256));
   }
}
