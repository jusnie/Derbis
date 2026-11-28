import Foundation

/// Cross-screen dispatch for tab-bar shortcuts.
///
/// The shortcut slots in the tab bar (Playlists, Favorites, Downloads, Stats, Local Files)
/// jump straight into a section of the Library tab without owning any view state.
/// RootView observes the same singleton, so both the liquid-glass bar and the classic
/// pill dock can fire the same actions.
@MainActor
final class TabBarCommandCenter: ObservableObject {
    static let shared = TabBarCommandCenter()

    struct LibrarySectionRequest: Equatable {
        let section: LibrarySectionTab
        let nonce: Int
    }

    @Published private(set) var librarySectionRequest: LibrarySectionRequest?
    private var counter = 0

    private init() {}

    func requestLibrarySection(_ section: LibrarySectionTab) {
        counter += 1
        librarySectionRequest = LibrarySectionRequest(section: section, nonce: counter)
    }
}
