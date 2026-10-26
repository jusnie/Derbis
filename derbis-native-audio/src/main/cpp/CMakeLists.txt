cmake_minimum_required(VERSION 3.22.1)

project(derbis_native_audio C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(BUILD_SHARED_LIBS OFF)

if(NOT DEFINED DERBIS_NATIVE_DEPS_DIR)
    message(FATAL_ERROR "DERBIS_NATIVE_DEPS_DIR is required. Run the prepareNativeAudioDependencies task.")
endif()

set(derbis_ffmpeg_dir "${DERBIS_NATIVE_DEPS_DIR}/ffmpeg/${ANDROID_ABI}")
set(derbis_oboe_dir "${DERBIS_NATIVE_DEPS_DIR}/oboe")

if(NOT EXISTS "${derbis_ffmpeg_dir}/lib/libavcodec.a")
    message(FATAL_ERROR "FFmpeg static libraries missing for ${ANDROID_ABI} in ${derbis_ffmpeg_dir}")
endif()
if(NOT EXISTS "${derbis_oboe_dir}/CMakeLists.txt")
    message(FATAL_ERROR "Oboe sources missing in ${derbis_oboe_dir}")
endif()

add_compile_options(
        "-ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}=."
        "-ffile-prefix-map=${DERBIS_NATIVE_DEPS_DIR}=deps")

set(derbis_link_options "-Wl,-z,max-page-size=16384" "-Wl,--gc-sections" "-Wl,--exclude-libs,ALL" "-Wl,--no-undefined")

foreach(ffmpeg_lib avcodec swresample avutil)
    add_library(${ffmpeg_lib} STATIC IMPORTED)
    set_target_properties(${ffmpeg_lib} PROPERTIES
            IMPORTED_LOCATION "${derbis_ffmpeg_dir}/lib/lib${ffmpeg_lib}.a")
endforeach()

find_library(android_log_lib log)

add_library(ffmpegJNI SHARED ffmpeg/ffmpeg_jni.cc)
target_include_directories(ffmpegJNI PRIVATE "${derbis_ffmpeg_dir}/include")
target_link_libraries(ffmpegJNI PRIVATE avcodec swresample avutil android m ${android_log_lib})
target_link_options(ffmpegJNI PRIVATE ${derbis_link_options})
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_link_options(ffmpegJNI PRIVATE "-Wl,-Bsymbolic")
endif()

add_subdirectory("${derbis_oboe_dir}" "${CMAKE_CURRENT_BINARY_DIR}/oboe" EXCLUDE_FROM_ALL)

add_library(derbis_oboe SHARED
        oboe/DerbisOboeOutput.cpp
        oboe/derbis_oboe_jni.cpp)
target_compile_options(derbis_oboe PRIVATE -Wall -Wextra -fvisibility=hidden)
target_link_libraries(derbis_oboe PRIVATE oboe ${android_log_lib})
target_link_options(derbis_oboe PRIVATE ${derbis_link_options})
