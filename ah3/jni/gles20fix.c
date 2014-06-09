#include <jni.h>
#include <GLES2/gl2.h>

// ===========================================================
// se.sics.ah3.graphics.Core
// ===========================================================

void Java_se_sics_ah3_graphics_Core_glVertexAttribPointer (JNIEnv *env, jclass c, jint index, jint size, jint type, jboolean normalized, jint stride, jint offset) {
	glVertexAttribPointer(index, size, type, normalized, stride, (void*) offset);
}

void Java_se_sics_ah3_graphics_Core_glDrawElements (JNIEnv *env, jclass c, jint mode, jint count, jint type, jint offset) {
	glDrawElements(mode, count, type, (void*) offset);
}