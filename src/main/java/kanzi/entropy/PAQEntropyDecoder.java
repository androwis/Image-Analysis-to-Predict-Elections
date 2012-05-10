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

// This class is a port from the code of the dcs-bwt-compressor project
// http://code.google.com/p/dcs-bwt-compressor/(itself based on PAQ coders)



//// This is an algorithm for entropy compression of the Burrows-Wheeler
//// transform of a text. It was originally written by Matt Mahoney as
//// bbb.cpp - big block BWT compressor version 1, Aug. 31, 2006.
//// http://cs.fit.edu/~mmahoney/compression/bbb.cpp
////
//// ENTROPY CODING
////
//// BWT data is best coded with an order 0 model.  The transformed text tends
//// to have long runs of identical bytes (e.g. "nnbaaa").  The BWT data is
//// modeled with a modified PAQ with just one context (no mixing) followed
//// by a 5 stage SSE (APM) and bitwise arithmetic coding.  Modeling typically
//// takes about as much time as sorting and unsorting in slow mode.
//// The model uses about 5 MB memory.
//// [ Now reduced to about 256KB of memory. ]
////
//// The order 0 model consists of a mapping:
////
////             order 1, 2, 3 contexts ----------+
////                                              V
////  order 0 context -> bit history -> p -> APM chain -> arithmetic coder
////                  t1             sm
////
//// Bits are coded one at a time.  The arithmetic coder maintains a range
//// [lo, hi), initially [0, 1) and repeatedly subdivides the range in proportion
//// to p(0), p(1), the next bit probabilites predicted by the model.  The final
//// output is the intest base 256 number x such that lo <= x < hi.  As the
//// leading bytes of x become known, they are output.  To decompress, the model
//// predictions are repeated as during compression, then the actual bit is
//// determined by which half of the subrange contains x.
////
//// The model inputs a bytewise order 0 context consisting of the last 0 to 7
//// bits of the current byte, plus the number of bits.  There are a total of
//// 255 possible bitwise contexts.  For each context, a table (t1) maintains
//// an 8 bit state representing the history of 0 and 1 bits previously seen.
//// This history is mapped by another table (a StateMap sm) to a probability,
//// p, that the next bit will be 1. This table is adaptive: after each
//// prediction, the mapping (state -> p) is adjusted to improve the last
//// prediction.
////
//// The output of the StateMap is passed through a series of 6 more adaptive
//// tables, (Adaptive Probability Maps, or APM) each of which maps a context
//// and the input probability to an output probability.  The input probability
//// is interpolated between 33 bins on a nonlinear scale with smaller bins
//// near 0 and 1.  After each prediction, the corresponding table entries
//// on both sides of p are adjusted to improve the last prediction.
////  The APM chain is like this:
////
////      + A11 ->+            +--->---+ +--->---+
////      |       |            |       | |       |
////  p ->+       +-> A2 -> A3 +-> A4 -+-+-> A5 -+-> Decoder
////      |       |
////      + A12 ->+
////
//// [ The APM chain has been modified into:
////
////                 +--->---+ +--->---+
////                 |       | |       |
////  p --> A2 -> A3 +-> A5 -+-+-> A4 -+-> Decoder
////
//// ]
////
//// A11 and A12 both take c0 (the preceding bits of the current byte) as
//// additional context, but one is fast adapting and the other is slow
//// adapting.  Their outputs are averaged.
////
//// A2 is an order 1 context (previous byte and current partial byte).
//// [ A2 has been modified so that it uses only two bits of information
//// from the previous byte: what is the bit in the current bit position
//// and whether the preceding bits are same or different from c0. ]
////
//// A3 takes the previous (but not current) byte as context, plus 2 bits
//// that depend on the current run length (0, 1, 2-3, or 4+), the number
//// of times the last byte was repeated.
//// [ A3 now only takes the two bits on run length. ]
////
//// A4 takes the current byte and the low 5 bits of the second byte back.
//// The output is averaged with 3/4 weight to the A3 output with 1/4 weight.
//// [ A4 has been moved after A5, it takes only the current byte (not the
//// 5 additional bits), and the averaging weights are 1/2 and 1/2. ]
////
//// A5 takes a 14 bit hash of an order 3 context (last 3 bytes plus
//// current partial byte) and is averaged with 1/2 weight to the A4 output.
//// [ A5 takes now 11 bit hash of an order 4 context. ]
////
//// The StateMap, state table, APM, Decoder, and associated code (Array,
//// squash(), stretch()) are taken from PAQ8 with minor non-functional
//// changes (e.g. removing global context).

public class PAQEntropyDecoder extends AbstractDecoder
{
   private final PAQPredictor predictor;
   private long low;
   private long high;
   private long current;
   private final InputBitStream bitstream;

   
   public PAQEntropyDecoder(InputBitStream bitstream)
   {
      if (bitstream == null)
         throw new NullPointerException("Invalid null bistream parameter");

      this.low = 0L;
      this.high = 0xFFFFFFFFL;
      this.bitstream = bitstream;
      this.predictor = new PAQPredictor();
      this.current = -1;
   }


   @Override
   public byte decodeByte()
   {
      // Initialize 'current' with bytes read from the bitstream
      if (this.current == -1)
         this.current = this.bitstream.readBits(32);

      int res = 0;

      for (int i=7; i>=0; i--)
      {
        // Compute prediction
        int bit;
        int pred = this.predictor.get();

        // Calculate interval split
        final long xmid = (this.low + (((this.high - this.low) >> 12) * pred) +
                 ((((this.high - this.low) & 0x0FFF) * pred) >> 12));

        if (this.current <= xmid)
        {
           bit = 1;
           this.high = xmid;
        }
        else
        {
           bit = 0;
           this.low = xmid + 1;
        }

         // Update predictor
        this.predictor.update(bit);

         // Read from bitstream
        while (((this.low ^ this.high) & 0xFF000000L) == 0)
        {
           this.low = (this.low << 8) & 0xFFFFFFFFL;
           this.high = ((this.high << 8) + 255) & 0xFFFFFFFFL;
           this.current = ((this.current << 8) | this.bitstream.readBits(8)) & 0xFFFFFFFFL;

        }

         res |= (bit << i);
      }

      return (byte) res;
   }


   @Override
   public InputBitStream getBitStream()
   {
      return this.bitstream;
   }
}
