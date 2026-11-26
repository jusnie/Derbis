import SwiftUI

@MainActor
@main
struct DerbisApp: App {
    @StateObject private var theme = ThemeManager()
    @StateObject private var player = AudioPlayerEngine.shared
    @StateObject private var library = LibraryStore.shared
    @StateObject private var subsonic = SubsonicService.shared

    init() {
        ThemeManager.registerBundledFonts()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(theme)
                .environmentObject(player)
                .environmentObject(library)
                .environmentObject(subsonic)
                .onOpenURL { url in
                    handleIncomingURL(url)
                }
        }
    }

    private func handleIncomingURL(_ url: URL) {
        if JamService.shared.handleIncomingJamURL(url) {
            return
        }

        if url.isFileURL {
            Task {
                await library.importLocalAudioFiles(urls: [url])
                if let imported = library.localTracks.first {
                    player.play(track: imported, in: library.localTracks)
                }
            }
            return
        }

        if url.scheme?.lowercased() == "derbis" {
            let comps = URLComponents(url: url, resolvingAgainstBaseURL: false)
            if url.host == "track" || url.host == "t",
               let vid = url.pathComponents.dropFirst().first,
               vid.count == 11 {
                let track = Track(
                    id: "yt:\(vid)",
                    title: comps?.queryItems?.first(where: { $0.name == "title" })?.value ?? "Shared Track",
                    artist: comps?.queryItems?.first(where: { $0.name == "artist" })?.value ?? "Derbis",
                    thumbnailURL: "https://i.ytimg.com/vi/\(vid)/hqdefault.jpg",
                    largeThumbnailURL: "https://i.ytimg.com/vi/\(vid)/hqdefault.jpg",
                    source: .youtubeMusic
                )
                player.play(track: track, in: [track])
            }
        }
    }
}
