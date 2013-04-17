#ifdef ANDROID_BUILD
#include <dlfcn.h>
#endif
#include <SkBitmap.h>
#include <SkCanvas.h>
#include <SkImageDecoder.h>
#include <SkImageEncoder.h>
#include <SkStream.h>
#include "java_renderRules.h"
#include "common.h"
#include "java_wrap.h"
#include "binaryRead.h"
#include "rendering.h"
#include "binaryRoutePlanner.h"


JavaVM* globalJVM = NULL;
void loadJniRenderingContext(JNIEnv* env);
void loadJniRenderingRules(JNIEnv* env);
jclass jclassIntArray ;
jclass jclassString;
jmethodID jmethod_Object_toString = NULL;

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv* globalJniEnv;
	if(vm->GetEnv((void **)&globalJniEnv, JNI_VERSION_1_6))
		return JNI_ERR; /* JNI version not supported */
	globalJVM = vm;
	loadJniRenderingContext(globalJniEnv);
	loadJniRenderingRules(globalJniEnv);
	jclassIntArray = findClass(globalJniEnv, "[I");
	jclassString = findClass(globalJniEnv, "java/lang/String");

	osmand_log_print(LOG_INFO, "JNI_OnLoad completed");
	
	return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL Java_net_osmand_NativeLibrary_deleteSearchResult(JNIEnv* ienv,
		jobject obj, jlong searchResult) {
	ResultPublisher* result = (ResultPublisher*) searchResult;
	if(result != NULL){
		delete result;
	}
}


extern "C" JNIEXPORT void JNICALL Java_net_osmand_NativeLibrary_closeBinaryMapFile(JNIEnv* ienv,
		jobject obj, jobject path) {
	const char* utf = ienv->GetStringUTFChars((jstring) path, NULL);
	std::string inputName(utf);
	ienv->ReleaseStringUTFChars((jstring) path, utf);
	closeBinaryMapFile(inputName);
}


extern "C" JNIEXPORT jboolean JNICALL Java_net_osmand_NativeLibrary_initCacheMapFiles(JNIEnv* ienv,
		jobject obj,
		jobject path) {
	const char* utf = ienv->GetStringUTFChars((jstring) path, NULL);
	std::string inputName(utf);
	ienv->ReleaseStringUTFChars((jstring) path, utf);
	return initMapFilesFromCache(inputName);
}

extern "C" JNIEXPORT jboolean JNICALL Java_net_osmand_NativeLibrary_initBinaryMapFile(JNIEnv* ienv,
		jobject obj, jobject path) {
	// Verify that the version of the library that we linked against is
	const char* utf = ienv->GetStringUTFChars((jstring) path, NULL);
	std::string inputName(utf);
	ienv->ReleaseStringUTFChars((jstring) path, utf);
	BinaryMapFile* fl = initBinaryMapFile(inputName);
	if(fl == NULL) {
		osmand_log_print(LOG_WARN, "File %s was not initialized", inputName.c_str());
	}
	return fl != NULL;
}


// Global object
UNORDERED(map)<std::string, RenderingRulesStorage*> cachedStorages;

RenderingRulesStorage* getStorage(JNIEnv* env, jobject storage) {
	std::string hash = getStringMethod(env, storage, jmethod_Object_toString);
	if (cachedStorages.find(hash) == cachedStorages.end()) {
		osmand_log_print(LOG_DEBUG, "Init rendering storage %s ", hash.c_str());
		cachedStorages[hash] = createRenderingRulesStorage(env, storage);
	}
	return cachedStorages[hash];
}


extern "C" JNIEXPORT void JNICALL Java_net_osmand_NativeLibrary_initRenderingRulesStorage(JNIEnv* ienv,
		jobject obj, jobject storage) {
	getStorage(ienv, storage);
}

RenderingRuleSearchRequest* initSearchRequest(JNIEnv* env, jobject renderingRuleSearchRequest) {
	jobject storage = env->GetObjectField(renderingRuleSearchRequest, RenderingRuleSearchRequest_storage);
	RenderingRulesStorage* st = getStorage(env, storage);
	env->DeleteLocalRef(storage);
	RenderingRuleSearchRequest* res = new RenderingRuleSearchRequest(st);
	initRenderingRuleSearchRequest(env, res, renderingRuleSearchRequest);
	return res;
}


extern "C" JNIEXPORT jlong JNICALL Java_net_osmand_NativeLibrary_searchNativeObjectsForRendering(JNIEnv* ienv,
		jobject obj, jint sleft, jint sright, jint stop, jint sbottom, jint zoom,
		jobject renderingRuleSearchRequest, bool skipDuplicates, int renderRouteDataFile,
		jobject objInterrupted, jstring msgNothingFound) {
	RenderingRuleSearchRequest* req = initSearchRequest(ienv, renderingRuleSearchRequest);
	jfieldID interruptedField = 0;
	if(objInterrupted != NULL) {
		jclass clObjInterrupted = ienv->GetObjectClass(objInterrupted);
		interruptedField = getFid(ienv, clObjInterrupted, "interrupted", "Z");
		ienv->DeleteLocalRef(clObjInterrupted);
	}

	ResultJNIPublisher* j = new ResultJNIPublisher( objInterrupted, interruptedField, ienv);
	SearchQuery q(sleft, sright, stop, sbottom, req, j);
	q.zoom = zoom;


	ResultPublisher* res = searchObjectsForRendering(&q, skipDuplicates, renderRouteDataFile, getString(ienv, msgNothingFound));
	delete req;
	return (jlong) j;
}


//////////////////////////////////////////
///////////// JNI RENDERING //////////////
void fillRenderingAttributes(JNIRenderingContext& rc, RenderingRuleSearchRequest* req) {
	req->clearState();
	req->setIntFilter(req->props()->R_MINZOOM, rc.getZoom());
	if (req->searchRenderingAttribute("defaultColor")) {
		rc.setDefaultColor(req->getIntPropertyValue(req->props()->R_ATTR_COLOR_VALUE));
	}
	req->clearState();
	req->setIntFilter(req->props()->R_MINZOOM, rc.getZoom());
	if (req->searchRenderingAttribute("shadowRendering")) {
		rc.setShadowRenderingMode(req->getIntPropertyValue(req->props()->R_ATTR_INT_VALUE));
		rc.setShadowRenderingColor(req->getIntPropertyValue(req->props()->R_SHADOW_COLOR));
	}
	req->clearState();
	req->setIntFilter(req->props()->R_MINZOOM, rc.getZoom());
	if (req->searchRenderingAttribute("polygonMinSizeToDisplay")) {
		rc.polygonMinSizeToDisplay = req->getIntPropertyValue(req->props()->R_ATTR_INT_VALUE);
	}
	req->clearState();
	req->setIntFilter(req->props()->R_MINZOOM, rc.getZoom());
	if (req->searchRenderingAttribute("roadDensityZoomTile")) {
		rc.roadDensityZoomTile = req->getIntPropertyValue(req->props()->R_ATTR_INT_VALUE);
	}
	req->clearState();
	req->setIntFilter(req->props()->R_MINZOOM, rc.getZoom());
	if (req->searchRenderingAttribute("roadsDensityLimitPerTile")) {
		rc.roadsDensityLimitPerTile = req->getIntPropertyValue(req->props()->R_ATTR_INT_VALUE);
	}
}

#ifdef ANDROID_BUILD
#include <android/bitmap.h>

extern "C" JNIEXPORT jobject JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_generateRenderingDirect( JNIEnv* ienv, jobject obj,
    jobject renderingContext, jlong searchResult, jobject targetBitmap, jobject renderingRuleSearchRequest) {

	// libJniGraphics interface
	typedef int (*PTR_AndroidBitmap_getInfo)(JNIEnv*, jobject, AndroidBitmapInfo*);
	typedef int (*PTR_AndroidBitmap_lockPixels)(JNIEnv*, jobject, void**);
	typedef int (*PTR_AndroidBitmap_unlockPixels)(JNIEnv*, jobject);
	static PTR_AndroidBitmap_getInfo dl_AndroidBitmap_getInfo = 0;
	static PTR_AndroidBitmap_lockPixels dl_AndroidBitmap_lockPixels = 0;
	static PTR_AndroidBitmap_unlockPixels dl_AndroidBitmap_unlockPixels = 0;
	static void* module_libjnigraphics = 0;

	if(!module_libjnigraphics)
	{
		module_libjnigraphics = dlopen("jnigraphics", /*RTLD_NOLOAD*/0x0004);
		if(!module_libjnigraphics) {
			osmand_log_print(LOG_WARN, "jnigraphics was not found in loaded libraries");
			module_libjnigraphics = dlopen("jnigraphics", RTLD_NOW);
		}
		if(!module_libjnigraphics) {
			osmand_log_print(LOG_WARN, "jnigraphics was not loaded in default location");
			module_libjnigraphics = dlopen("/system/lib/libjnigraphics.so", RTLD_NOW);
		}
		if(!module_libjnigraphics)
		{
			osmand_log_print(LOG_ERROR, "Failed to load jnigraphics via dlopen, will going to crash");
			return NULL;
		}
		dl_AndroidBitmap_getInfo = (PTR_AndroidBitmap_getInfo)dlsym(module_libjnigraphics, "AndroidBitmap_getInfo");
		dl_AndroidBitmap_lockPixels = (PTR_AndroidBitmap_lockPixels)dlsym(module_libjnigraphics, "AndroidBitmap_lockPixels");
		dl_AndroidBitmap_unlockPixels = (PTR_AndroidBitmap_unlockPixels)dlsym(module_libjnigraphics, "AndroidBitmap_unlockPixels");
	}

	// Gain information about bitmap
	AndroidBitmapInfo bitmapInfo;
	if(dl_AndroidBitmap_getInfo(ienv, targetBitmap, &bitmapInfo) != ANDROID_BITMAP_RESUT_SUCCESS)
	osmand_log_print(LOG_ERROR, "Failed to execute AndroidBitmap_getInfo");

	osmand_log_print(LOG_INFO, "Creating SkBitmap in native w:%d h:%d s:%d f:%d!", bitmapInfo.width, bitmapInfo.height, bitmapInfo.stride, bitmapInfo.format);

	SkBitmap* bitmap = new SkBitmap();
	if(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
		int rowBytes = bitmapInfo.stride;
		osmand_log_print(LOG_INFO, "Row bytes for RGBA_8888 is %d", rowBytes);
		bitmap->setConfig(SkBitmap::kARGB_8888_Config, bitmapInfo.width, bitmapInfo.height, rowBytes);
	} else if(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565) {
		int rowBytes = bitmapInfo.stride;
		osmand_log_print(LOG_INFO, "Row bytes for RGB_565 is %d", rowBytes);
		bitmap->setConfig(SkBitmap::kRGB_565_Config, bitmapInfo.width, bitmapInfo.height, rowBytes);
	} else {
		osmand_log_print(LOG_ERROR, "Unknown target bitmap format");
	}

	void* lockedBitmapData = NULL;
	if(dl_AndroidBitmap_lockPixels(ienv, targetBitmap, &lockedBitmapData) != ANDROID_BITMAP_RESUT_SUCCESS || !lockedBitmapData) {
		osmand_log_print(LOG_ERROR, "Failed to execute AndroidBitmap_lockPixels");
	}
	osmand_log_print(LOG_INFO, "Locked %d bytes at %p", bitmap->getSize(), lockedBitmapData);

	bitmap->setPixels(lockedBitmapData);

	osmand_log_print(LOG_INFO, "Initializing rendering");
	ElapsedTimer initObjects;
	initObjects.start();

	RenderingRuleSearchRequest* req = initSearchRequest(ienv, renderingRuleSearchRequest);
	JNIRenderingContext rc;
	pullFromJavaRenderingContext(ienv, renderingContext, &rc);
	ResultPublisher* result = ((ResultPublisher*) searchResult);
	fillRenderingAttributes(rc, req);
	osmand_log_print(LOG_INFO, "Rendering image");
	initObjects.pause();


	// Main part do rendering
	rc.nativeOperations.start();
	SkCanvas* canvas = new SkCanvas(*bitmap);
	canvas->drawColor(rc.getDefaultColor());
	if(result != NULL) {
		doRendering(result->result, canvas, req, &rc);
	}

	rc.nativeOperations.pause();

	pushToJavaRenderingContext(ienv, renderingContext, &rc);
	osmand_log_print(LOG_INFO, "End Rendering image");
	if(dl_AndroidBitmap_unlockPixels(ienv, targetBitmap) != ANDROID_BITMAP_RESUT_SUCCESS) {
		osmand_log_print(LOG_ERROR, "Failed to execute AndroidBitmap_unlockPixels");
	}

	// delete  variables
	delete canvas;
	delete req;
	delete bitmap;
	//    deleteObjects(mapDataObjects);

	jclass resultClass = findClass(ienv, "net/osmand/NativeLibrary$RenderingGenerationResult");

	jmethodID resultClassCtorId = ienv->GetMethodID(resultClass, "<init>", "(Ljava/nio/ByteBuffer;)V");

#ifdef DEBUG_NAT_OPERATIONS
	osmand_log_print(LOG_INFO, LOG_TAG,"Native ok (init %d, native op %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#else
	osmand_log_print(LOG_INFO, "Native ok (init %d, rendering %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#endif

	/* Construct a result object */
	jobject resultObject = ienv->NewObject(resultClass, resultClassCtorId, NULL);

	return resultObject;
}
#endif


void* bitmapData = NULL;
size_t bitmapDataSize = 0;
extern "C" JNIEXPORT jobject JNICALL Java_net_osmand_NativeLibrary_generateRenderingIndirect( JNIEnv* ienv,
		jobject obj, jobject renderingContext, jlong searchResult, jboolean isTransparent,
		jobject renderingRuleSearchRequest, jboolean encodePNG) {

	JNIRenderingContext rc;
	pullFromJavaRenderingContext(ienv, renderingContext, &rc);

	osmand_log_print(LOG_INFO, "Creating SkBitmap in native w:%d h:%d!", rc.getWidth(), rc.getHeight());

	SkBitmap* bitmap = new SkBitmap();
	if (isTransparent == JNI_TRUE)
		bitmap->setConfig(SkBitmap::kARGB_8888_Config, rc.getWidth(), rc.getHeight(), 0);
	else
		bitmap->setConfig(SkBitmap::kRGB_565_Config, rc.getWidth(), rc.getHeight(), 0);

	if (bitmapData != NULL && bitmapDataSize != bitmap->getSize()) {
		free(bitmapData);
		bitmapData = NULL;
		bitmapDataSize = 0;
	}
	if (bitmapData == NULL && bitmapDataSize == 0) {
		bitmapDataSize = bitmap->getSize();
		bitmapData = malloc(bitmapDataSize);
		osmand_log_print(LOG_INFO, "Allocated %d bytes at %p", bitmapDataSize, bitmapData);
	}

	bitmap->setPixels(bitmapData);
	ElapsedTimer initObjects;
	initObjects.start();

	RenderingRuleSearchRequest* req = initSearchRequest(ienv, renderingRuleSearchRequest);

	ResultPublisher* result = ((ResultPublisher*) searchResult);
	//    std::vector <BaseMapDataObject* > mapDataObjects = marshalObjects(binaryMapDataObjects);

	initObjects.pause();
	// Main part do rendering
	fillRenderingAttributes(rc, req);


	SkCanvas* canvas = new SkCanvas(*bitmap);
	canvas->drawColor(rc.getDefaultColor());
	if (result != NULL) {
		doRendering(result->result, canvas, req, &rc);
	}
	pushToJavaRenderingContext(ienv, renderingContext, &rc);

	jclass resultClass = findClass(ienv, "net/osmand/NativeLibrary$RenderingGenerationResult");

	jmethodID resultClassCtorId = ienv->GetMethodID(resultClass, "<init>", "(Ljava/nio/ByteBuffer;)V");

#ifdef DEBUG_NAT_OPERATIONS
	osmand_log_print(LOG_INFO, "Native ok (init %d, native op %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#else
	osmand_log_print(LOG_INFO, "Native ok (init %d, rendering %d) ", initObjects.getElapsedTime(),
			rc.nativeOperations.getElapsedTime());
#endif
	// Allocate ctor paramters
	jobject bitmapBuffer;
	if(encodePNG) {
		SkImageEncoder* enc = SkImageEncoder::Create(SkImageEncoder::kPNG_Type);
		SkDynamicMemoryWStream* stream = new SkDynamicMemoryWStream();
		enc->encodeStream(stream, *bitmap, 80);
		// clean previous data
		free(bitmapData);
		bitmapDataSize = stream->bytesWritten();
		bitmapData = malloc(bitmapDataSize);

		stream->copyTo(bitmapData);
		delete enc;
	}
	bitmapBuffer = ienv->NewDirectByteBuffer(bitmapData, bitmapDataSize);

	// delete  variables
	delete canvas;
	delete req;
	delete bitmap;

	fflush(stdout);

	/* Construct a result object */
	jobject resultObject = ienv->NewObject(resultClass, resultClassCtorId, bitmapBuffer);

	return resultObject;
}


///////////////////////////////////////////////
//////////  JNI Rendering Context //////////////

jclass jclass_JUnidecode;
jclass jclass_Reshaper;
jmethodID jmethod_JUnidecode_unidecode;
jmethodID jmethod_Reshaper_reshape;
jclass jclass_RouteCalculationProgress = NULL;
jfieldID jfield_RouteCalculationProgress_segmentNotFound = NULL;
jfieldID jfield_RouteCalculationProgress_distanceFromBegin = NULL;
jfieldID jfield_RouteCalculationProgress_directSegmentQueueSize = NULL;
jfieldID jfield_RouteCalculationProgress_distanceFromEnd = NULL;
jfieldID jfield_RouteCalculationProgress_reverseSegmentQueueSize = NULL;
jfieldID jfield_RouteCalculationProgress_isCancelled = NULL;

jclass jclass_RenderingContext = NULL;
jfieldID jfield_RenderingContext_interrupted = NULL;
jfieldID jfield_RenderingContext_leftX = NULL;
jfieldID jfield_RenderingContext_topY = NULL;
jfieldID jfield_RenderingContext_width = NULL;
jfieldID jfield_RenderingContext_height = NULL;
jfieldID jfield_RenderingContext_zoom = NULL;
jfieldID jfield_RenderingContext_tileDivisor = NULL;
jfieldID jfield_RenderingContext_rotate = NULL;
jfieldID jfield_RenderingContext_useEnglishNames = NULL;
jfieldID jfield_RenderingContext_pointCount = NULL;
jfieldID jfield_RenderingContext_pointInsideCount = NULL;
jfieldID jfield_RenderingContext_visible = NULL;
jfieldID jfield_RenderingContext_allObjects = NULL;
jfieldID jfield_RenderingContext_density = NULL;
jfieldID jfield_RenderingContext_shadowRenderingMode = NULL;
jfieldID jfield_RenderingContext_shadowRenderingColor = NULL;
jfieldID jfield_RenderingContext_defaultColor = NULL;
jfieldID jfield_RenderingContext_textRenderingTime = NULL;
jfieldID jfield_RenderingContext_lastRenderedKey = NULL;

jmethodID jmethod_RenderingContext_getIconRawData = NULL;

jclass jclass_RouteDataObject = NULL;
jfieldID jfield_RouteDataObject_types = NULL;
jfieldID jfield_RouteDataObject_pointsX = NULL;
jfieldID jfield_RouteDataObject_pointsY = NULL;
jfieldID jfield_RouteDataObject_restrictions = NULL;
jfieldID jfield_RouteDataObject_pointTypes = NULL;
jfieldID jfield_RouteDataObject_id = NULL;
jmethodID jmethod_RouteDataObject_init = NULL;

jclass jclass_NativeRouteSearchResult = NULL;
jmethodID jmethod_NativeRouteSearchResult_init = NULL;


jclass jclass_RouteSubregion = NULL;
jfieldID jfield_RouteSubregion_length = NULL;
jfieldID jfield_RouteSubregion_filePointer= NULL;
jfieldID jfield_RouteSubregion_left = NULL;
jfieldID jfield_RouteSubregion_right = NULL;
jfieldID jfield_RouteSubregion_top = NULL;
jfieldID jfield_RouteSubregion_bottom = NULL;
jfieldID jfield_RouteSubregion_shiftToData = NULL;

jclass jclass_RouteRegion = NULL;
jfieldID jfield_RouteRegion_length = NULL;
jfieldID jfield_RouteRegion_filePointer= NULL;

jclass jclass_RouteSegmentResult = NULL;
jclass jclass_RouteSegmentResultAr = NULL;
jmethodID jmethod_RouteSegmentResult_ctor = NULL;
jfieldID jfield_RouteSegmentResult_preAttachedRoutes = NULL;



void loadJniRenderingContext(JNIEnv* env)
{
	jclass_RouteSegmentResult = findClass(env, "net/osmand/router/RouteSegmentResult");
	jclass_RouteSegmentResultAr = findClass(env, "[Lnet/osmand/router/RouteSegmentResult;");
	jmethod_RouteSegmentResult_ctor = env->GetMethodID(jclass_RouteSegmentResult,
			"<init>", "(Lnet/osmand/binary/RouteDataObject;II)V");
	jfield_RouteSegmentResult_preAttachedRoutes = getFid(env, jclass_RouteSegmentResult, "preAttachedRoutes",
			"[[Lnet/osmand/router/RouteSegmentResult;");

	jclass_RouteCalculationProgress = findClass(env, "net/osmand/router/RouteCalculationProgress");
	jfield_RouteCalculationProgress_isCancelled  = getFid(env, jclass_RouteCalculationProgress, "isCancelled", "Z");
	jfield_RouteCalculationProgress_segmentNotFound  = getFid(env, jclass_RouteCalculationProgress, "segmentNotFound", "I");
	jfield_RouteCalculationProgress_distanceFromBegin  = getFid(env, jclass_RouteCalculationProgress, "distanceFromBegin", "F");
	jfield_RouteCalculationProgress_distanceFromEnd  = getFid(env, jclass_RouteCalculationProgress, "distanceFromEnd", "F");
	jfield_RouteCalculationProgress_directSegmentQueueSize  = getFid(env, jclass_RouteCalculationProgress, "directSegmentQueueSize", "I");
	jfield_RouteCalculationProgress_reverseSegmentQueueSize  = getFid(env, jclass_RouteCalculationProgress, "reverseSegmentQueueSize", "I");

	jclass_RenderingContext = findClass(env, "net/osmand/RenderingContext");
	jfield_RenderingContext_interrupted = getFid(env, jclass_RenderingContext, "interrupted", "Z");
	jfield_RenderingContext_leftX = getFid(env,  jclass_RenderingContext, "leftX", "F" );
	jfield_RenderingContext_topY = getFid(env,  jclass_RenderingContext, "topY", "F" );
	jfield_RenderingContext_width = getFid(env,  jclass_RenderingContext, "width", "I" );
	jfield_RenderingContext_height = getFid(env,  jclass_RenderingContext, "height", "I" );
	jfield_RenderingContext_zoom = getFid(env,  jclass_RenderingContext, "zoom", "I" );
	jfield_RenderingContext_tileDivisor = getFid(env,  jclass_RenderingContext, "tileDivisor", "F" );
	jfield_RenderingContext_rotate = getFid(env,  jclass_RenderingContext, "rotate", "F" );
	jfield_RenderingContext_useEnglishNames = getFid(env,  jclass_RenderingContext, "useEnglishNames", "Z" );
	jfield_RenderingContext_pointCount = getFid(env,  jclass_RenderingContext, "pointCount", "I" );
	jfield_RenderingContext_pointInsideCount = getFid(env,  jclass_RenderingContext, "pointInsideCount", "I" );
	jfield_RenderingContext_visible = getFid(env,  jclass_RenderingContext, "visible", "I" );
	jfield_RenderingContext_allObjects = getFid(env,  jclass_RenderingContext, "allObjects", "I" );
	jfield_RenderingContext_density = getFid(env,  jclass_RenderingContext, "density", "F" );
	jfield_RenderingContext_shadowRenderingMode = getFid(env,  jclass_RenderingContext, "shadowRenderingMode", "I" );
	jfield_RenderingContext_shadowRenderingColor = getFid(env,  jclass_RenderingContext, "shadowRenderingColor", "I" );
	jfield_RenderingContext_defaultColor = getFid(env,  jclass_RenderingContext, "defaultColor", "I" );
	jfield_RenderingContext_textRenderingTime = getFid(env,  jclass_RenderingContext, "textRenderingTime", "I" );
	jfield_RenderingContext_lastRenderedKey = getFid(env,  jclass_RenderingContext, "lastRenderedKey", "I" );
	jmethod_RenderingContext_getIconRawData = env->GetMethodID(jclass_RenderingContext,
				"getIconRawData", "(Ljava/lang/String;)[B");

	jmethod_Object_toString = env->GetMethodID(findClass(env, "java/lang/Object", true),
					"toString", "()Ljava/lang/String;");


	jclass_JUnidecode = findClass(env, "net/sf/junidecode/Junidecode");
    jmethod_JUnidecode_unidecode = env->GetStaticMethodID(jclass_JUnidecode, "unidecode", "(Ljava/lang/String;)Ljava/lang/String;");
    jclass_Reshaper = findClass(env, "net/osmand/Reshaper");
    jmethod_Reshaper_reshape = env->GetStaticMethodID(jclass_Reshaper, "reshape", "(Ljava/lang/String;)Ljava/lang/String;");

    jclass_RouteDataObject = findClass(env, "net/osmand/binary/RouteDataObject");
    jclass_NativeRouteSearchResult = findClass(env, "net/osmand/NativeLibrary$NativeRouteSearchResult");
    jmethod_NativeRouteSearchResult_init = env->GetMethodID(jclass_NativeRouteSearchResult, "<init>", "(J[Lnet/osmand/binary/RouteDataObject;)V");

    jfield_RouteDataObject_types = getFid(env,  jclass_RouteDataObject, "types", "[I" );
    jfield_RouteDataObject_pointsX = getFid(env,  jclass_RouteDataObject, "pointsX", "[I" );
    jfield_RouteDataObject_pointsY = getFid(env,  jclass_RouteDataObject, "pointsY", "[I" );
    jfield_RouteDataObject_restrictions = getFid(env,  jclass_RouteDataObject, "restrictions", "[J" );
    jfield_RouteDataObject_pointTypes = getFid(env,  jclass_RouteDataObject, "pointTypes", "[[I" );
    jfield_RouteDataObject_id = getFid(env,  jclass_RouteDataObject, "id", "J" );
    jmethod_RouteDataObject_init = env->GetMethodID(jclass_RouteDataObject, "<init>", "(Lnet/osmand/binary/BinaryMapRouteReaderAdapter$RouteRegion;[I[Ljava/lang/String;)V");


    jclass_RouteRegion = findClass(env, "net/osmand/binary/BinaryMapRouteReaderAdapter$RouteRegion");
    jfield_RouteRegion_length= getFid(env,  jclass_RouteRegion, "length", "I" );
    jfield_RouteRegion_filePointer= getFid(env,  jclass_RouteRegion, "filePointer", "I" );

    jclass_RouteSubregion = findClass(env, "net/osmand/binary/BinaryMapRouteReaderAdapter$RouteSubregion");
    jfield_RouteSubregion_length= getFid(env,  jclass_RouteSubregion, "length", "I" );
    jfield_RouteSubregion_filePointer= getFid(env,  jclass_RouteSubregion, "filePointer", "I" );
    jfield_RouteSubregion_left= getFid(env,  jclass_RouteSubregion, "left", "I" );
    jfield_RouteSubregion_right= getFid(env,  jclass_RouteSubregion, "right", "I" );
    jfield_RouteSubregion_top= getFid(env,  jclass_RouteSubregion, "top", "I" );
    jfield_RouteSubregion_bottom= getFid(env,  jclass_RouteSubregion, "bottom", "I" );
    jfield_RouteSubregion_shiftToData= getFid(env,  jclass_RouteSubregion, "shiftToData", "I" );
	// public final RouteRegion routeReg;

}

void pullFromJavaRenderingContext(JNIEnv* env, jobject jrc, JNIRenderingContext* rc)
{
	rc->env = env;
	rc->setLocation(env->GetFloatField( jrc, jfield_RenderingContext_leftX ), env->GetFloatField( jrc, jfield_RenderingContext_topY ));
	rc->setDimension(env->GetIntField( jrc, jfield_RenderingContext_width ), env->GetIntField( jrc, jfield_RenderingContext_height ));

	rc->setZoom(env->GetIntField( jrc, jfield_RenderingContext_zoom ));
	rc->setTileDivisor(env->GetFloatField( jrc, jfield_RenderingContext_tileDivisor ));
	rc->setRotate(env->GetFloatField( jrc, jfield_RenderingContext_rotate ));
	rc->setDensityScale(env->GetFloatField( jrc, jfield_RenderingContext_density ));
	rc->setUseEnglishNames(env->GetBooleanField( jrc, jfield_RenderingContext_useEnglishNames ));
	rc->javaRenderingContext = jrc;
}


// ElapsedTimer routingTimer;

jobject convertRouteDataObjectToJava(JNIEnv* ienv, RouteDataObject* route, jobject reg) {
	jintArray nameInts = ienv->NewIntArray(route->names.size());
	jobjectArray nameStrings = ienv->NewObjectArray(route->names.size(), jclassString, NULL);
	jint ar[route->names.size()];
	UNORDERED(map)<int, std::string >::iterator itNames = route->names.begin();
	jsize sz = 0;
	for (; itNames != route->names.end(); itNames++, sz++) {
		std::string name = itNames->second;
		jstring js = ienv->NewStringUTF(name.c_str());
		ienv->SetObjectArrayElement(nameStrings, sz, js);
		ienv->DeleteLocalRef(js);
		ar[sz] = itNames->first;
	}
	ienv->SetIntArrayRegion(nameInts, 0, route->names.size(), ar);
	jobject robj = ienv->NewObject(jclass_RouteDataObject, jmethod_RouteDataObject_init, reg, nameInts, nameStrings);
	ienv->DeleteLocalRef(nameInts);
	ienv->DeleteLocalRef(nameStrings);

	ienv->SetLongField(robj, jfield_RouteDataObject_id, route->id);

	jintArray types = ienv->NewIntArray(route->types.size());
	if (route->types.size() > 0) {
		ienv->SetIntArrayRegion(types, 0, route->types.size(), (jint*) &route->types[0]);
	}
	ienv->SetObjectField(robj, jfield_RouteDataObject_types, types);
	ienv->DeleteLocalRef(types);

	jintArray pointsX = ienv->NewIntArray(route->pointsX.size());
	if (route->pointsX.size() > 0) {
		ienv->SetIntArrayRegion(pointsX, 0, route->pointsX.size(), (jint*) &route->pointsX[0]);
	}
	ienv->SetObjectField(robj, jfield_RouteDataObject_pointsX, pointsX);
	ienv->DeleteLocalRef(pointsX);

	jintArray pointsY = ienv->NewIntArray(route->pointsY.size());
	if (route->pointsY.size() > 0) {
		ienv->SetIntArrayRegion(pointsY, 0, route->pointsY.size(), (jint*) &route->pointsY[0]);
	}
	ienv->SetObjectField(robj, jfield_RouteDataObject_pointsY, pointsY);
	ienv->DeleteLocalRef(pointsY);

	jlongArray restrictions = ienv->NewLongArray(route->restrictions.size());
	if (route->restrictions.size() > 0) {
		ienv->SetLongArrayRegion(restrictions, 0, route->restrictions.size(), (jlong*) &route->restrictions[0]);
	}
	ienv->SetObjectField(robj, jfield_RouteDataObject_restrictions, restrictions);
	ienv->DeleteLocalRef(restrictions);

	jobjectArray pointTypes = ienv->NewObjectArray(route->pointTypes.size(), jclassIntArray, NULL);
	for (jint k = 0; k < route->pointTypes.size(); k++) {
		std::vector<uint32_t> ts = route->pointTypes[k];
		if (ts.size() > 0) {
			jintArray tos = ienv->NewIntArray(ts.size());
			ienv->SetIntArrayRegion(tos, 0, ts.size(), (jint*) &ts[0]);
			ienv->SetObjectArrayElement(pointTypes, k, tos);
			ienv->DeleteLocalRef(tos);
		}
	}

	ienv->SetObjectField(robj, jfield_RouteDataObject_pointTypes, pointTypes);
	ienv->DeleteLocalRef(pointTypes);
	return robj;
}

jobject convertRouteSegmentResultToJava(JNIEnv* ienv, RouteSegmentResult& r, UNORDERED(map)<int64_t, int>& indexes,
		jobjectArray regions) {
	RouteDataObject* rdo = r.object.get();
	jobject reg = NULL;
	int64_t fp = rdo->region->filePointer;
	int64_t ln = rdo->region->length;
	if(indexes.find((fp <<31) + ln) != indexes.end()) {
		reg = ienv->GetObjectArrayElement(regions, indexes[(fp <<31) + ln]);
	}
	jobjectArray ar = ienv->NewObjectArray(r.attachedRoutes.size(), jclass_RouteSegmentResultAr, NULL);
	for(jsize k = 0; k < r.attachedRoutes.size(); k++) {
		jobjectArray art = ienv->NewObjectArray(r.attachedRoutes[k].size(), jclass_RouteSegmentResult, NULL);
		for(jsize kj = 0; kj < r.attachedRoutes[k].size(); kj++) {
			jobject jo = convertRouteSegmentResultToJava(ienv, r.attachedRoutes[k][kj], indexes, regions);
			ienv->SetObjectArrayElement(art, kj, jo);
			ienv->DeleteLocalRef(jo);
		}
		ienv->SetObjectArrayElement(ar, k, art);
		ienv->DeleteLocalRef(art);
	}
	jobject robj = convertRouteDataObjectToJava(ienv, rdo, reg);
	jobject resobj = ienv->NewObject(jclass_RouteSegmentResult, jmethod_RouteSegmentResult_ctor, robj,
			r.startPointIndex, r.endPointIndex);
	ienv->SetObjectField(resobj, jfield_RouteSegmentResult_preAttachedRoutes, ar);
	if(reg != NULL) {
		ienv->DeleteLocalRef(reg);
	}
	ienv->DeleteLocalRef(robj);
	ienv->DeleteLocalRef(ar);
	return resobj;
}

class NativeRoutingTile {
public:
	std::vector<RouteDataObject*> result;
	UNORDERED(map)<uint64_t, std::vector<RouteDataObject*> > cachedByLocations;
};


//	protected static native void deleteRouteSearchResult(long searchResultHandle!);
extern "C" JNIEXPORT void JNICALL Java_net_osmand_NativeLibrary_deleteRouteSearchResult(JNIEnv* ienv,
		jobject obj, jlong ref) {
	NativeRoutingTile* t = (NativeRoutingTile*) ref;
	for (unsigned int i = 0; i < t->result.size(); i++) {
		delete t->result[i];
		t->result[i] = NULL;
	}
	delete t;
}

class RouteCalculationProgressWrapper: public RouteCalculationProgress {
	jobject j;
	JNIEnv* ienv;
public:
	RouteCalculationProgressWrapper(JNIEnv* ienv, jobject j) : RouteCalculationProgress(),
			ienv(ienv), j(j)  {
	}
	virtual bool isCancelled() {
		return ienv->GetBooleanField(j, jfield_RouteCalculationProgress_isCancelled);
	}
	virtual void setSegmentNotFound(int s) {
		ienv->SetIntField(j, jfield_RouteCalculationProgress_segmentNotFound, s);
	}
	virtual void updateStatus(float distanceFromBegin, int directSegmentQueueSize, float distanceFromEnd,
			int reverseSegmentQueueSize) {
		RouteCalculationProgress::updateStatus(distanceFromBegin, directSegmentQueueSize,
				distanceFromEnd, reverseSegmentQueueSize);
		ienv->SetFloatField(j, jfield_RouteCalculationProgress_distanceFromBegin, this->distanceFromBegin);
		ienv->SetFloatField(j, jfield_RouteCalculationProgress_distanceFromEnd, this->distanceFromEnd);
		ienv->SetIntField(j, jfield_RouteCalculationProgress_directSegmentQueueSize, this->directSegmentQueueSize);
		ienv->SetIntField(j, jfield_RouteCalculationProgress_reverseSegmentQueueSize, this->reverseSegmentQueueSize);

	}
};

//p RouteSegmentResult[] nativeRouting(int[] coordinates, int[] state, String[] keyConfig, String[] valueConfig);
extern "C" JNIEXPORT jobjectArray JNICALL Java_net_osmand_NativeLibrary_nativeRouting(JNIEnv* ienv,
		jobject obj, jintArray  coordinates,
		jintArray stateConfig, jobjectArray keyConfig, jobjectArray valueConfig, jfloat initDirection,
		jobjectArray regions, jobject progress) {
	vector<ROUTE_TRIPLE> cfg;
	int* data = ienv->GetIntArrayElements(stateConfig, NULL);
	for(int k = 0; k < ienv->GetArrayLength(stateConfig); k++) {
		jstring kl = (jstring)ienv->GetObjectArrayElement(keyConfig, k);
		jstring vl = (jstring)ienv->GetObjectArrayElement(valueConfig, k);
		ROUTE_TRIPLE t = ROUTE_TRIPLE (data[k], std::pair<string, string>(
				getString(ienv, kl),
				getString(ienv, vl))
		);
		ienv->DeleteLocalRef(kl);
		ienv->DeleteLocalRef(vl);
		cfg.push_back(t);
	}
	ienv->ReleaseIntArrayElements(stateConfig, data, 0);

	RoutingConfiguration config(cfg, initDirection);
	RoutingContext c(config);
	c.progress = SHARED_PTR<RouteCalculationProgress>(new RouteCalculationProgressWrapper(ienv, progress));
	data = ienv->GetIntArrayElements(coordinates, NULL);
	c.startX = data[0];
	c.startY = data[1];
	c.endX = data[2];
	c.endY = data[3];
	ienv->ReleaseIntArrayElements(coordinates, data, 0);
	vector<RouteSegmentResult> r = searchRouteInternal(&c, false);
	UNORDERED(map)<int64_t, int> indexes;
	for (int t = 0; t< ienv->GetArrayLength(regions); t++) {
		jobject oreg = ienv->GetObjectArrayElement(regions, t);
		int64_t fp = ienv->GetIntField(oreg, jfield_RouteRegion_filePointer);
		int64_t ln = ienv->GetIntField(oreg, jfield_RouteRegion_length);
		ienv->DeleteLocalRef(oreg);
		indexes[(fp <<31) + ln] = t;
	}

	// convert results
	jobjectArray res = ienv->NewObjectArray(r.size(), jclass_RouteSegmentResult, NULL);
	for (int i = 0; i < r.size(); i++) {
		jobject resobj = convertRouteSegmentResultToJava(ienv, r[i], indexes, regions);
		ienv->SetObjectArrayElement(res, i, resobj);
		ienv->DeleteLocalRef(resobj);
	}
	if (r.size() == 0) {
		osmand_log_print(LOG_INFO, "No route found");
	}
	fflush(stdout);
	return res;
}

//	protected static native RouteDataObject[] getRouteDataObjects(NativeRouteSearchResult rs, int x31, int y31!);
extern "C" JNIEXPORT jobjectArray JNICALL Java_net_osmand_NativeLibrary_getRouteDataObjects(JNIEnv* ienv,
		jobject obj, jobject reg, jlong ref, jint x31, jint y31) {

	NativeRoutingTile* t = (NativeRoutingTile*) ref;
	uint64_t lr = ((uint64_t) x31 << 31) + y31;
	std::vector<RouteDataObject*> collected = t->cachedByLocations[lr];
	jobjectArray res = ienv->NewObjectArray(collected.size(), jclass_RouteDataObject, NULL);
	for (jint i = 0; i < collected.size(); i++) {
		jobject robj = convertRouteDataObjectToJava(ienv, collected[i], reg);
		ienv->SetObjectArrayElement(res, i, robj);
		ienv->DeleteLocalRef(robj);
	}
	return res;
}

//protected static native NativeRouteSearchResult loadRoutingData(RouteRegion reg, String regName, int regfp, RouteSubregion subreg,
//			boolean loadObjects);
extern "C" JNIEXPORT jobject JNICALL Java_net_osmand_NativeLibrary_loadRoutingData(JNIEnv* ienv,
		jobject obj, jobject reg, jstring regName, jint regFilePointer,
		jobject subreg, jboolean loadObjects) {
	RoutingIndex ind;
	ind.filePointer = regFilePointer;
	ind.name = getString(ienv, regName);
	RouteSubregion sub(&ind);
	sub.filePointer = ienv->GetIntField(subreg, jfield_RouteSubregion_filePointer);
	sub.length = ienv->GetIntField(subreg, jfield_RouteSubregion_length);
	sub.left = ienv->GetIntField(subreg, jfield_RouteSubregion_left);
	sub.right = ienv->GetIntField(subreg, jfield_RouteSubregion_right);
	sub.top = ienv->GetIntField(subreg, jfield_RouteSubregion_top);
	sub.bottom = ienv->GetIntField(subreg, jfield_RouteSubregion_bottom);
	sub.mapDataBlock= ienv->GetIntField(subreg, jfield_RouteSubregion_shiftToData);
	std::vector<RouteDataObject*> result;
	SearchQuery q;
	searchRouteDataForSubRegion(&q, result, &sub);


	if (loadObjects) {
		jobjectArray res = ienv->NewObjectArray(result.size(), jclass_RouteDataObject, NULL);
		for (jint i = 0; i < result.size(); i++) {
			if (result[i] != NULL) {
				jobject robj = convertRouteDataObjectToJava(ienv, result[i], reg);
				ienv->SetObjectArrayElement(res, i, robj);
				ienv->DeleteLocalRef(robj);
			}
		}
		for (unsigned int i = 0; i < result.size(); i++) {
			if (result[i] != NULL) {
				delete result[i];
			}
			result[i] = NULL;
		}
		return ienv->NewObject(jclass_NativeRouteSearchResult, jmethod_NativeRouteSearchResult_init, ((jlong) 0), res);
	} else {
		NativeRoutingTile* r = new NativeRoutingTile();
		for (jint i = 0; i < result.size(); i++) {
			if (result[i] != NULL) {
				r->result.push_back(result[i]);
				for (jint j = 0; j < result[i]->pointsX.size(); j++) {
					jint x = result[i]->pointsX[j];
					jint y = result[i]->pointsY[j];
					uint64_t lr = ((uint64_t) x << 31) + y;
					r->cachedByLocations[lr].push_back(result[i]);
				}
			}
		}
		jlong ref = (jlong) r;
		if(r->result.size() == 0) {
			ref = 0;
			delete r;
		}

		return ienv->NewObject(jclass_NativeRouteSearchResult, jmethod_NativeRouteSearchResult_init, ref, NULL);
	}

}


void pushToJavaRenderingContext(JNIEnv* env, jobject jrc, JNIRenderingContext* rc)
{
	env->SetIntField( jrc, jfield_RenderingContext_pointCount, (jint) rc->pointCount);
	env->SetIntField( jrc, jfield_RenderingContext_pointInsideCount, (jint)rc->pointInsideCount);
	env->SetIntField( jrc, jfield_RenderingContext_visible, (jint)rc->visible);
	env->SetIntField( jrc, jfield_RenderingContext_allObjects, rc->allObjects);
	env->SetIntField( jrc, jfield_RenderingContext_textRenderingTime, rc->textRendering.getElapsedTime());
	env->SetIntField( jrc, jfield_RenderingContext_lastRenderedKey, rc->lastRenderedKey);
}

bool JNIRenderingContext::interrupted()
{
	return env->GetBooleanField(javaRenderingContext, jfield_RenderingContext_interrupted);
}

SkBitmap* JNIRenderingContext::getCachedBitmap(const std::string& bitmapResource) {
	JNIEnv* env = this->env;
	jstring jstr = env->NewStringUTF(bitmapResource.c_str());
	jbyteArray javaIconRawData = (jbyteArray)env->CallObjectMethod(this->javaRenderingContext, jmethod_RenderingContext_getIconRawData, jstr);
	env->DeleteLocalRef(jstr);
	if(!javaIconRawData)
		return NULL;

	jbyte* bitmapBuffer = env->GetByteArrayElements(javaIconRawData, NULL);
	jint bufferLen = env->GetArrayLength(javaIconRawData);

	// Decode bitmap
	SkBitmap* iconBitmap = new SkBitmap();
	//TODO: JPEG is badly supported! At the moment it needs sdcard to be present (sic). Patch that
	if(!SkImageDecoder::DecodeMemory(bitmapBuffer, bufferLen, iconBitmap))
	{
		// Failed to decode
		delete iconBitmap;

		this->nativeOperations.start();
		env->ReleaseByteArrayElements(javaIconRawData, bitmapBuffer, JNI_ABORT);
		env->DeleteLocalRef(javaIconRawData);

		throwNewException(env, (std::string("Failed to decode ") + bitmapResource).c_str());

		return NULL;
	}

	env->ReleaseByteArrayElements(javaIconRawData, bitmapBuffer, JNI_ABORT);
	env->DeleteLocalRef(javaIconRawData);

	return iconBitmap;
}

std::string JNIRenderingContext::getTranslatedString(const std::string& name) {
	if (this->isUsingEnglishNames()) {
		jstring n = this->env->NewStringUTF(name.c_str());
		jstring translate = (jstring) this->env->CallStaticObjectMethod(jclass_JUnidecode, jmethod_JUnidecode_unidecode, n);
		std::string res = getString(this->env, translate);
		this->env->DeleteLocalRef(translate);
		this->env->DeleteLocalRef(n);
		return res;
	}
	return name;
}

std::string JNIRenderingContext::getReshapedString(const std::string& name) {
	jstring n = this->env->NewStringUTF(name.c_str());
	jstring translate = (jstring) this->env->CallStaticObjectMethod(jclass_Reshaper, jmethod_Reshaper_reshape, n);
	std::string res = getString(this->env, translate);
	this->env->DeleteLocalRef(translate);
	this->env->DeleteLocalRef(n);
	return res;
}

