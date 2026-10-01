#pragma once

#include <jni.h>

#include <map>
#include <memory>
#include <string>
#include <string_view>

#if defined(__ANDROID__)
#include <android/log.h>
#endif

namespace az {
namespace jni {

// Слой-обёртка над JNI для собственного маленького ffi.
// Не потокобезопасный сам по себе; используйте на JNI-потоке либо
// приатчьте через JniThreadEnv (ниже).

// jstring <-> std::string (без копирования лишний раз - использует GetStringUTFChars).
inline std::string toString(JNIEnv* env, jstring js) {
    if (!js) return {};
    const char* utf = env->GetStringUTFChars(js, nullptr);
    if (!utf) return {};
    std::string out(utf);
    env->ReleaseStringUTFChars(js, utf);
    return out;
}

inline jstring toJString(JNIEnv* env, std::string_view s) {
    return env->NewStringUTF(std::string(s).c_str());
}

// Очистка pending-exception + печать в logcat. Возвращает true, если был exception.
inline bool check_exception(JNIEnv* env, const char* where = nullptr) {
    if (!env->ExceptionCheck()) return false;
#if defined(__ANDROID__)
    if (where) __android_log_print(ANDROID_LOG_ERROR, "az-jni", "exc @ %s", where);
#endif
    env->ExceptionDescribe();
    env->ExceptionClear();
    return true;
}

// RAII для глобальной ссылки.
class GlobalRef {
public:
    GlobalRef() = default;
    GlobalRef(JNIEnv* env, jobject obj) : env_(env), obj_(obj ? env->NewGlobalRef(obj) : nullptr) {}
    GlobalRef(const GlobalRef&) = delete;
    GlobalRef& operator=(const GlobalRef&) = delete;
    GlobalRef(GlobalRef&& o) noexcept : env_(o.env_), obj_(o.obj_) {
        o.env_ = nullptr;
        o.obj_ = nullptr;
    }
    GlobalRef& operator=(GlobalRef&& o) noexcept {
        if (this != &o) {
            if (obj_) env_->DeleteGlobalRef(obj_);
            env_ = o.env_;
            obj_ = o.obj_;
            o.env_ = nullptr;
            o.obj_ = nullptr;
        }
        return *this;
    }
    ~GlobalRef() {
        if (obj_) env_->DeleteGlobalRef(obj_);
    }
    jobject get() const { return obj_; }
    operator bool() const { return obj_ != nullptr; }

private:
    JNIEnv* env_ = nullptr;
    jobject obj_ = nullptr;
};

// Класс-кэш: кэширует jclass/jmethodID (глобальные ссылки), чтобы не
// искать методы на каждый вызов. Типичное использование:
//   static JniClass cls(env, "xyz/azraellab/app/NativeBridge");
//   cls.method(env, "onResult", "(Ljava/lang/String;B)V");
class JniClass {
public:
    JniClass() = default;
    // Создаёт глобальную ссылку на класс.
    JniClass(JNIEnv* env, const char* name) {
        env_ = env;
        name_ = name;
        jclass local = env->FindClass(name);
        if (local) {
            global_ = env->NewGlobalRef(local);
            env->DeleteLocalRef(local);
        }
    }
    ~JniClass() {
        if (global_) env_->DeleteGlobalRef(global_);
    }
    JniClass(const JniClass&) = delete;
    JniClass& operator=(const JniClass&) = delete;

    bool valid() const { return global_ != nullptr; }

    jmethodID method(JNIEnv* env, const char* mname, const char* sig) {
        if (auto it = caches_.find(sig); it != caches_.end()) return it->second;
        jmethodID mid = env->GetMethodID(global_, mname, sig);
        if (mid) caches_[sig] = mid;
        return mid;
    }

    jclass get(JNIEnv* env) {
        // Возвращает локальную ссылку (не удалять вручную - локальная живёт до popLocalFrame).
        jclass local = env->FindClass(name());
        return local ? local : nullptr;
    }

    const char* name() const { return name_; }

private:
    JNIEnv* env_ = nullptr;
    jclass global_ = nullptr;
    std::string name_;
    std::map<std::string, jmethodID> caches_;
};

// Подключает текущий поток к JavaVM (для вызовов из C++ фоновых потоков);
// отключает в деструкторе. RAII:
//   az::jni::JniAttach attach(jvm);
//   if (attach) { attach.env()->CallVoidMethod(...); }
class JniAttach {
public:
    explicit JniAttach(JavaVM* vm) : vm_(vm) {
        if (!vm) return;
        JNIEnv* env = nullptr;
        jint rc = vm_->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
        if (rc == JNI_OK) {
            env_ = env;
            attached_ = false;
        } else if (rc == JNI_EDETACHED) {
            // Прикрепиться.
            JavaVMAttachArgs args{JNI_VERSION_1_6, "native-worker", nullptr};
            if (vm_->AttachCurrentThread(&env, &args) == JNI_OK) {
                env_ = env;
                attached_ = true;
            }
        }
    }
    ~JniAttach() {
        if (attached_ && vm_) vm_->DetachCurrentThread();
    }
    JniAttach(const JniAttach&) = delete;
    JniAttach& operator=(const JniAttach&) = delete;

    JNIEnv* env() const { return env_; }
    explicit operator bool() const { return env_ != nullptr; }

private:
    JavaVM* vm_ = nullptr;
    JNIEnv* env_ = nullptr;
    bool attached_ = false;
};

}  // namespace jni
}  // namespace az