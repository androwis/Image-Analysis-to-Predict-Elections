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

package kanzi.transform;

import kanzi.IntTransform;


// Walsh-Hadamard transform of dimension 4
public final class WHT4 implements IntTransform
{
    public WHT4()
    {
    }


    public int[] forward(int[] block)
    {
        return this.compute(block, 0);
    }


    // Thread safe
    @Override
    public int[] forward(int[] block, int blkptr)
    {
        return this.compute(block, blkptr);
    }


    // Not thread safe
    private int[] compute(int[] block, int blkptr)
    {
       int b0, b1, b2, b3, b4, b5, b6, b7;
       int b8, b9, b10, b11, b12, b13, b14, b15;

       // Pass 1: process rows.

       {
         // Aliasing for speed
         int x0  = block[blkptr];
         int x1  = block[blkptr+1];
         int x2  = block[blkptr+2];
         int x3  = block[blkptr+3];
         int x4  = block[blkptr+4];
         int x5  = block[blkptr+5];
         int x6  = block[blkptr+6];
         int x7  = block[blkptr+7];
         int x8  = block[blkptr+8];
         int x9  = block[blkptr+9];
         int x10 = block[blkptr+10];
         int x11 = block[blkptr+11];
         int x12 = block[blkptr+12];
         int x13 = block[blkptr+13];
         int x14 = block[blkptr+14];
         int x15 = block[blkptr+15];

         int a0  = x0  + x1;
         int a1  = x2  + x3;
         int a2  = x0  - x1;
         int a3  = x2  - x3;
         int a4  = x4  + x5;
         int a5  = x6  + x7;
         int a6  = x4  - x5;
         int a7  = x6  - x7;
         int a8  = x8  + x9;
         int a9  = x10 + x11;
         int a10 = x8  - x9;
         int a11 = x10 - x11;
         int a12 = x12 + x13;
         int a13 = x14 + x15;
         int a14 = x12 - x13;
         int a15 = x14 - x15;

         b0  = a0  + a1;
         b1  = a2  + a3;
         b2  = a0  - a1;
         b3  = a2  - a3;
         b4  = a4  + a5;
         b5  = a6  + a7;
         b6  = a4  - a5;
         b7  = a6  - a7;
         b8  = a8  + a9;
         b9  = a10 + a11;
         b10 = a8  - a9;
         b11 = a10 - a11;
         b12 = a12 + a13;
         b13 = a14 + a15;
         b14 = a12 - a13;
         b15 = a14 - a15;
       }

       // Pass 2: process columns.

       {
         int a0  = b0  + b4;
         int a1  = b8  + b12;
         int a2  = b0  - b4;
         int a3  = b8  - b12;
         int a4  = b1  + b5;
         int a5  = b9  + b13;
         int a6  = b1  - b5;
         int a7  = b9  - b13;
         int a8  = b2  + b6;
         int a9  = b10 + b14;
         int a10 = b2  - b6;
         int a11 = b10 - b14;
         int a12 = b3  + b7;
         int a13 = b11 + b15;
         int a14 = b3  - b7;
         int a15 = b11 - b15;

         block[blkptr]    = (a0  + a1  + 2) >> 2;
         block[blkptr+4]  = (a2  + a3  + 2) >> 2;
         block[blkptr+8]  = (a0  - a1  + 2) >> 2;
         block[blkptr+12] = (a2  - a3  + 2) >> 2;
         block[blkptr+1]  = (a4  + a5  + 2) >> 2;
         block[blkptr+5]  = (a6  + a7  + 2) >> 2;
         block[blkptr+9]  = (a4  - a5  + 2) >> 2;
         block[blkptr+13] = (a6  - a7  + 2) >> 2;
         block[blkptr+2]  = (a8  + a9  + 2) >> 2;
         block[blkptr+6]  = (a10 + a11 + 2) >> 2;
         block[blkptr+10] = (a8  - a9  + 2) >> 2;
         block[blkptr+14] = (a10 - a11 + 2) >> 2;
         block[blkptr+3]  = (a12 + a13 + 2) >> 2;
         block[blkptr+7]  = (a14 + a15 + 2) >> 2;
         block[blkptr+11] = (a12 - a13 + 2) >> 2;
         block[blkptr+15] = (a14 - a15 + 2) >> 2;
       }

       return block;
    }


    public int[] inverse(int[] block)
    {
        return this.compute(block, 0);
    }


    @Override
    public int[] inverse(int[] block, int blkptr)
    {
        return this.compute(block, blkptr);
    }

}