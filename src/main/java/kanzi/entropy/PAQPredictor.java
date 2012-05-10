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


// This class is a port from the code of the dcs-bwt-compressor project
// http://code.google.com/p/dcs-bwt-compressor/(itself based on PAQ coders)

public class PAQPredictor
{
   ///////////////////////// state table ////////////////////////
   // STATE_TABLE[state][0] = next state if bit y is 0, 0 <= state < 256
   // STATE_TABLE[state][1] = next state if bit y is 1
   // STATE_TABLE[state][2] = number of zeros in bit history represented by state
   // STATE_TABLE[state][3] = number of ones represented
   // States represent a bit history within some context.
   // State 0 is the starting state (no bits seen).
   // States 1-30 represent all possible sequences of 1-4 bits.
   // States 31-252 represent a pair of counts, (n0,n1), the number
   //   of 0 and 1 bits respectively.  If n0+n1 < 16 then there are
   //   two states for each pair, depending on if a 0 or 1 was the last
   //   bit seen.
   // If n0 and n1 are too large, then there is no state to represent this
   // pair, so another state with about the same ratio of n0/n1 is substituted.
   // Also, when a bit is observed and the count of the opposite bit is large,
   // then part of this count is discarded to favor newer data over old.

    private static int[][] STATE_TABLE =
    {
       {  1,  2, 0, 0}, {  3,  5, 1, 0}, {  4,  6, 0, 1}, {  7, 10, 2, 0}, // 0-3
       {  8, 12, 1, 1}, {  9, 13, 1, 1}, { 11, 14, 0, 2}, { 15, 19, 3, 0}, // 4-7
       { 16, 23, 2, 1}, { 17, 24, 2, 1}, { 18, 25, 2, 1}, { 20, 27, 1, 2}, // 8-11
       { 21, 28, 1, 2}, { 22, 29, 1, 2}, { 26, 30, 0, 3}, { 31, 33, 4, 0}, // 12-15
       { 32, 35, 3, 1}, { 32, 35, 3, 1}, { 32, 35, 3, 1}, { 32, 35, 3, 1}, // 16-19
       { 34, 37, 2, 2}, { 34, 37, 2, 2}, { 34, 37, 2, 2}, { 34, 37, 2, 2}, // 20-23
       { 34, 37, 2, 2}, { 34, 37, 2, 2}, { 36, 39, 1, 3}, { 36, 39, 1, 3}, // 24-27
       { 36, 39, 1, 3}, { 36, 39, 1, 3}, { 38, 40, 0, 4}, { 41, 43, 5, 0}, // 28-31
       { 42, 45, 4, 1}, { 42, 45, 4, 1}, { 44, 47, 3, 2}, { 44, 47, 3, 2}, // 32-35
       { 46, 49, 2, 3}, { 46, 49, 2, 3}, { 48, 51, 1, 4}, { 48, 51, 1, 4}, // 36-39
       { 50, 52, 0, 5}, { 53, 43, 6, 0}, { 54, 57, 5, 1}, { 54, 57, 5, 1}, // 40-43
       { 56, 59, 4, 2}, { 56, 59, 4, 2}, { 58, 61, 3, 3}, { 58, 61, 3, 3}, // 44-47
       { 60, 63, 2, 4}, { 60, 63, 2, 4}, { 62, 65, 1, 5}, { 62, 65, 1, 5}, // 48-51
       { 50, 66, 0, 6}, { 67, 55, 7, 0}, { 68, 57, 6, 1}, { 68, 57, 6, 1}, // 52-55
       { 70, 73, 5, 2}, { 70, 73, 5, 2}, { 72, 75, 4, 3}, { 72, 75, 4, 3}, // 56-59
       { 74, 77, 3, 4}, { 74, 77, 3, 4}, { 76, 79, 2, 5}, { 76, 79, 2, 5}, // 60-63
       { 62, 81, 1, 6}, { 62, 81, 1, 6}, { 64, 82, 0, 7}, { 83, 69, 8, 0}, // 64-67
       { 84, 71, 7, 1}, { 84, 71, 7, 1}, { 86, 73, 6, 2}, { 86, 73, 6, 2}, // 68-71
       { 44, 59, 5, 3}, { 44, 59, 5, 3}, { 58, 61, 4, 4}, { 58, 61, 4, 4}, // 72-75
       { 60, 49, 3, 5}, { 60, 49, 3, 5}, { 76, 89, 2, 6}, { 76, 89, 2, 6}, // 76-79
       { 78, 91, 1, 7}, { 78, 91, 1, 7}, { 80, 92, 0, 8}, { 93, 69, 9, 0}, // 80-83
       { 94, 87, 8, 1}, { 94, 87, 8, 1}, { 96, 45, 7, 2}, { 96, 45, 7, 2}, // 84-87
       { 48, 99, 2, 7}, { 48, 99, 2, 7}, { 88,101, 1, 8}, { 88,101, 1, 8}, // 88-91
       { 80,102, 0, 9}, {103, 69,10, 0}, {104, 87, 9, 1}, {104, 87, 9, 1}, // 92-95
       {106, 57, 8, 2}, {106, 57, 8, 2}, { 62,109, 2, 8}, { 62,109, 2, 8}, // 96-99
       { 88,111, 1, 9}, { 88,111, 1, 9}, { 80,112, 0,10}, {113, 85,11, 0}, // 100-103
       {114, 87,10, 1}, {114, 87,10, 1}, {116, 57, 9, 2}, {116, 57, 9, 2}, // 104-107
       { 62,119, 2, 9}, { 62,119, 2, 9}, { 88,121, 1,10}, { 88,121, 1,10}, // 108-111
       { 90,122, 0,11}, {123, 85,12, 0}, {124, 97,11, 1}, {124, 97,11, 1}, // 112-115
       {126, 57,10, 2}, {126, 57,10, 2}, { 62,129, 2,10}, { 62,129, 2,10}, // 116-119
       { 98,131, 1,11}, { 98,131, 1,11}, { 90,132, 0,12}, {133, 85,13, 0}, // 120-123
       {134, 97,12, 1}, {134, 97,12, 1}, {136, 57,11, 2}, {136, 57,11, 2}, // 124-127
       { 62,139, 2,11}, { 62,139, 2,11}, { 98,141, 1,12}, { 98,141, 1,12}, // 128-131
       { 90,142, 0,13}, {143, 95,14, 0}, {144, 97,13, 1}, {144, 97,13, 1}, // 132-135
       { 68, 57,12, 2}, { 68, 57,12, 2}, { 62, 81, 2,12}, { 62, 81, 2,12}, // 136-139
       { 98,147, 1,13}, { 98,147, 1,13}, {100,148, 0,14}, {149, 95,15, 0}, // 140-143
       {150,107,14, 1}, {150,107,14, 1}, {108,151, 1,14}, {108,151, 1,14}, // 144-147
       {100,152, 0,15}, {153, 95,16, 0}, {154,107,15, 1}, {108,155, 1,15}, // 148-151
       {100,156, 0,16}, {157, 95,17, 0}, {158,107,16, 1}, {108,159, 1,16}, // 152-155
       {100,160, 0,17}, {161,105,18, 0}, {162,107,17, 1}, {108,163, 1,17}, // 156-159
       {110,164, 0,18}, {165,105,19, 0}, {166,117,18, 1}, {118,167, 1,18}, // 160-163
       {110,168, 0,19}, {169,105,20, 0}, {170,117,19, 1}, {118,171, 1,19}, // 164-167
       {110,172, 0,20}, {173,105,21, 0}, {174,117,20, 1}, {118,175, 1,20}, // 168-171
       {110,176, 0,21}, {177,105,22, 0}, {178,117,21, 1}, {118,179, 1,21}, // 172-175
       {110,180, 0,22}, {181,115,23, 0}, {182,117,22, 1}, {118,183, 1,22}, // 176-179
       {120,184, 0,23}, {185,115,24, 0}, {186,127,23, 1}, {128,187, 1,23}, // 180-183
       {120,188, 0,24}, {189,115,25, 0}, {190,127,24, 1}, {128,191, 1,24}, // 184-187
       {120,192, 0,25}, {193,115,26, 0}, {194,127,25, 1}, {128,195, 1,25}, // 188-191
       {120,196, 0,26}, {197,115,27, 0}, {198,127,26, 1}, {128,199, 1,26}, // 192-195
       {120,200, 0,27}, {201,115,28, 0}, {202,127,27, 1}, {128,203, 1,27}, // 196-199
       {120,204, 0,28}, {205,115,29, 0}, {206,127,28, 1}, {128,207, 1,28}, // 200-203
       {120,208, 0,29}, {209,125,30, 0}, {210,127,29, 1}, {128,211, 1,29}, // 204-207
       {130,212, 0,30}, {213,125,31, 0}, {214,137,30, 1}, {138,215, 1,30}, // 208-211
       {130,216, 0,31}, {217,125,32, 0}, {218,137,31, 1}, {138,219, 1,31}, // 212-215
       {130,220, 0,32}, {221,125,33, 0}, {222,137,32, 1}, {138,223, 1,32}, // 216-219
       {130,224, 0,33}, {225,125,34, 0}, {226,137,33, 1}, {138,227, 1,33}, // 220-223
       {130,228, 0,34}, {229,125,35, 0}, {230,137,34, 1}, {138,231, 1,34}, // 224-227
       {130,232, 0,35}, {233,125,36, 0}, {234,137,35, 1}, {138,235, 1,35}, // 228-231
       {130,236, 0,36}, {237,125,37, 0}, {238,137,36, 1}, {138,239, 1,36}, // 232-235
       {130,240, 0,37}, {241,125,38, 0}, {242,137,37, 1}, {138,243, 1,37}, // 236-239
       {130,244, 0,38}, {245,135,39, 0}, {246,137,38, 1}, {138,247, 1,38}, // 240-243
       {140,248, 0,39}, {249,135,40, 0}, {250, 69,39, 1}, { 80,251, 1,39}, // 244-247
       {140,252, 0,40}, {249,135,41, 0}, {250, 69,40, 1}, { 80,251, 1,40}, // 248-251
       {140,252, 0,41}, {  0,  0, 0, 0}, {  0,  0, 0, 0}, {  0,  0, 0, 0}  // 253-255 are reserved
    };  


    private static int[] INV_EXP =
    {
         1,    2,    3,    6,   10,   16,   27,   45,
        73,  120,  194,  310,  488,  747, 1101, 1546,
      2047, 2549, 2994, 3348, 3607, 3785, 3901, 3975,
      4022, 4050, 4068, 4079, 4085, 4089, 4092, 4093,
      4094
    };


    // Inverse of squash. d = ln(p/(1-p)), d scaled by 8 bits, p by 12 bits.
    // d has range -2047 to 2047 representing -8 to 8.  p has range 0 to 4095.
    private static int[] STRETCH = init();


    private static int[] init()
    {
       int[] res = new int[4096];
       int pi = 0;

       for (int x=-2047; x<=2047; x++)
       {
          final int i = squash(x);

          for (int j=pi; j<=i; j++)
            res[j] = x;

          pi = i + 1;
       }

       res[4095] = 2047;
       return res;
   }


   private int pr;                   // next predicted value (0-4095)
   private int c0;                   // bitwise context: last 0-7 bits with a leading 1 (1-255)
   private int c4;                   // last 4 whole bytes, last is in low 8 bits
   private int bpos;                 // number of bits in c0 (0-7)
   private final int[] states;       // context -> state
   private final StateMap sm;        // state -> pr
   private int ctxPtr;               // context pointer
   private int run;                  // count of consecutive identical bytes (0-65535)
   private int runCtx;               // (0-3) if run is 0, 1, 2-3, 4+
   private final AdaptiveProbMap apm2;
   private final AdaptiveProbMap apm3;
   private final AdaptiveProbMap apm5;
   private final AdaptiveProbMap apm4;


   public PAQPredictor()
   {
     this.pr = 2048;
     this.c0 = 1;
     this.states = new int[256];
     this.apm2 = new AdaptiveProbMap(1024);
     this.apm3 = new AdaptiveProbMap(4);
     this.apm4 = new AdaptiveProbMap(256);
     this.apm5 = new AdaptiveProbMap(2048);
     this.sm = new StateMap();
   }


   public void update(int y)
   {
     // update model
     this.states[this.ctxPtr] = STATE_TABLE[this.states[this.ctxPtr]][y];

     // update context
     this.c0 += (this.c0 + y);

     if (++this.bpos == 8)
     {
        this.bpos = 0;
        this.c4 = (this.c4 << 8) | (this.c0 - 256);
        this.c0 = 1;

        if (((this.c4 ^ (this.c4>>8)) & 255) == 0)
        {
           if (this.run < 0xFFFF)
              this.run++;

           if ((this.run == 1) || (this.run == 2) || (this.run == 4))
              this.runCtx++;
        }
        else
        {
           this.run = 0;
           this.runCtx = 0;
        }
     }

     final int c1c = ((this.c4 & 255) + 256) >> (8 - this.bpos);
     int c1d = (this.c4 >> (7-this.bpos)) & 1;

     if (c1c == this.c0)
        c1d += 2;

     // Prediction chain
     this.ctxPtr = this.c0;
     int pred = this.sm.get(y, this.states[this.ctxPtr]);
     pred = this.apm2.get(y, pred, this.c0 | (c1d<<8), 7);
     pred = this.apm3.get(y, pred, this.runCtx, 8);
     final int ctx = (int) (this.c0 ^ (((this.c4*123456791L) & 0xFFFFFFFFL) >> 21));
     pred = (this.apm5.get(y, pred, ctx, 7) + pred + 1) >> 1;
     this.pr = (this.apm4.get(y, pred, this.c0, 7) + pred + 1) >> 1;
   }


   public int get()
   {
      return (this.pr >= 2048) ? this.pr : this.pr+1;
   }


   // return p = 1/(1 + exp(-d)), d scaled by 8 bits, p scaled by 12 bits
   private static int squash(int d)
   {
      if (d > 2047)
         return 4095;

      if (d < -2047)
         return 0;

      final int w = d & 127;
      d = (d >> 7) + 16;
      return (INV_EXP[d]*(128-w) + INV_EXP[d+1]*w + 64) >> 7;
   }


   //////////////////////////////////////////////////////////////////
   // A StateMap maps a nonstationary counter state to a probability.
   // After each mapping, the mapping is adjusted to improve future
   // predictions.  Methods:
   //
   //  get(y, cx) converts state cx (0-255) to a probability (0-4095),
   //  and trains by updating the previous prediction with y (0-1).
   //
   // Counter state -> probability * 256
   //////////////////////////////////////////////////////////////////
   static class StateMap
   {
      private int ctx;
      private final int[] data;


      StateMap()
      {
         this.data = new int[256];

         for (int i=0; i<256; i++)
         {
            int n0 = STATE_TABLE[i][2];
            int n1 = STATE_TABLE[i][3];

            if (n0 == 0)
               n1 <<= 7;

            if (n1 == 0)
               n0 <<= 7;

            this.data[i] = (int) (((n1+1) << 16) / (n0+n1+2));
         }
      }


      int get(int y, int cx)
      {
         this.data[this.ctx] += (((y<<16) - this.data[this.ctx] + 128) >> 8);
         this.ctx = cx;
         return this.data[cx] >> 4;
      }
   }


   /////////////////////////////////////////////////////////////////
   // APM maps a probability and a context into a new probability
   // that bit y will next be 1.  After each guess it updates
   // its state to improve future guesses.  Methods:
   //
   // APM a(N) creates with N contexts, uses 66*N bytes memory.
   // a.p(y, pr, cx, rate=8) returned adjusted probability in context cx (0 to
   //   N-1).  rate determines the learning rate (smaller = faster, default 8).
   //   Probabilities are scaled 12 bits (0-4095).  Update on last bit y (0-1).
   //////////////////////////////////////////////////////////////////
   static class AdaptiveProbMap
   {
     private int index;        // last p, context
     private final int[] data; // [NbCtx][33]:  p, context -> p


     // maps p, cxt -> p initially
     AdaptiveProbMap(int n)
     {
        this.data = new int[n*33];

        for (int i=0, k=0; i<n; i++, k+=33)
        {
           for (int j=0; j<33; j++)
              this.data[k+j] = (i == 0) ? (int) (squash((j-16) << 7) << 4) : this.data[j];
        }
     }


     int get(int y, int pr, int ctx, int rate)
     {
        pr = STRETCH[pr];
        final int g = (y<<16) + (y<<rate) - (y<<1);
        this.data[this.index] += ((g-this.data[this.index]) >> rate);
        this.data[this.index+1] += ((g-this.data[this.index+1]) >> rate);
        final int w = pr & 127;  // interpolation weight (33 points)
        this.index = ((pr+2048) >> 7) + (ctx*33);
        return (this.data[this.index]*(128-w) + this.data[this.index+1]*w) >> 11;
     }
   }

}
