#include "tfilestream.h"
#include "utils.h"

extern "C" {
JNIEXPORT jobject JNICALL
Java_com_kyant_taglib_TagLib_getAudioProperties(
        JNIEnv *env,
        jclass,
        jint fd,
        jint read_style
) {
    char *path = getRealPathFromFd(fd);
    if (path == nullptr) {
        return nullptr;
    }
    const auto stream = std::make_unique<TagLib::FileStream>(fd, true);
    const auto style = static_cast<TagLib::AudioProperties::ReadStyle>(read_style);
    const TagLibExt::FileRef f(path, stream.get(), true, style);

    if (f.isNull()) {
        free(path);
        return nullptr;
    }

    jobject audioProperties = getAudioProperties(env, f);
    free(path);
    return audioProperties;
}

JNIEXPORT jobject JNICALL
Java_com_kyant_taglib_TagLib_getMetadata(
        JNIEnv *env,
        jclass,
        jint fd,
        jboolean read_pictures
) {
    char *path = getRealPathFromFd(fd);
    if (path == nullptr) {
        return nullptr;
    }
    const auto stream = std::make_unique<TagLib::FileStream>(fd, true);
    const TagLibExt::FileRef f(path, stream.get(), false);

    if (f.isNull()) {
        free(path);
        return nullptr;
    }

    jobject propertiesMap = getPropertyMap(env, f);
    jobjectArray pictures;
    if (read_pictures) {
        pictures = getPictures(env, f);
    } else {
        pictures = emptyPictureArray(env);
    }

    jobject metadata = env->NewObject(
            metadataClass, metadataConstructor,
            propertiesMap, pictures
    );
    free(path);
    return metadata;
}

JNIEXPORT jobjectArray JNICALL
Java_com_kyant_taglib_TagLib_getMetadataPropertyValues(
        JNIEnv *env,
        jclass,
        jint fd,
        jstring property_name
) {
    const char *propertyName = env->GetStringUTFChars(property_name, nullptr);
    if (propertyName == nullptr) {
        return nullptr;
    }

    char *path = getRealPathFromFd(fd);
    if (path == nullptr) {
        env->ReleaseStringUTFChars(property_name, propertyName);
        return nullptr;
    }
    const auto stream = std::make_unique<TagLib::FileStream>(fd, true);
    const TagLibExt::FileRef f(path, stream.get(), false);

    if (f.isNull()) {
        free(path);
        return nullptr;
    }

    const auto propertyMap = f.properties();
    const auto valueList = propertyMap.find(TagLib::String(propertyName))->second;
    jobjectArray result = env->NewObjectArray(static_cast<jsize>(valueList.size()),
                                              stringClass, nullptr);
    int i = 0;
    for (const auto &value: valueList) {
        jstring jValue = env->NewStringUTF(value.toCString(true));
        env->SetObjectArrayElement(result, i, jValue);
        env->DeleteLocalRef(jValue);
        i++;
    }

    env->ReleaseStringUTFChars(property_name, propertyName);
    free(path);
    return result;
}

JNIEXPORT jobjectArray JNICALL
Java_com_kyant_taglib_TagLib_getPictures(
        JNIEnv *env,
        jclass,
        jint fd
) {
    char *path = getRealPathFromFd(fd);
    if (path == nullptr) {
        return nullptr;
    }
    const auto stream = std::make_unique<TagLib::FileStream>(fd, true);
    const TagLibExt::FileRef f(path, stream.get(), false);

    if (f.isNull()) {
        free(path);
        return emptyPictureArray(env);
    }

    jobjectArray pictures = getPictures(env, f);
    free(path);
    return pictures;
}

JNIEXPORT jboolean JNICALL
Java_com_kyant_taglib_TagLib_savePropertyMap(
        JNIEnv *env,
        jclass,
        jint fd,
        jobject property_map
) {
    char *path = getRealPathFromFd(fd);
    if (path == nullptr) {
        return false;
    }
    const auto stream = std::make_unique<TagLib::FileStream>(fd, false);
    TagLibExt::FileRef f(path, stream.get(), false);

    if (f.isNull()) {
        free(path);
        return false;
    }

    const PropertyMap propertyMap = JniHashMapToPropertyMap(env, property_map);
    f.setProperties(propertyMap);
    const bool success = f.save();
    free(path);
    return success;
}

JNIEXPORT jboolean JNICALL
Java_com_kyant_taglib_TagLib_savePictures(
        JNIEnv *env,
        jclass,
        jint fd,
        jobjectArray pictures
) {
    char *path = getRealPathFromFd(fd);
    if (path == nullptr) {
        return false;
    }
    const auto stream = std::make_unique<TagLib::FileStream>(fd, false);
    TagLibExt::FileRef f(path, stream.get(), false);

    if (f.isNull()) {
        free(path);
        return false;
    }

    auto pictureList = JniPictureArrayToPictureList(env, pictures);
    f.setComplexProperties("PICTURE", pictureList);
    const bool success = f.save();
    free(path);
    return success;
}
}
