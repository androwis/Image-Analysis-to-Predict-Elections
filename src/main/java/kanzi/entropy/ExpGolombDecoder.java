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

import kanzi.InputBitStream;


// Exponential Golomb Coder
public final class ExpGolombDecoder extends AbstractDecoder
{
    private final boolean signed;
    private final InputBitStream bitstream;


    public ExpGolombDecoder(InputBitStream bitstream, boolean signed)
    {
        if (bitstream == null)
           throw new NullPointerException("Invalid null bitstream parameter");

        this.signed = signed;
        this.bitstream = bitstream;
    }


    public boolean isSigned()
    {
        return this.signed;
    }


    @Override
    public byte decodeByte()
    {
       int log2;
       long info = 0;

       // Decode unsigned
       for (log2=0; log2<8; log2++)
       {
          if (this.bitstream.readBit() == 1)
             break;
       }

       if (log2 > 0)
          info = this.bitstream.readBits(log2);

       byte res = (byte) ((1 << log2) - 1 + info);

       // Read signed if necessary
       if ((res != 0) && (this.signed == true))
       {
           // If res != 0, Get the sign (0 for negative values)
           if (this.bitstream.readBit() == 1)
               return (byte) -res;
       }

       return res;
    }


    @Override
    public InputBitStream getBitStream()
    {
       return this.bitstream;
    }
}
