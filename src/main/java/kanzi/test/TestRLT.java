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

package kanzi.test;

import kanzi.IndexedByteArray;
import kanzi.function.RLT;
import java.util.Arrays;
import java.util.Random;


public class TestRLT
{
    public static void main(String[] args)
    {
        System.out.println("TestRLT");
        byte[] input;
        byte[] output;
        byte[] reverse;
        Random rnd = new Random();

        // Test behavior
        {
           System.out.println("Correctness test");
           for (int ii=0; ii<20; ii++)
           {
              System.out.println("\nTest "+ii);
              int[] arr;

              if (ii == 2)
              {
                 arr = new int[] {
                    0, 1, 2, 2, 2, 2, 7, 9,  9, 16, 16, 16, 1, 3,
                   3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3
                 };
              }
              else if (ii == 1)
              {
                 arr = new int[500];
                 arr[0] = 1;
                 
                 for (int i=1; i<500; i++)
                    arr[i] = 8;
              }
              else if (ii == 0)
              {
                 arr = new int[] { 0, 0, 1, 1, 2, 2, 3, 3 };
              }
              else
              {
                 arr = new int[1024];
                 int idx = 0;

                 while (idx < arr.length)
                 {
                    int len = rnd.nextInt(270);

                    if (len % 3 == 0)
                      len = 1;

                     int val = rnd.nextInt(256);
                     int end = (idx+len) < arr.length ? idx+len : arr.length;

                     for (int j=idx; j<end; j++)
                        arr[j] = val;

                    idx += len;
                    System.out.print(val+" ("+len+") ");
                 }
              }

               int size = arr.length;
               input = new byte[size];
               output = new byte[size];
               reverse = new byte[size];
               IndexedByteArray iba1 = new IndexedByteArray(input, 0);
               IndexedByteArray iba2 = new IndexedByteArray(output, 0);
               IndexedByteArray iba3 = new IndexedByteArray(reverse, 0);
               Arrays.fill(output, (byte) 0xAA);

               for (int i = 0; i < arr.length; i++)
               {
                  input[i] = (byte) (arr[i] & 255);

                  for (int j=arr.length; j<size; j++)
                      input[j] = (byte) (0);
               }

               RLT rlt = new RLT();

               System.out.println("\nOriginal: ");

               for (int i = 0; i < input.length; i++)
               {
                  System.out.print((input[i] & 255) + " ");
               }

               System.out.println("\nCoded: ");

               if (rlt.forward(iba1, iba2) == false)
               {
                  System.out.println("Encoding error");
                  continue;
               }

               if (iba1.index != input.length)
               {
                  System.out.println("No compression (ratio > 1.0), skip reverse");
                  continue;
               }

               //java.util.Arrays.fill(input, (byte) 0);

               for (int i = 0; i < iba2.index; i++)
               {
                  System.out.print((output[i] & 255) + " "); //+"("+Integer.toBinaryString(output[i] & 255)+") ");
               }

               rlt = new RLT(); // Required to reset internal attributes
               iba1.index = 0;
               iba2.index = 0;
               iba3.index = 0;
               rlt.inverse(iba2, iba3);
               System.out.println("\nDecoded: ");

               for (int i = 0; i < reverse.length; i++)
               {
                  System.out.print((reverse[i] & 255) + " ");
               }

               System.out.println();

               for (int i = 0; i < input.length; i++)
               {
                  if (input[i] != reverse[i])
                  {
                     System.out.println("Different (index "+i+": "+input[i]+" - "+reverse[i]+")");
                     System.exit(1);
                  }
               }

               System.out.println("Identical");
               System.out.println();
            }
      }

      // Test speed
      int iter = 50000;
      System.out.println("\n\nSpeed test");
      System.out.println("Iterations: "+iter);
      
      for (int jj=0; jj<3; jj++)
      {
         input = new byte[30000];
         output = new byte[input.length];
         reverse = new byte[input.length];
         IndexedByteArray iba1 = new IndexedByteArray(input, 0);
         IndexedByteArray iba2 = new IndexedByteArray(output, 0);
         IndexedByteArray iba3 = new IndexedByteArray(reverse, 0);

         // Generate random data with runs
         int n = 0;
                 
         while (n < input.length)        
         {
            byte val = (byte) (rnd.nextInt() & 255);
            input[n++] = val;
            int run = rnd.nextInt() & 255;
            run -= 200;
            
            while ((--run > 0) && (n < input.length))       
               input[n++] = val;
         }

         RLT rlt = new RLT();
         long before, after;
         long delta1 = 0;
         long delta2 = 0;

         before = System.nanoTime();

         for (int ii = 0; ii < iter; ii++)
         {
            rlt = new RLT(); // Required to reset internal attributes
            iba1.index = 0;
            iba2.index = 0;
            rlt.forward(iba1, iba2);
         }

         after = System.nanoTime();
         delta1 += (after - before);
         before = System.nanoTime();

         for (int ii = 0; ii < iter; ii++)
         {
            rlt = new RLT(); // Required to reset internal attributes
            iba3.index = 0;
            iba2.index = 0;
            rlt.inverse(iba2, iba3);
         }

         after = System.nanoTime();
         delta2 += (after - before);
         System.out.println();
         int idx = -1;
         
         // Sanity check
         for (int i=0; i<iba1.index; i++)
         {
            if (iba1.array[i] != iba3.array[i])
            {
               idx = i;
               break;
            }
         }
         
         if (idx >= 0)
            System.out.println("Failure at index "+idx+" ("+iba1.array[idx]+"<->"+iba3.array[idx]+")");
         
         System.out.println("RLT encoding [ms]: " + delta1 / 1000000);
         System.out.println("RLT decoding [ms]: " + delta2 / 1000000);
      }
   }
}
