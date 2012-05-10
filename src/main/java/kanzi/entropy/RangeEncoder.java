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

package kanzi.entropy;

import kanzi.OutputBitStream;


// Based on Order 0 range coder by Dmitry Subbotin itself derived from the algorithm
// described by G.N.N Martin in his seminal article in 1979.
// [G.N.N. Martin on the Data Recording Conference, Southampton, 1979]
// Optimized for speed.

// Not thread safe
public final class RangeEncoder extends AbstractEncoder
{
    private static final long TOP       = 1L << 48;
    private static final long BOTTOM    = (1L << 40) - 1;
    private static final long MAX_RANGE = BOTTOM + 1;
    private static final long MASK      = 0x00FFFFFFFFFFFFFFL;

    private static final int NB_SYMBOLS = 257; //256 + EOF

    private long low;
    private long range;
    private boolean flushed;
    private final int[] baseFreq;
    private final int[] deltaFreq;
    private final OutputBitStream bitstream;
    private boolean written;


    public RangeEncoder(OutputBitStream bitstream)
    {
        if (bitstream == null)
            throw new NullPointerException("Invalid null bitstream parameter");

        this.range = (TOP << 8) - 1;
        this.bitstream = bitstream;

        // Since the frequency update after each byte encoded is the bottleneck,
        // split the frequency table into an array of absolute frequencies (with
        // indexes multiple of 16) and delta frequencies (relative to the previous
        // absolute frequency) with indexes in the [0..15] range
        this.deltaFreq = new int[NB_SYMBOLS+1];
        this.baseFreq = new int[(NB_SYMBOLS>>4)+1];

        for (int i=0; i<this.deltaFreq.length; i++)
            this.deltaFreq[i] = i & 15; // DELTA

        for (int i=0; i<this.baseFreq.length; i++)
            this.baseFreq[i] = i << 4; // BASE
    }


    // This method is on the speed critical path (called for each byte)
    // The speed optimization is focused on reducing the frequency table update
    @Override
    public boolean encodeByte(byte b)
    {
        int value = b & 0xFF;
        int symbolLow = this.baseFreq[value>>4] + this.deltaFreq[value];
        int symbolHigh = this.baseFreq[(value+1)>>4] + this.deltaFreq[value+1];
        this.range /= (this.baseFreq[NB_SYMBOLS>>4] + this.deltaFreq[NB_SYMBOLS]);

        // Encode symbol
        this.low += (symbolLow * this.range);
        this.range *= (symbolHigh - symbolLow);

        long checkRange = ((this.low ^ (this.low + this.range)) & MASK);

        // If the left-most digits are the same throughout the range, write bits to bitstream
        while ((checkRange < TOP) || (this.range < MAX_RANGE))
        {
            // Normalize
            if (checkRange >= TOP)
                this.range = (-this.low & MASK) & BOTTOM;

            this.bitstream.writeBits(((this.low >> 48) & 0xFF), 8);
            this.range <<= 8;
            this.low <<= 8;
            checkRange = ((this.low ^ (this.low + this.range)) & MASK);
        }

        // Update frequencies: computational bottleneck !!!
        this.updateFrequencies(value+1);
        this.written = true;
        return true;
    }


    private void updateFrequencies(int value)
    {
        int[] freq = this.baseFreq;
        final int start = (value + 15) >> 4;
        final int len = freq.length;

        // Update absolute frequencies
        for (int j=start; j<len; j++)
            freq[j]++;

        freq = this.deltaFreq;

        // Update relative frequencies (in the 'right' segment only)
        for (int j=(start<<4)-1; j>=value; j--)
            freq[j]++;
    }


    @Override
    public void dispose()
    {
        if ((this.written == true) && (this.flushed == false))
        {
            // After this call the frequency tables may not be up to date
            this.flushed = true;

            for (int i=0; i<7; i++)
            {
                this.bitstream.writeBits(((this.low >> 48) & 0xFF), 8);
                this.low <<= 8;
            }

            this.bitstream.flush();
        }
    }


    @Override
    public OutputBitStream getBitStream()
    {
       return this.bitstream;
    }
}