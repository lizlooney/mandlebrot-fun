/*
 * Copyright 2023 Liz Looney
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
   Java_org_lizlooney_mandlebrot_Mandlebrot_calculateValueNative(
   JNIEnv* env, jclass clazz,
   jdouble cA, jdouble cB, jint maxValue) {
   jdouble zA = cA;
   jdouble zB = cB;
   for (jint i = 0; i <= maxValue; i++) {
     jdouble zAzA = zA * zA;
     jdouble zBzB = zB * zB;
     if (zAzA + zBzB >= 4) {
       return i;
     }
     jdouble nextA = zAzA - zBzB + cA;
     zB = 2 * zA * zB + cB;
     zA = nextA;
   }
   return 2147483647; // Integer.MAX_VALUE;
}

#ifdef __cplusplus
}
#endif
