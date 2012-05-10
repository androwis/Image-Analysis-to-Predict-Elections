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


// Walsh-Hadamard transform of dimension 16
public final class WHT16 implements IntTransform
{
    private final int[] data;


    public WHT16()
    {
        this.data = new int[256];
    }


    public int[] forward(int[] block)
    {
        return this.compute(block, 0);
    }


    // Not thread safe
    @Override
    public int[] forward(int[] block, int blkptr)
    {
        return this.compute(block, blkptr);
    }


    // Not thread safe
    private int[] compute(int[] block, int blkptr)
    {
        int dataptr = 0;
        int end = blkptr + 256;
        int[] buffer = this.data;

        // Pass 1: process rows.
        for (int i=blkptr; i<end; i+=16)
        {
            // Aliasing for speed
            int x0  = block[i];
            int x1  = block[i+1];
            int x2  = block[i+2];
            int x3  = block[i+3];
            int x4  = block[i+4];
            int x5  = block[i+5];
            int x6  = block[i+6];
            int x7  = block[i+7];
            int x8  = block[i+8];
            int x9  = block[i+9];
            int x10 = block[i+10];
            int x11 = block[i+11];
            int x12 = block[i+12];
            int x13 = block[i+13];
            int x14 = block[i+14];
            int x15 = block[i+15];

            int a0  = x0  + x1;
            int a1  = x2  + x3;
            int a2  = x4  + x5;
            int a3  = x6  + x7;
            int a4  = x8  + x9;
            int a5  = x10 + x11;
            int a6  = x12 + x13;
            int a7  = x14 + x15;
            int a8  = x0  - x1;
            int a9  = x2  - x3;
            int a10 = x4  - x5;
            int a11 = x6  - x7;
            int a12 = x8  - x9;
            int a13 = x10 - x11;
            int a14 = x12 - x13;
            int a15 = x14 - x15;

            int b0  = a0  + a1;
            int b1  = a2  + a3;
            int b2  = a4  + a5;
            int b3  = a6  + a7;
            int b4  = a8  + a9;
            int b5  = a10 + a11;
            int b6  = a12 + a13;
            int b7  = a14 + a15;
            int b8  = a0  - a1;
            int b9  = a2  - a3;
            int b10 = a4  - a5;
            int b11 = a6  - a7;
            int b12 = a8  - a9;
            int b13 = a10 - a11;
            int b14 = a12 - a13;
            int b15 = a14 - a15;

            a0  = b0  + b1;
            a1  = b2  + b3;
            a2  = b4  + b5;
            a3  = b6  + b7;
            a4  = b8  + b9;
            a5  = b10 + b11;
            a6  = b12 + b13;
            a7  = b14 + b15;
            a8  = b0  - b1;
            a9  = b2  - b3;
            a10 = b4  - b5;
            a11 = b6  - b7;
            a12 = b8  - b9;
            a13 = b10 - b11;
            a14 = b12 - b13;
            a15 = b14 - b15;

            buffer[dataptr]    = a0  + a1;
            buffer[dataptr+1]  = a2  + a3;
            buffer[dataptr+2]  = a4  + a5;
            buffer[dataptr+3]  = a6  + a7;
            buffer[dataptr+4]  = a8  + a9;
            buffer[dataptr+5]  = a10 + a11;
            buffer[dataptr+6]  = a12 + a13;
            buffer[dataptr+7]  = a14 + a15;
            buffer[dataptr+8]  = a0  - a1;
            buffer[dataptr+9]  = a2  - a3;
            buffer[dataptr+10] = a4  - a5;
            buffer[dataptr+11] = a6  - a7;
            buffer[dataptr+12] = a8  - a9;
            buffer[dataptr+13] = a10 - a11;
            buffer[dataptr+14] = a12 - a13;
            buffer[dataptr+15] = a14 - a15;

            dataptr += 16;
        }

        dataptr = 0;
        end = blkptr + 16;

        // Pass 2: process aolumns.
        for (int i=blkptr; i<end; i++)
        {
            // Aliasing for speed
            int x0  = buffer[dataptr];
            int x1  = buffer[dataptr+16];
            int x2  = buffer[dataptr+32];
            int x3  = buffer[dataptr+48];
            int x4  = buffer[dataptr+64];
            int x5  = buffer[dataptr+80];
            int x6  = buffer[dataptr+96];
            int x7  = buffer[dataptr+112];
            int x8  = buffer[dataptr+128];
            int x9  = buffer[dataptr+144];
            int x10 = buffer[dataptr+160];
            int x11 = buffer[dataptr+176];
            int x12 = buffer[dataptr+192];
            int x13 = buffer[dataptr+208];
            int x14 = buffer[dataptr+224];
            int x15 = buffer[dataptr+240];

            int a0  = x0  + x1;
            int a1  = x2  + x3;
            int a2  = x4  + x5;
            int a3  = x6  + x7;
            int a4  = x8  + x9;
            int a5  = x10 + x11;
            int a6  = x12 + x13;
            int a7  = x14 + x15;
            int a8  = x0  - x1;
            int a9  = x2  - x3;
            int a10 = x4  - x5;
            int a11 = x6  - x7;
            int a12 = x8  - x9;
            int a13 = x10 - x11;
            int a14 = x12 - x13;
            int a15 = x14 - x15;

            int b0  = a0  + a1;
            int b1  = a2  + a3;
            int b2  = a4  + a5;
            int b3  = a6  + a7;
            int b4  = a8  + a9;
            int b5  = a10 + a11;
            int b6  = a12 + a13;
            int b7  = a14 + a15;
            int b8  = a0  - a1;
            int b9  = a2  - a3;
            int b10 = a4  - a5;
            int b11 = a6  - a7;
            int b12 = a8  - a9;
            int b13 = a10 - a11;
            int b14 = a12 - a13;
            int b15 = a14 - a15;

            a0  = b0  + b1;
            a1  = b2  + b3;
            a2  = b4  + b5;
            a3  = b6  + b7;
            a4  = b8  + b9;
            a5  = b10 + b11;
            a6  = b12 + b13;
            a7  = b14 + b15;
            a8  = b0  - b1;
            a9  = b2  - b3;
            a10 = b4  - b5;
            a11 = b6  - b7;
            a12 = b8  - b9;
            a13 = b10 - b11;
            a14 = b12 - b13;
            a15 = b14 - b15;

            block[i]      = (a0  + a1  + 8) >> 4;
            block[i+16]   = (a2  + a3  + 8) >> 4;
            block[i+32]   = (a4  + a5  + 8) >> 4;
            block[i+48]   = (a6  + a7  + 8) >> 4;
            block[i+64]   = (a8  + a9  + 8) >> 4;
            block[i+80]   = (a10 + a11 + 8) >> 4;
            block[i+96]   = (a12 + a13 + 8) >> 4;
            block[i+112]  = (a14 + a15 + 8) >> 4;
            block[i+128]  = (a0  - a1  + 8) >> 4;
            block[i+144]  = (a2  - a3  + 8) >> 4;
            block[i+160]  = (a4  - a5  + 8) >> 4;
            block[i+176]  = (a6  - a7  + 8) >> 4;
            block[i+192]  = (a8  - a9  + 8) >> 4;
            block[i+208]  = (a10 - a11 + 8) >> 4;
            block[i+224]  = (a12 - a13 + 8) >> 4;
            block[i+240]  = (a14 - a15 + 8) >> 4;

            dataptr++;
        }

        return block;
    }


    // Yep, the transform is symetric
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