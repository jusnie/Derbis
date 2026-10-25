cmake_minimum_required(VERSION 3.22.1)
project(derbis_discord_bridge)

add_library(derbis_discord_bridge SHARED DiscordRichPresenceBridge.cpp)

find_library(log-lib log)
find_package(discord_partner_sdk REQUIRED CONFIG)

target_compile_features(derbis_discord_bridge PRIVATE cxx_std_17)
target_link_libraries(
    derbis_discord_bridge
    PRIVATE
    discord_partner_sdk::discord_partner_sdk
    ${log-lib}
)
