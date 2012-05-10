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


public final class ExpGolombEncoder extends AbstractEncoder
{
    private final boolean signed;
    private final OutputBitStream bitsream;
    
    
    public ExpGolombEncoder(OutputBitStream bitStream, boolean signed)
    {
       if (bitStream == null)
          throw new NullPointerException("Invalid null bitStream parameter");

       this.signed = signed;
       this.bitsream = bitStream;
    }
    
    
    public boolean isSigned()
    {
       return this.signed;
    }
       
    
    @Override
    public boolean encodeByte(byte val)
    {
       if (val == 0)
          return this.bitsream.writeBit(1);

       //  Take the number 'val' add 1 to it
       //  Count the bits (log2), subtract one, and write that number of zeros
       //  preceding the previous bit string to get the encoded value
       int log2 = 0;
       int val2 = val;
       val2 = (val2 + (val2 >> 31)) ^ (val2 >> 31); // abs(val2)
       val2++;
       long emit = val2;

       for ( ; val2>1; val2>>=1)
          log2++;

       // Add log2 zeros and 1 one (unary coding), then remainder
       // 0 => 1 => 1
       // 1 => 10 => 010
       // 2 => 11 => 011
       // 3 => 100 => 00100
       // 4 => 101 => 00101
       // 5 => 110 => 00110
       // 6 => 111 => 00111
       int n = log2 + (log2 + 1);

       if (this.signed == true)
       {
          // Add 0 for positive and 1 for negative sign
          n++;
          emit = (emit << 1) | (val >>> 31);
       }

       return (this.bitsream.writeBits(emit, n) == n);
    }  


    @Override
    public OutputBitStream getBitStream()
    {
       return this.bitsream;
    }
}
