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

package kanzi.util;

import java.io.ByteArrayOutputStream;
import kanzi.EntropyEncoder;
import kanzi.IndexedByteArray;
import kanzi.IntTransform;
import kanzi.OutputBitStream;
import kanzi.bitstream.DefaultOutputBitStream;
import kanzi.function.BlockCodec;
import kanzi.transform.WHT16;
import kanzi.transform.WHT8;
import kanzi.util.IntraPredictor.Mode;


// Encode a complete intra frame
// For each line of 16 pixels, analyze energy of 16x16 (and potentially 8x8) blocks
// Select best partition and find best mode prediction; compute difference with
// prediction and apply DHT then quantize. Encode the quantized coefficients.
// Finally, use the block codec to entropy encode the resulting data.
public class IntraEncoder
{
   private static final int DEFAULT_ERROR_THRESHOLD = 26010 ; // 5%*255*255 (scaled by 8)

   private final int width;
   private final int height;
   private final int stride;
   private final BlockAnalyzer ba16;
   private final int errThreshold8;
   private final EncodingProcessor processor8;
   private final EncodingProcessor processor16;
   private final BlockCodec blockCodec;
   private final EntropyEncoder entropyCoder;
   private final ByteArrayOutputStream residueBOS;
   private final OutputBitStream residueBS;


   public IntraEncoder(int width, int height, int stride, int[] quantMatrix, EntropyEncoder entropyCoder)
   {
      this(width, height, stride, quantMatrix, entropyCoder, 25, 
              new BlockCodec(65536), DEFAULT_ERROR_THRESHOLD);
   }


   public IntraEncoder(int width, int height, int stride, int[] quantMatrix, 
           EntropyEncoder entropyCoder, int percentHotspots)
   {
      this(width, height, stride, quantMatrix, entropyCoder, 
              percentHotspots, new BlockCodec(65536), DEFAULT_ERROR_THRESHOLD);
   }


   // errThreshold selects 16x16 or 8x8 blocks
   public IntraEncoder(int width, int height, int stride, int[] quantMatrix, 
           EntropyEncoder entropyCoder, int percentHotspots, BlockCodec blockCodec, 
           int errThreshold8)
   {
      if ((percentHotspots < 1) || (percentHotspots > 100))
         throw new IllegalArgumentException("The percentage of hotspots must be "
                 + "in the [1..100] range");

      if ((errThreshold8 < 1) || (errThreshold8 > (255 << 8)))
         throw new IllegalArgumentException("The error threshold for choice of "
                 + "block dimension must be in the [1..255] per pixel range");

      if ((quantMatrix != null) && (quantMatrix.length != 64))
          throw new IllegalArgumentException("Invalid quantization matrix length: "
                  +quantMatrix.length+" (must be 64)");

      this.ba16 = new BlockAnalyzer(16, stride);
      this.width = width;
      this.height = height;
      this.stride = stride;
      this.errThreshold8 = errThreshold8; // Error for a 16x16 block
      this.residueBOS = new ByteArrayOutputStream(32768);
      this.residueBS = new DefaultOutputBitStream(this.residueBOS, 32768);
      this.blockCodec = (blockCodec != null) ? blockCodec : new BlockCodec(65536);
      this.entropyCoder = entropyCoder;
      ResidueBlockEncoder rbc = new ResidueBlockEncoder(3, this.residueBS);
      this.processor8  = new EncodingProcessor(new IntraPredictor(8, width, height),
              new WHT8(), rbc, quantMatrix);
      this.processor16 = new EncodingProcessor(new IntraPredictor(16, width, height),
              new WHT16(), rbc, quantMatrix);
   }


   public int encode(int[] input, int[] previous, int[] output)
   {
      int iOffs = 0;
      int oOffs = 0;
      final int h = this.height;
      final int w = this.width;
      final int line8 = this.stride << 3;
      this.processor8.input = input;
      this.processor8.output = output;
      this.processor8.previous = previous;
      this.processor16.input = input;
      this.processor16.output = output;
      this.processor16.previous = previous;
      final OutputBitStream bitstream = this.entropyCoder.getBitStream();

      for (int y=0; y<h; y+=16)
      {
         int prevPredMode = IntraPredictor.Mode.TEMPORAL.ordinal();

         // Alternate 0 and 4 starting offset per row (used to remove vertical
         // blocking artefacts during decompression)
         int x0 = 0;//((y >> 4) & 1) << 2; 

         // Analyze line of 16x16 and/or 8x8 blocks
         for (int x=x0; x<w; x+=16)
         {
            int offset = iOffs + x;
            int nrj16 = this.ba16.computeEnergy(input, offset);
  System.out.println(nrj16+" "+errThreshold8);
            // If good enough, proceed with a 16x16 block
            if (nrj16 <= this.errThreshold8)
            {
                bitstream.writeBit(1);
                bitstream.writeBits(prevPredMode, 3);
                prevPredMode = this.processor16.encode(x, y, prevPredMode, offset, oOffs);
                oOffs += 256;
            }
            else
            {
                // Z scanning of the four 8x8 blocks
                bitstream.writeBit(0);
                bitstream.writeBits(prevPredMode, 3);
                prevPredMode = this.processor8.encode(x, y, prevPredMode, offset, oOffs);
                oOffs += 64;
                bitstream.writeBits(prevPredMode, 3);
                prevPredMode = this.processor8.encode(x+8, y, prevPredMode, offset+8, oOffs);
                oOffs += 64;
                bitstream.writeBits(prevPredMode, 3);
                prevPredMode = this.processor8.encode(x, y+8, prevPredMode, offset+line8, oOffs);
                oOffs += 64;
                bitstream.writeBits(prevPredMode, 3);
                prevPredMode = this.processor8.encode(x+8, y+8, prevPredMode, offset+line8+8, oOffs);
                oOffs += 64;
            }
         }

         iOffs += line8;
         iOffs += line8;
      }
      
      // Now, block-and-entropy encode the pre-processed residue coefficients
      this.residueBS.flush();   
      this.blockCodec.setSize(0);
      IndexedByteArray iba = new IndexedByteArray(this.residueBOS.toByteArray(), 0);
      return this.blockCodec.encode(iba, this.entropyCoder);
   }
   
   
   private static class EncodingProcessor
   {
      final IntraPredictor predictor;
      final IntTransform transform;
      final ResidueBlockEncoder rbc;
      final int[] quantMatrix;
      int[] input;
      int[] output;
      int[] previous;
      
      
      EncodingProcessor(IntraPredictor predictor, IntTransform transform, 
              ResidueBlockEncoder rbc, int[] quantMatrix)
      {
         this.predictor = predictor;
         this.transform = transform;
         this.rbc = rbc;
         this.quantMatrix = quantMatrix;
      }
      
      
      // Return selected prediction mode
      public int encode(int x, int y, int prevPredMode, int iOffs, int oOffs)
      {
          // Find best mode prediction
          long predMode = this.predictor.predict(this.input, x, y, this.previous, x, y);
          int currentPredMode;
          System.out.println("x="+x+", y="+y);          
//
//          // There are 3 possibilities: right dependent modes (<), left dependent modes (>)
//          // and other modes (|) such as TEMPORAL or VERTICAL
//          // Scanning from left if the previous mode was <, then only < or | is allowed
//          // Otherwise, any mode is allowed. The modes are decided based on
//          // the minimal energy of the residue giving the above constraints.
//          int errLeft = (int) ((predMode >> 19) & 0xFFFF);
//          int errRight = (int) ((predMode >> 38) & 0xFFFF);
//          int errOther = (int) (predMode & 0xFFFF);
//          int modeLeft = (int) ((predMode >> 54) & 7);
//          int modeRight = (int) ((predMode >> 35) & 7);
//          int modeOther = (int) ((predMode >> 16) & 7);
//
//          if (prevPredMode == 0/*right dependent*/)
//          {
//              if (errRight < errOther)
//                  currentPredMode = modeRight;
//              else
//                  currentPredMode = modeOther;
//          } 
//          else
//          {
//              if (errOther <= errRight)
//              {
//                  if (errLeft < errOther)
//                      currentPredMode = modeLeft;
//                  else
//                      currentPredMode = modeOther;
//              } 
//              else if (errLeft < errRight)
//                  currentPredMode = modeLeft;
//              else
//                  currentPredMode = modeRight;
//          }
          if (x == 0)
             if (y == 0)
                currentPredMode = Mode.TEMPORAL.ordinal();
             else
                currentPredMode = Mode.VERTICAL.ordinal();
          else
             currentPredMode = Mode.HORIZONTAL_L.ordinal();

          // Compute diff block
          this.predictor.computeDifferences(this.input, iOffs, this.output,
                  oOffs, this.previous, 0, currentPredMode);         

          // Apply Hadamard transform to diff block
          int oIdx = oOffs;
          this.transform.forward(this.output, oOffs);
          
// TODO, adaptive quantization based on energy (lower if high energy, higher if low energy)
          // Quantize the result of the transform
          if (this.quantMatrix != null)
          {
              int[] matrix = this.quantMatrix; // aliasing
              int[] out = this.output; //aliasing
              
              // Quantize diff block: use first 8x8 quadrant only, other coefficients
              // (high frequencies) are set to 0, which is similar to downsampling
              for (int i=0; i<64; i+=8)
              {
                  out[oIdx] *= matrix[i];
                  out[oIdx++] >>= 8;
                  out[oIdx] *= matrix[i+1];
                  out[oIdx++] >>= 8;
                  out[oIdx] *= matrix[i+2];
                  out[oIdx++] >>= 8;
                  out[oIdx] *= matrix[i+3];
                  out[oIdx++] >>= 8;
                  out[oIdx] *= matrix[i+4];
                  out[oIdx++] >>= 8;
                  out[oIdx] *= matrix[i+5];
                  out[oIdx++] >>= 8;
                  out[oIdx] *= matrix[i+6];
                  out[oIdx++] >>= 8;
                  out[oIdx] *= matrix[i+7];
                  out[oIdx++] >>= 8;
              }
          }

          // Encode the quantized coefficients of the residue block
          this.rbc.encode(this.output, oIdx-64);
          return currentPredMode;
      }
   }
}
