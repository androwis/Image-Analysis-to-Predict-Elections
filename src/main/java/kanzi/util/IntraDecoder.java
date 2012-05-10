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

import java.io.ByteArrayInputStream;
import kanzi.EntropyDecoder;
import kanzi.IndexedByteArray;
import kanzi.InputBitStream;
import kanzi.IntTransform;
import kanzi.bitstream.DefaultInputBitStream;
import kanzi.function.BlockCodec;
import kanzi.transform.WHT16;
import kanzi.transform.WHT8;


// Decode a complete intra frame
// For each line of 16 pixels, analyze energy of 16x16 (and potentially 8x8) blocks
// Select best partition and find best mode prediction; compute difference with
// prediction and apply DHT then quantize. Decode the quantized coefficients.
// Finally, use the block codec to entropy Decode the resulting data.
public class IntraDecoder
{
   private final int width;
   private final int height;
   private final int stride;
   private final DecodingProcessor processor8;
   private final DecodingProcessor processor16;
   private final BlockCodec blockCodec;
   private final EntropyDecoder entropyDecoder;
   private final ByteArrayInputStream residueBIS;
   private final InputBitStream residueBS;
   private final byte[] buffer;


   public IntraDecoder(int width, int height, int stride, int[] dequantMatrix, 
           EntropyDecoder entropyDecoder)
   {
      this(width, height, stride, dequantMatrix, entropyDecoder, new BlockCodec(65536));
   }

   
   public IntraDecoder(int width, int height, int stride, int[] dequantMatrix, 
           EntropyDecoder entropyDecoder, BlockCodec blockCodec)
   {
      if ((dequantMatrix != null) && (dequantMatrix.length != 64))
          throw new IllegalArgumentException("Invalid quantization matrix length: "
                  +dequantMatrix.length+" (must be 64)");

      this.width = width;
      this.height = height;
      this.stride = stride;
      this.buffer = new byte[width*height/4]; // TODO: revisit
      this.residueBIS = new ByteArrayInputStream(this.buffer);
      this.residueBS = new DefaultInputBitStream(this.residueBIS, 32768);
      this.blockCodec = (blockCodec != null) ? blockCodec : new BlockCodec(65536);
      this.entropyDecoder = entropyDecoder;
      ResidueBlockDecoder rbd = new ResidueBlockDecoder(this.residueBS);
      this.processor8  = new DecodingProcessor(new IntraPredictor(8, width, height),
              new WHT8(), rbd, dequantMatrix, 8);
      this.processor16 = new DecodingProcessor(new IntraPredictor(16, width, height),
              new WHT16(), rbd, dequantMatrix, 16);
   }


   // Return number of decoded pixels
   public int decode(int[] previous, int[] output, int blkptr)
   {       
      // Block and entropy decode into 'buffer'
      IndexedByteArray iba = new IndexedByteArray(this.buffer, 0);
      int decoded = this.blockCodec.decode(iba, this.entropyDecoder);
      
      if (decoded == -1)
         return -1;
      
      final int h = this.height;
      final int w = this.width;
      final int line8 = this.stride << 3;
      this.processor8.output = output;
      this.processor8.previous = previous;
      this.processor16.output = output;
      this.processor16.previous = previous; 
      final InputBitStream bitstream = this.entropyDecoder.getBitStream();
      int offset = blkptr;

      for (int y=0; y<h; y+=16)
      {
          for (int x=0; x<w; x+=16)
          {
            // Read the next chunk of data from 'buffer' and decode to 'output'
             boolean is8x8Block = (bitstream.readBit() == 0);
             int predictionModeValue = (int) bitstream.readBits(3);

             if (is8x8Block == false)
             {                
                this.processor16.decode(offset+x, predictionModeValue);
             }
             else
             {
                this.processor8.decode(offset+x, predictionModeValue);
                predictionModeValue = (int) bitstream.readBits(3);
                this.processor8.decode(offset+x+8, predictionModeValue);
                predictionModeValue = (int) bitstream.readBits(3);
                this.processor8.decode(offset+x+line8, predictionModeValue);
                predictionModeValue = (int) bitstream.readBits(3);
                this.processor8.decode(offset+x+line8+8, predictionModeValue);
             }
          }
          
          offset += line8;
          offset += line8;
      }
      
      return offset - blkptr;
   }
   
   
   private static class DecodingProcessor
   {
      final IntTransform transform;
      final ResidueBlockDecoder rbd;
      final IntraPredictor predictor;
      final int[] dequantMatrix;
      final int blockDim;
      final int[] block;
      int[] output;
      int[] previous;      
      
      
      DecodingProcessor(IntraPredictor predictor, IntTransform transform, 
              ResidueBlockDecoder rbd, int[] dequantMatrix, int blockDim)
      {
         this.predictor = predictor;
         this.transform = transform;
         this.rbd = rbd;
         this.dequantMatrix = dequantMatrix;
         this.blockDim = blockDim;
         this.block = new int[blockDim*blockDim];
      }
      
      
      // Return number of decoded pixels
      public int decode(int offset, int predictionModeValue)
      {
          // Find best mode prediction
          this.rbd.decode(this.block, 0);
          int[] buf = this.block; //aliasing
          int idx = 0;
          
// TODO, adaptive dequantization based on energy (lower if high energy, higher if low energy)
          // Dequantize the result of the transform
          if (this.dequantMatrix != null)
          {
              int[] matrix = this.dequantMatrix; // aliasing
              
              for (int i=0; i<64; i+=8)
              {
                  buf[idx] *= matrix[i];
                  buf[idx++] >>= 8;
                  buf[idx] *= matrix[i+1];
                  buf[idx++] >>= 8;
                  buf[idx] *= matrix[i+2];
                  buf[idx++] >>= 8;
                  buf[idx] *= matrix[i+3];
                  buf[idx++] >>= 8;
                  buf[idx] *= matrix[i+4];
                  buf[idx++] >>= 8;
                  buf[idx] *= matrix[i+5];
                  buf[idx++] >>= 8;
                  buf[idx] *= matrix[i+6];
                  buf[idx++] >>= 8;
                  buf[idx] *= matrix[i+7];
                  buf[idx++] >>= 8;
              }
          }

          // For 16x16 blocks, high frequency quadrants were reset during encoding
          for (int i=buf.length-1; i>=64; i-=8)
          {
             buf[idx++] = 0;  
             buf[idx++] = 0;  
             buf[idx++] = 0;  
             buf[idx++] = 0;  
             buf[idx++] = 0;  
             buf[idx++] = 0;  
             buf[idx++] = 0;  
             buf[idx++] = 0;  
          }              

          // Apply Hadamard inverse transform from diff block
          this.transform.inverse(buf, 0);
          
          // Add to reference block to get the final block
          this.predictor.computeDifferences(this.block, 0, this.output, offset, 
                  null, 0, predictionModeValue);
          
          return idx;
       }
   }
}
