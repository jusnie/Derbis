# Third-party notices

## SimpMusic Apple Music player UI

The Apple Music 13 portrait player in
`app/src/main/java/com/yasuda/derbis/ui/player/AppleMusicPlayer.kt` and the Apple Music 13 lyrics
surface in `app/src/main/java/com/yasuda/derbis/ui/DerbisApp.kt` adapt the visual structure of
SimpMusic’s `NowPlayingContentAppleMusic.kt`, `AppleMusicShared.kt`, `AppleMusicLyricsView.kt`,
`AppleMusicQueueView.kt`, and `FullWidthItems.kt`, from upstream revision
[`068d744`](https://github.com/maxrave-dev/SimpMusic/tree/068d74435f0cf0e8552af3da07879eafd328fea7).

- Project: [maxrave-dev/SimpMusic](https://github.com/maxrave-dev/SimpMusic)
- Copyright: SimpMusic contributors
- License: GNU General Public License, version 3.0
- Local changes: translated to Derbis’s Android Compose architecture and existing player-state/actions;
  retained Derbis’s discrete word-by-word lyric renderer rather than SimpMusic’s sweep renderer,
  while keeping the upstream queue’s flat song rows, source-style layout, and fixed bottom cluster.

Derbis is distributed under the GNU General Public License v3.0; the complete license text is available in the repository’s
[`LICENSE`](LICENSE).
