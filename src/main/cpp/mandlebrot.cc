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

#include <stdlib.h>
#include <jni.h>
#include <pthread.h>

#ifdef __cplusplus
extern "C" {
#endif

jint calculateValue(jdouble cA, jdouble cB, jint maxValue) {
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

typedef struct argsForThread {
  jint threadNumber;
  jint *values;
  jint numThreads;
  jdouble aMin;
  jdouble bMin;
  jdouble pixelsPerUnit;
  jint sizeInPixels;
  jint maxValue;
} ArgsForThread;

void *calculateValuesForThread(void *vargp) {
  ArgsForThread *args = (ArgsForThread *)vargp;

  int i = 0;
  for (int y = 0; y < args->sizeInPixels; y++) {
    for (int x = 0; x < args->sizeInPixels; x++) {
      if (args->numThreads == 1 || i % args->numThreads == args->threadNumber) {
        double cA = args->aMin + x / args->pixelsPerUnit;
        double cB = args->bMin + y / args->pixelsPerUnit;
        args->values[i] = calculateValue(cA, cB, args->maxValue);
      }
      i++;
    }
  }
  pthread_exit(NULL);
  return NULL;
}

JNIEXPORT void JNICALL
Java_org_lizlooney_mandlebrot_Mandlebrot_calculatePixelValuesNative(
  JNIEnv* env, jclass clazz,
  jintArray valuesArg, jint numThreads, double aMin, double bMin, jdouble pixelsPerUnit, jint sizeInPixels, jint maxValue) {

  jboolean copy = JNI_FALSE;
  jint* const values = env->GetIntArrayElements(valuesArg, &copy);

  ArgsForThread *args = (ArgsForThread *) malloc(numThreads * sizeof(ArgsForThread));
  pthread_t *threadIds = (pthread_t *) malloc(numThreads * sizeof(pthread_t));

  for (int threadNumber = 0; threadNumber < numThreads; threadNumber++) {
    args[threadNumber].threadNumber = threadNumber;
    args[threadNumber].values = values;
    args[threadNumber].numThreads = numThreads;
    args[threadNumber].aMin = aMin;
    args[threadNumber].bMin = bMin;
    args[threadNumber].pixelsPerUnit = pixelsPerUnit;
    args[threadNumber].sizeInPixels = sizeInPixels;
    args[threadNumber].maxValue = maxValue;

    pthread_create(&threadIds[threadNumber], NULL, calculateValuesForThread, &args[threadNumber]);
  }

  for (int threadNumber = 0; threadNumber < numThreads; threadNumber++) {
    pthread_join(threadIds[threadNumber], NULL);
  }

  free(args);
  free(threadIds);

  env->ReleaseIntArrayElements(valuesArg, values, 0);
}

JNIEXPORT jint JNICALL
Java_org_lizlooney_mandlebrot_Mandlebrot_calculateValueNative(
  JNIEnv* env, jclass clazz,
  jdouble cA, jdouble cB, jint maxValue) {
  return calculateValue(cA, cB, maxValue);
}


#ifdef __cplusplus
}
#endif
